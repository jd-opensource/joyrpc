package io.joyrpc.config;

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


import io.joyrpc.cluster.discovery.registry.Registry;
import io.joyrpc.cluster.event.NodeEvent;
import io.joyrpc.event.EventHandler;
import io.joyrpc.extension.URL;
import io.joyrpc.invoker.ServiceManager;
import io.joyrpc.invoker.Refer;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.joyrpc.Plugin.REGISTRY;

/**
 * 消费者配置
 */
public class ConsumerConfig<T> extends AbstractConsumerConfig<T> implements Serializable, EventHandler<NodeEvent> {

    public ConsumerConfig() {
    }

    public ConsumerConfig(ConsumerConfig config) {
        super(config);
    }

    public ConsumerConfig(AbstractConsumerConfig config) {
        super(config);
    }

    public ConsumerConfig(AbstractConsumerConfig config, String alias) {
        super(config, alias);
    }

    @Override
    protected ConsumerController<T> create() {
        return new ConsumerController<>(this);
    }

    @Override
    public void handle(final NodeEvent event) {
        eventHandlers.forEach(h -> h.handle(event));
    }

    public URL getServiceUrl() {
        return controller == null ? null : controller.getServiceUrl();
    }

    public Refer getRefer() {
        return controller == null ? null : ((ConsumerController) controller).getRefer();
    }

    /**
     * 消费者控制器
     *
     * @param <T>
     */
    protected static class ConsumerController<T> extends AbstractConsumerController<T, ConsumerConfig<T>> {
        /**
         * 注册中心
         */
        protected Registry registryRef;
        /**
         * Refer对象
         */
        protected volatile Refer refer;

        /**
         * 构造函数
         *
         * @param config
         */
        public ConsumerController(ConsumerConfig<T> config) {
            super(config);
        }

        @Override
        protected CompletableFuture<Void> doOpen() {
            CompletableFuture<Void> future = new CompletableFuture<>();
            //创建注册中心
            registryRef = REGISTRY.get(registryUrl.getProtocol()).getRegistry(registryUrl);
            //构建代理
            config.proxy();
            //订阅，等到初始化配置
            chain(subscribe(), future, (v) -> chain(waitingConfig, future, (url) -> {
                //检查动态配置是否修改了别名，需要重新订阅
                serviceUrl = url;
                registerUrl = config.register ? buildRegisteredUrl(registryRef, url) : null;
                resubscribe(buildSubscribedUrl(configureRef, url), false);
                try {
                    refer = ServiceManager.refer(url, config, registryRef, registerUrl, configureRef, subscribeUrl, configHandler);
                    //打开
                    chain(refer.open(), future, s -> {
                        //构建调用器
                        invokeHandler = new ConsumerInvokeHandler(refer, proxyClass, refer.getUrl());
                        future.complete(null);
                    });
                } catch (Throwable ex) {
                    future.completeExceptionally(ex);
                }
            }));
            future.whenComplete((v, err) -> latch.countDown());
            return future;
        }

        @Override
        public CompletableFuture<Void> close(final boolean gracefully) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            //拒绝新请求
            invokeHandler = null;
            latch = null;
            if (refer != null) {
                refer.close(gracefully).whenComplete((v, t) -> future.complete(null));
            } else {
                future.complete(null);
            }
            return future;
        }

        /**
         * 订阅，打开注册中心
         *
         * @return
         */
        protected CompletableFuture<Void> subscribe() {
            //等待注册中心初始化
            CompletableFuture<Void> result = subscribe(registryRef, new AtomicBoolean(false));
            if (!config.subscribe) {
                //不订阅配置
                waitingConfig.complete(serviceUrl);
            }
            return result;
        }

        @Override
        protected CompletableFuture<Void> update(final URL newUrl) {
            //只在opened状态触发
            CompletableFuture<Void> future = new CompletableFuture<>();
            Refer oldRefer = refer;
            URL newRegisterUrl = config.register ? buildRegisteredUrl(registryRef, newUrl) : null;
            URL newSubscribeUrl = config.subscribe ? buildSubscribedUrl(configureRef, newUrl) : null;
            Refer newRefer = ServiceManager.refer(newUrl, config, registryRef, newRegisterUrl, configureRef, newSubscribeUrl, configHandler);
            chain(newRefer.open(), future, v -> {
                if (!isClose()) {
                    //检查动态配置是否修改了别名，需要重新订阅
                    resubscribe(newSubscribeUrl, true);
                    serviceUrl = newUrl;
                    registerUrl = newRegisterUrl;
                    invokeHandler = new ConsumerInvokeHandler(refer, proxyClass, newRefer.getUrl());
                    refer = newRefer;
                    oldRefer.close(true);
                    //再次判断是否在关闭，防止前面复制的
                    if (isClose()) {
                        newRefer.close(true);
                    }
                } else {
                    newRefer.close(false);
                }
            });
            return future;
        }

        public Refer getRefer() {
            return refer;
        }
    }
}
