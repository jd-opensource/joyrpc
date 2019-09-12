package io.joyrpc.spring;

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

import io.joyrpc.config.ConsumerGroupConfig;
import io.joyrpc.config.RegistryConfig;
import io.joyrpc.spring.event.ConsumerReferDoneEvent;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.util.Switcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.*;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;


/**
 * 消费组
 */
public class ConsumerGroupBean<T> extends ConsumerGroupConfig<T> implements InitializingBean, FactoryBean,
        ApplicationContextAware, DisposableBean, BeanNameAware, ApplicationListener<ContextRefreshedEvent>, ApplicationEventPublisherAware {

    /**
     * slf4j logger for this class
     */
    private final static Logger logger = LoggerFactory.getLogger(ConsumerGroupBean.class);
    /**
     * spring上下文
     */
    protected transient ApplicationContext applicationContext;
    /**
     * 实例
     */
    protected transient T object;
    /**
     * 事件发布器
     */
    protected transient ApplicationEventPublisher applicationEventPublisher;
    /**
     * 开关
     */
    protected Switcher switcher = new Switcher();

    protected transient CountDownLatch latch = new CountDownLatch(1);

    /**
     * 默认构造函数
     */
    public ConsumerGroupBean() {

    }

    @Override
    public void setBeanName(String name) {
        this.id = name;
    }

    @Override
    public void setApplicationContext(final ApplicationContext appContext) throws BeansException {
        this.applicationContext = appContext;
    }

    @Override
    public T getObject() {
        return object;
    }

    @Override
    public void afterPropertiesSet() {
        // 如果没有配置注册中心，则默认订阅全部注册中心
        if (getRegistry() == null) {
            setRegistry(applicationContext.getBeansOfType(RegistryConfig.class, false, false));
        }
        referCounter.incrementAndGet();
        CompletableFuture<Void> openFuture = new CompletableFuture<>();
        openFuture.whenComplete((v, t) -> latch.countDown());
        object = refer(openFuture);
    }

    @Override
    public Class getObjectType() {
        return getProxyClass();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void destroy() {
        logger.info("destroy consumer group with bean name : {}", id);
        //TODO 如何做到优雅停机
        super.unrefer();
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        switcher.open(() -> {
            try {
                latch.await();
                if (referCounter.decrementAndGet() == 0) {
                    applicationEventPublisher.publishEvent(new ConsumerReferDoneEvent(true));
                }
            } catch (InterruptedException e) {
                throw new InitializationException("wait refer error", ExceptionCode.CONSUMER_REFER_WAIT_ERROR);
            }
        });
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
