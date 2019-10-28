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

import io.joyrpc.config.ConsumerConfig;
import io.joyrpc.config.RegistryConfig;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.spring.event.ConsumerReferDoneEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.*;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.concurrent.CountDownLatch;

/**
 * 消费者
 */
public class ConsumerBean<T> extends ConsumerConfig<T> implements InitializingBean, FactoryBean,
        ApplicationContextAware, DisposableBean, BeanNameAware, ApplicationListener<ContextRefreshedEvent>, ApplicationEventPublisherAware {

    /**
     * slf4j logger for this class
     */
    private final static Logger logger = LoggerFactory.getLogger(ConsumerBean.class);
    /**
     * spring的上下文
     */
    protected transient ApplicationContext applicationContext;
    /**
     * 事件发布器
     */
    protected transient ApplicationEventPublisher applicationEventPublisher;
    /**
     * 等待完成
     */
    protected transient CountDownLatch latch = new CountDownLatch(1);
    /**
     * 初始化的Future
     */
    protected transient Throwable referThrowable;

    /**
     * 默认构造函数，不允许从外部new
     */
    public ConsumerBean() {
    }

    @Override
    public void setBeanName(String name) {
        this.id = name;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.applicationEventPublisher = publisher;
    }

    @Override
    public void setApplicationContext(ApplicationContext appContext) throws BeansException {
        this.applicationContext = appContext;
    }

    @Override
    public T getObject() {
        return stub;
    }

    @Override
    public Class getObjectType() {
        // 如果spring注入在前，reference操作在后，则会提前走到此方法，此时interface为空
        try {
            return getProxyClass();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void destroy() {
        logger.info(String.format("destroy consumer with bean name : %s", id));
        unrefer();
    }

    @Override
    public synchronized void onApplicationEvent(final ContextRefreshedEvent contextRefreshedEvent) {
        try {
            latch.await();
            if (referThrowable != null) {
                //创建引用失败
                throw new InitializationException(String.format("Error occurs while referring consumer bean %s", id),
                        referThrowable, ExceptionCode.CONSUMER_REFER_WAIT_ERROR);
            }
            if (referCounter.decrementAndGet() == 0) {
                applicationEventPublisher.publishEvent(new ConsumerReferDoneEvent(true));
            }
        } catch (InterruptedException e) {
            throw new InitializationException("wait refer error", ExceptionCode.CONSUMER_REFER_WAIT_ERROR);
        }
    }

    @Override
    public void afterPropertiesSet() {
        //在setApplicationContext调用
        // 如果没有配置注册中心，则默认订阅全部注册中心
        if (getRegistry() == null) {
            setRegistry(applicationContext.getBeansOfType(RegistryConfig.class, false, false));
        }
        //记录消费者的数量
        referCounter.incrementAndGet();
        //生成代理，并创建引用
        refer().whenComplete((v, t) -> {
            if (t != null) {
                //出了异常
                referThrowable = t;
            }
            latch.countDown();
        });
    }

    public ConsumerBean<T> applicationEventPublisher(ApplicationEventPublisher publisher) {
        setApplicationEventPublisher(publisher);
        return this;
    }
}
