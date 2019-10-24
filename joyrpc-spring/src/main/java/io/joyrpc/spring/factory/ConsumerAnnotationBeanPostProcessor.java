package io.joyrpc.spring.factory;

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

import io.joyrpc.spring.ConsumerBean;
import io.joyrpc.spring.annotation.Consumer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.context.*;
import org.springframework.context.event.ContextRefreshedEvent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumer注解解析器
 */
public class ConsumerAnnotationBeanPostProcessor extends AnnotationInjectedBeanPostProcessor<Consumer>
        implements ApplicationContextAware, ApplicationListener, ApplicationEventPublisherAware {

    public static final String BEAN_NAME = "consumerAnnotationBeanPostProcessor";

    protected final Map<String, ConsumerBean> beans = new ConcurrentHashMap<>(32);

    protected final Map<String, ReferenceBeanInvocationHandler> localReferenceBeanInvocationHandlerCache = new ConcurrentHashMap<>(32);

    protected ApplicationContext applicationContext;

    /**
     * 事件发布器
     */
    protected transient ApplicationEventPublisher applicationEventPublisher;

    @Override
    protected Object doGetInjectedBean(Consumer consumer, Object bean, String beanName, Class<?> injectedType,
                                       InjectionMetadata.InjectedElement injectedElement) {
        String name = buildBeanName(consumer, injectedType);
        ConsumerBean consumerBean = buildBean(name, consumer, injectedType, getClassLoader());

        InvocationHandler handler = buildInvocationHandler(name, consumerBean);
        return Proxy.newProxyInstance(getClassLoader(), new Class[]{injectedType}, handler);
    }

    private InvocationHandler buildInvocationHandler(String referencedBeanName, ConsumerBean referenceBean) {

        ReferenceBeanInvocationHandler handler = localReferenceBeanInvocationHandlerCache.get(referencedBeanName);

        if (handler == null) {
            handler = new ReferenceBeanInvocationHandler(referenceBean);
        }

        if (applicationContext.containsBean(referencedBeanName)) {
            localReferenceBeanInvocationHandlerCache.put(referencedBeanName, handler);
        } else {
            handler.init();
        }

        return handler;
    }

    @Override
    protected String buildInjectedObjectCacheKey(Consumer reference, Class<?> injectedType) {
        return buildBeanName(reference, injectedType);
    }

    /**
     * 构建Bean的名称
     *
     * @param consumer
     * @param injectedType
     * @return
     */
    protected String buildBeanName(Consumer consumer, Class<?> injectedType) {
        return AnnotationBeanNameBuilder.builder().alias(consumer.alias())
                .interfaceClassName(injectedType.getName())
                .environment(getEnvironment())
                .build();
    }

    /**
     * 构建Bean
     *
     * @param consumerBeanName
     * @param consumer
     * @param referencedType
     * @param classLoader
     * @return
     */
    protected ConsumerBean buildBean(final String consumerBeanName, final Consumer consumer,
                                     final Class<?> referencedType, final ClassLoader classLoader) {

        return beans.computeIfAbsent(consumerBeanName,
                o -> ConsumerBeanBuilder.builder().annotation(consumer).classLoader(classLoader)
                        .applicationContext(applicationContext).interfaceClass(referencedType).build()
                        .applicationEventPublisher(applicationEventPublisher));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            beans.forEach((beanName, consumerBean) -> consumerBean.onApplicationEvent((ContextRefreshedEvent) event));
        }
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void destroy() throws Exception {
        beans.clear();
        localReferenceBeanInvocationHandlerCache.clear();
        super.destroy();
    }

    private static class ReferenceBeanInvocationHandler implements InvocationHandler {

        private final ConsumerBean referenceBean;

        private Object bean;

        private ReferenceBeanInvocationHandler(ConsumerBean referenceBean) {
            this.referenceBean = referenceBean;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(bean, args);
        }

        private void init() {
            this.bean = referenceBean.getObject();
        }
    }
}

