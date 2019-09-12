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
import io.joyrpc.Result;
import io.joyrpc.cluster.discovery.registry.AbstractRegistry;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.Futures;
import io.joyrpc.util.Shutdown;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static io.joyrpc.Plugin.ENVIRONMENT;

/**
 * 抽象调用器
 */
public abstract class AbstractInvoker<T> implements Invoker {

    /**
     * 代理的接口类
     */
    protected Class<T> interfaceClass;

    /**
     * 接口真实名称
     */
    protected String interfaceName;
    /**
     * URL
     */
    protected URL url;
    /**
     * 往注册中心订阅注册的URL
     */
    protected URL registerUrl;
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
     * 调用计数器
     */
    protected AtomicLong requests = new AtomicLong(0);
    /**
     * 关闭Future
     */
    protected volatile CompletableFuture<Void> closeFuture;
    /**
     * 等到请求处理完
     */
    protected volatile CompletableFuture<Void> flyingFuture;
    /**
     * 是否关闭了
     */
    protected volatile boolean closed;

    public URL getUrl() {
        return this.url;
    }

    @Override
    public String getName() {
        return name;
    }

    public Class<T> getInterfaceClass() {
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
     * @return
     */
    protected abstract Throwable shutdownException();

    @Override
    public CompletableFuture<Result> invoke(final RequestMessage<Invocation> request) {
        //在关闭判断之前增加计数器，确保安全
        requests.incrementAndGet();
        if ((Shutdown.isShutdown() || closed) && !system) {
            //系统服务允许执行，例如注册中心再关闭的时候进行注销操作
            requests.decrementAndGet();
            return CompletableFuture.completedFuture(new Result(request.getContext(), shutdownException()));
        }

        //执行调用链，减少计数器
        return doInvoke(request).whenComplete((r, t) -> {
            if (requests.decrementAndGet() == 0 && flyingFuture != null) {
                //通知请求已经完成
                flyingFuture.complete(null);
            }
        });

    }

    /**
     * 执行调用
     *
     * @param request
     * @return
     */
    protected abstract CompletableFuture<Result> doInvoke(final RequestMessage<Invocation> request);

    /**
     * 异步打开
     *
     * @return
     */
    public synchronized CompletableFuture<Void> open() {
        closed = false;
        closeFuture = null;
        flyingFuture = null;
        return doOpen();
    }

    @Override
    public synchronized CompletableFuture<Void> close() {
        return close(ENVIRONMENT.get().getBoolean(Shutdown.GRACEFULLY_SHUTDOWN, Boolean.TRUE));
    }

    /**
     * 关闭
     *
     * @param gracefully 是否优雅关闭
     * @return
     */
    public synchronized CompletableFuture<Void> close(boolean gracefully) {
        if (!closed) {
            closed = true;
            if (gracefully) {
                closeFuture = new CompletableFuture<>();
                flyingFuture = new CompletableFuture<>();
                flyingFuture.whenComplete((v, t) -> Futures.chain(doClose(), closeFuture));
                //判断是否请求已经完成
                if (requests.get() == 0) {
                    flyingFuture.complete(null);
                }
            } else {
                closeFuture = doClose();
            }
        }
        return closeFuture;
    }

    /**
     * 打开
     *
     * @return
     */
    protected abstract CompletableFuture<Void> doOpen();

    /**
     * 关闭
     *
     * @return
     */
    protected abstract CompletableFuture<Void> doClose();

    /**
     * 构建订阅的URL
     *
     * @param url
     * @param clazz
     * @return
     */
    protected URL buildRegisterUrl(final URL url, final Class<?> clazz) {
        return AbstractRegistry.REGISTER_URL_FUNCTION.apply(url);
    }

}
