package io.joyrpc.invoker;

/*-
 * #%L
 * joyrpc
 * %%
 * Copyright (C) 2019 joyrpc.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import io.joyrpc.Invoker;
import io.joyrpc.InvokerAware;
import io.joyrpc.Result;
import io.joyrpc.cluster.discovery.config.Configure;
import io.joyrpc.config.InterfaceOption;
import io.joyrpc.context.injection.Transmit;
import io.joyrpc.exception.RpcException;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.Shutdown;
import io.joyrpc.util.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static io.joyrpc.Plugin.TRANSMIT;

/**
 * 抽象服务调用
 */
public abstract class AbstractService implements Invoker {
    /**
     * 代理的接口类
     */
    protected Class<?> interfaceClass;
    /**
     * 接口真实名称
     */
    protected String interfaceName;
    /**
     * URL
     */
    protected URL url;
    /**
     * 名称
     */
    protected String name;
    /**
     * 别名
     */
    protected String alias;
    /**
     * 是否是系统服务
     */
    protected boolean system;
    /**
     * 配置器
     */
    protected Configure configure;
    /***
     * 订阅的URL
     */
    protected URL subscribeUrl;
    /**
     * 方法选项
     */
    protected InterfaceOption option;
    /**
     * 调用链
     */
    protected Invoker chain;
    /**
     * 透传插件
     */
    protected Iterable<Transmit> transmits = TRANSMIT.extensions();
    /**
     * 调用计数器
     */
    protected AtomicLong requests = new AtomicLong(0);
    /**
     * 状态机
     */
    protected StateMachine<Void, StateController<Void>> stateMachine = new StateMachine<>(
            () -> new StateController<Void>() {
                @Override
                public CompletableFuture<Void> open() {
                    return doOpen();
                }

                @Override
                public CompletableFuture<Void> close(final boolean gracefully) {
                    return doClose();
                }
            },
            new StateFuture<>(null, () -> requests.get() <= 0 ? CompletableFuture.completedFuture(null) : new CompletableFuture<>()));
    /**
     * 构建器
     */
    protected Consumer<InvokerAware> builder = this::setup;

    public URL getUrl() {
        return url;
    }

    @Override
    public String getName() {
        return name;
    }

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public String getAlias() {
        return alias;
    }

    public InterfaceOption getOption() {
        return option;
    }

    /**
     * 关闭异常
     *
     * @return 异常
     */
    protected abstract Throwable shutdownException();

    @Override
    public CompletableFuture<Result> invoke(final RequestMessage<Invocation> request) {
        CompletableFuture<Result> future;
        //判断状态
        if ((Shutdown.isShutdown() || !stateMachine.isOpened()) && !system) {
            //系统服务允许执行，例如注册中心在关闭的时候进行注销操作
            if (request.getOption() == null) {
                try {
                    setup(request);
                } catch (Throwable ignored) {
                }
            }
            return Futures.completeExceptionally(shutdownException());
        }
        //增加计数器
        requests.incrementAndGet();
        try {
            if (request.getOption() == null) {
                setup(request);
            }
            future = doInvoke(request);
        } catch (Throwable e) {
            //如果抛出了异常
            if (e instanceof RpcException) {
                future = Futures.completeExceptionally(e);
            } else {
                future = Futures.completeExceptionally(new RpcException("Error occurs while invoking, caused by " + e.getMessage(), e));
            }
        }
        future.whenComplete((result, throwable) -> {
            if (requests.decrementAndGet() == 0) {
                //通知请求已经完成，触发优雅关闭
                stateMachine.pass();
            }
        });
        return future;
    }

    /**
     * 执行调用，可以用于跟踪拦截
     *
     * @param request 请求
     * @return CompletableFuture
     */
    protected CompletableFuture<Result> doInvoke(final RequestMessage<Invocation> request) {
        //执行调用链
        return chain.invoke(request);
    }

    /**
     * 异步打开
     *
     * @return CompletableFuture
     */
    public CompletableFuture<Void> open() {
        return stateMachine.open();
    }

    @Override
    public CompletableFuture<Void> close(final boolean gracefully) {
        return stateMachine.close(gracefully);
    }

    /**
     * 打开
     *
     * @return CompletableFuture
     */
    protected abstract CompletableFuture<Void> doOpen();

    /**
     * 关闭
     *
     * @return CompletableFuture
     */
    protected abstract CompletableFuture<Void> doClose();

    /**
     * 设置参数
     *
     * @param target 目标对象
     */
    protected void setup(final InvokerAware target) {
        target.setClassName(interfaceName);
        target.setClass(interfaceClass);
        target.setUrl(url);
        target.setup();
    }

    public Consumer<InvokerAware> getBuilder() {
        return builder;
    }

}
