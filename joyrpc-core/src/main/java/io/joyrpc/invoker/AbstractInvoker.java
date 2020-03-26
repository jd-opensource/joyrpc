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
import io.joyrpc.exception.RpcException;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.Futures;
import io.joyrpc.util.Shutdown;
import io.joyrpc.util.Status;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;

import static io.joyrpc.util.Status.*;

/**
 * 抽象调用器
 */
public abstract class AbstractInvoker implements Invoker {
    protected static final AtomicReferenceFieldUpdater<AbstractInvoker, Status> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(AbstractInvoker.class, Status.class, "status");

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
     * 调用计数器
     */
    protected AtomicLong requests = new AtomicLong(0);
    /**
     * 打开的结果
     */
    protected volatile CompletableFuture<Void> openFuture;
    /**
     * 关闭Future
     */
    protected volatile CompletableFuture<Void> closeFuture;
    /**
     * 等到请求处理完
     */
    protected volatile CompletableFuture<Void> flyingFuture;
    /**
     * 状态
     */
    protected volatile Status status = CLOSED;
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

    /**
     * 关闭异常
     *
     * @return 异常
     */
    protected abstract Throwable shutdownException();

    @Override
    public CompletableFuture<Result> invoke(final RequestMessage<Invocation> request) {
        if ((Shutdown.isShutdown() || status != Status.OPENED) && !system) {
            //系统服务允许执行，例如注册中心在关闭的时候进行注销操作
            return CompletableFuture.completedFuture(new Result(request.getContext(), shutdownException()));
        }
        //执行调用链，减少计数器
        CompletableFuture<Result> future;
        //在关闭判断之前增加计数器，确保安全
        requests.incrementAndGet();
        try {
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
            if (requests.decrementAndGet() == 0 && flyingFuture != null) {
                //通知请求已经完成
                flyingFuture.complete(null);
            }
        });
        return future;

    }

    /**
     * 执行调用
     *
     * @param request 请求
     * @return CompletableFuture
     */
    protected abstract CompletableFuture<Result> doInvoke(final RequestMessage<Invocation> request);

    /**
     * 异步打开
     *
     * @return CompletableFuture
     */
    public CompletableFuture<Void> open() {
        if (STATE_UPDATER.compareAndSet(this, CLOSED, OPENING)) {
            final CompletableFuture<Void> future = new CompletableFuture<>();
            openFuture = future;
            closeFuture = null;
            flyingFuture = null;
            doOpen().whenComplete((v, t) -> {
                if (openFuture != future || t == null && !STATE_UPDATER.compareAndSet(this, OPENING, OPENED)) {
                    future.completeExceptionally(new IllegalStateException("state is illegal."));
                } else if (t != null) {
                    //出现了异常
                    future.completeExceptionally(t);
                    //自动关闭
                    close();
                } else {
                    future.complete(null);
                }
            });
            return future;
        } else {
            switch (status) {
                case OPENING:
                case OPENED:
                    //可重入，没有并发调用
                    return openFuture;
                default:
                    //其它状态不应该并发执行
                    return Futures.completeExceptionally(new IllegalStateException("state is illegal."));
            }
        }
    }

    @Override
    public CompletableFuture<Void> close(final boolean gracefully) {
        if (STATE_UPDATER.compareAndSet(this, OPENING, CLOSING)) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            //请求数肯定为0
            closeFuture = future;
            openFuture.whenComplete((v, t) -> {
                status = CLOSED;
                future.complete(null);
            });
            return future;
        } else if (STATE_UPDATER.compareAndSet(this, OPENED, CLOSING)) {
            //状态从打开到关闭中，该状态只能变更为CLOSED
            CompletableFuture<Void> future = new CompletableFuture<>();
            closeFuture = future;
            flyingFuture = new CompletableFuture<>();
            flyingFuture.whenComplete((v, t) -> doClose().whenComplete((o, s) -> {
                status = CLOSED;
                future.complete(null);
            }));
            //判断是否请求已经完成
            if (!gracefully || requests.get() == 0) {
                flyingFuture.complete(null);
            }
            return future;
        } else {
            switch (status) {
                case CLOSING:
                case CLOSED:
                    return closeFuture;
                default:
                    return Futures.completeExceptionally(new IllegalStateException("state is illegal."));
            }
        }

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
