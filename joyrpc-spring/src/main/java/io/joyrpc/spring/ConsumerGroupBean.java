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

import io.joyrpc.annotation.Alias;
import io.joyrpc.config.ConsumerGroupConfig;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.*;
import org.springframework.context.event.ContextRefreshedEvent;


/**
 * 消费组
 */
public class ConsumerGroupBean<T> extends ConsumerGroupConfig<T> implements InitializingBean, FactoryBean,
        ApplicationContextAware, DisposableBean, BeanNameAware, ApplicationListener<ContextRefreshedEvent>, ApplicationEventPublisherAware {

    /**
     * spring处理器
     */
    protected transient ConsumerSpring<T> spring;

    /**
     * 默认构造函数
     */
    public ConsumerGroupBean() {
        this.spring = new ConsumerSpring<>(this);
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

    public String getName() {
        return id;
    }

    public String getRegistryName() {
        return spring.getRegistryName();
    }

    @Alias("registry")
    public void setRegistryName(String registryName) {
        spring.setRegistryName(registryName);
    }

    public String getConfigureName() {
        return spring.getConfigureName();
    }

    @Alias("configure")
    public void setConfigureName(String configureName) {
        spring.setConfigureName(configureName);
    }
}
