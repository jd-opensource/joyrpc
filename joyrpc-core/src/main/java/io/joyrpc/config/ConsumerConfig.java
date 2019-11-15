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
import io.joyrpc.constants.Constants;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.extension.URL;
import io.joyrpc.invoker.InvokerManager;
import io.joyrpc.invoker.Refer;
import io.joyrpc.proxy.ConsumerInvokeHandler;
import io.joyrpc.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.Plugin.REGISTRY;

/**
 * 消费者配置
 */
public class ConsumerConfig<T> extends AbstractConsumerConfig<T> implements Serializable {
    private final static Logger logger = LoggerFactory.getLogger(ConsumerConfig.class);
    /**
     * Refer对象
     */
    protected transient volatile Refer<T> refer;
    /**
     * 注册中心
     */
    protected transient Registry registryRef;

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
    protected void doRefer(final CompletableFuture<Void> future) {
        //构造代理类
        Class<T> proxyClass;
        try {
            proxyClass = getProxyClass();
        } catch (Exception e) {
            future.completeExceptionally(e);
            return;
        }
        //创建注册中心
        registryRef = REGISTRY.get(registryUrl.getProtocol()).getRegistry(registryUrl);
        //如果提前注入了其它配置中心
        configureRef = configure == null ? registryRef : configure;
        //连接注册中心
        CompletableFuture<Void> f = !register && !subscribe && StringUtils.isEmpty(url) ? CompletableFuture.completedFuture(null) : registryRef.open();
        f.whenComplete((s, t) -> {
            if (t == null) {
                //订阅
                if (serviceUrl.getBoolean(Constants.SUBSCRIBE_OPTION)) {
                    registryRef.subscribe(serviceUrl, configHandler);
                } else {
                    //没有订阅的时候，主动complete
                    waitingConfig.complete(serviceUrl);
                }
            } else {
                waitingConfig.completeExceptionally(t);
            }
        });
        //构建调用器
        invokeHandler = new ConsumerInvokeHandler(refer, proxyClass);
        //构建代理
        proxy();

        //等待订阅的初始化配置
        waitingConfig.whenComplete((url, e) -> {
            if (e != null) {
                future.completeExceptionally(e);
            } else {
                //检查动态配置是否修改了别名，需要重新订阅
                resubscribe(serviceUrl, url);
                serviceUrl = url;
                try {
                    refer = InvokerManager.refer(this, registryRef);
                    invokeHandler.setInvoker(refer);
                    invokeHandler.setServiceUrl(refer.getUrl());
                    //open
                    refer.open().whenComplete(((v, t) -> {
                        if (t == null) {
                            future.complete(null);
                        } else {
                            future.completeExceptionally(t);
                        }
                    }));
                } catch (Throwable ex) {
                    future.completeExceptionally(ex);
                }
            }
        });
    }

    @Override
    protected void doUnRefer(final CompletableFuture<Void> future) {
        if (invokeHandler != null) {
            logger.info(String.format("Unrefer consumer config : %s with bean id %s", name(), getId()));
            invokeHandler = null;
            if (!waitingConfig.isDone()) {
                waitingConfig.completeExceptionally(new InitializationException("Unrefer interrupted waiting config."));
            }
            //有可能初始化还在等待配置，没有创建Refer
            if (refer != null) {
                refer.close().whenComplete(((v, t) -> {
                    if (t == null) {
                        future.complete(null);
                    } else {
                        future.completeExceptionally(t);
                    }
                }));
                refer = null;
            }
            configureRef=null;
            registryRef = null;
        }
        future.complete(null);
    }

    /**
     * 节点事件
     *
     * @param event
     */
    public void onNodeEvent(final NodeEvent event) {
        eventHandlers.forEach(h -> h.handle(event));
    }

    public URL getServiceUrl() {
        return serviceUrl;
    }

    public URL getRegistryUrl() {
        return registryUrl;
    }

    public Refer getRefer() {
        return refer;
    }

    @Override
    protected void onChanged(final URL newUrl, final long version) {
        Refer oldRefer = refer;
        //更新 consumer config refer
        Refer newRefer = InvokerManager.refer(this, newUrl, registryRef);
        newRefer.open().whenComplete((v, t) -> {
            if (t == null) {
                //异步并发，需要进行版本比较
                synchronized (counter) {
                    long newVersion = counter.get();
                    if (newVersion == version) {
                        //检查动态配置是否修改了别名，需要重新订阅
                        resubscribe(serviceUrl, newUrl);
                        invokeHandler.setInvoker(newRefer);
                        refer = newRefer;
                        serviceUrl = newUrl;
                        oldRefer.close(true);
                    } else {
                        logger.info(String.format("Discard out-of-date config. old=%d, current=%d", version, newVersion));
                        newRefer.close();
                    }
                }
            } else {
                //出现异常
                logger.error("Error occurs while referring after attribute changed", t);
                newRefer.close();
            }
        });
    }
}
