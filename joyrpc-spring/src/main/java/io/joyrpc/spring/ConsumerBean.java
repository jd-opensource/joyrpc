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
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.*;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * 消费者
 */
public class ConsumerBean<T> extends ConsumerConfig<T> implements InitializingBean, FactoryBean,
        ApplicationContextAware, DisposableBean, BeanNameAware, ApplicationListener<ContextRefreshedEvent>, ApplicationEventPublisherAware {

    /**
     * spring处理器
     */
    protected transient ConsumerSpring<T> spring;

    /**
     * registry引用
     */
    protected transient String registryRef;

    /**
     * 默认构造函数，不允许从外部new
     */
    public ConsumerBean() {
        spring = new ConsumerSpring(this);
    }

    @Override
    public void setBeanName(String name) {
        spring.setBeanName(name);
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        spring.setApplicationEventPublisher(publisher);
    }

    @Override
    public void setApplicationContext(ApplicationContext context) {
        spring.setApplicationContext(context);
    }

    @Override
    public T getObject() {
        return spring.getObject();
    }

    @Override
    public Class getObjectType() {
        return spring.getObjectType();
    }

    @Override
    public boolean isSingleton() {
        return spring.isSingleton();
    }

    @Override
    public void destroy() {
        spring.destroy();
    }

    @Override
    public synchronized void onApplicationEvent(final ContextRefreshedEvent contextRefreshedEvent) {
        spring.onApplicationEvent(contextRefreshedEvent);
    }

    @Override
    public void afterPropertiesSet() {
        spring.afterPropertiesSet();
    }

    public ConsumerBean<T> applicationEventPublisher(ApplicationEventPublisher publisher) {
        setApplicationEventPublisher(publisher);
        return this;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return id;
    }

    public void setName(String name) {
        this.id = name;
    }

    public String getRegistryRef() {
        return registryRef;
    }

    public void setRegistryRef(String registryRef) {
        this.registryRef = registryRef;
    }
}
