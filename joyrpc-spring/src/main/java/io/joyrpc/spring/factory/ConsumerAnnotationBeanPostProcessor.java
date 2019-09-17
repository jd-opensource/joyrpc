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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Consumer注解解析器
 */
public class ConsumerAnnotationBeanPostProcessor extends AnnotationInjectedBeanPostProcessor<Consumer>
        implements ApplicationContextAware, ApplicationListener {

    public static final String BEAN_NAME = "consumerAnnotationBeanPostProcessor";

    /**
     * Cache size
     */
    private static final int CACHE_SIZE = Integer.getInteger(BEAN_NAME + ".cache.size", 32);

    private final ConcurrentMap<String, ConsumerBean<?>> referenceBeanCache = new ConcurrentHashMap<String, ConsumerBean<?>>(CACHE_SIZE);

    private final ConcurrentHashMap<String, ReferenceBeanInvocationHandler> localReferenceBeanInvocationHandlerCache = new ConcurrentHashMap<String, ReferenceBeanInvocationHandler>(CACHE_SIZE);

    private final ConcurrentMap<InjectionMetadata.InjectedElement, ConsumerBean<?>> injectedFieldReferenceBeanCache = new ConcurrentHashMap<InjectionMetadata.InjectedElement, ConsumerBean<?>>(CACHE_SIZE);

    private final ConcurrentMap<InjectionMetadata.InjectedElement, ConsumerBean<?>> injectedMethodReferenceBeanCache = new ConcurrentHashMap<InjectionMetadata.InjectedElement, ConsumerBean<?>>(CACHE_SIZE);

    private ApplicationContext applicationContext;


    @Override
    protected Object doGetInjectedBean(Consumer consumer, Object bean, String beanName, Class<?> injectedType,
                                       InjectionMetadata.InjectedElement injectedElement) throws Exception {

        String referencedBeanName = buildReferencedBeanName(consumer, injectedType);

        ConsumerBean referenceBean = buildReferenceBeanIfAbsent(referencedBeanName, consumer, injectedType, getClassLoader());

        cacheInjectedReferenceBean(referenceBean, injectedElement);

        InvocationHandler handler = buildInvocationHandler(referencedBeanName, referenceBean);
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

    @Override
    protected String buildInjectedObjectCacheKey(Consumer reference, Class<?> injectedType) {
        return buildReferencedBeanName(reference, injectedType);
    }

    private String buildReferencedBeanName(Consumer consumer, Class<?> injectedType) {

        AnnotationBeanNameBuilder builder = AnnotationBeanNameBuilder.create(consumer, injectedType, getEnvironment());

        return getEnvironment().resolvePlaceholders(builder.build());
    }

    private ConsumerBean buildReferenceBeanIfAbsent(String consumerBeanName, Consumer consumer,
                                                    Class<?> referencedType, ClassLoader classLoader) throws Exception {

        ConsumerBean<?> referenceBean = referenceBeanCache.get(consumerBeanName);

        if (referenceBean == null) {
            ConsumerBeanBuilder beanBuilder = ConsumerBeanBuilder
                    .create(consumer, classLoader, applicationContext)
                    .interfaceClass(referencedType);
            referenceBean = beanBuilder.build();
            referenceBeanCache.put(consumerBeanName, referenceBean);
        }

        return referenceBean;
    }

    private void cacheInjectedReferenceBean(ConsumerBean referenceBean,
                                            InjectionMetadata.InjectedElement injectedElement) {
        if (injectedElement.getMember() instanceof Field) {
            injectedFieldReferenceBeanCache.put(injectedElement, referenceBean);
        } else if (injectedElement.getMember() instanceof Method) {
            injectedMethodReferenceBeanCache.put(injectedElement, referenceBean);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {

    }

    @Override
    public void destroy() throws Exception {
        super.destroy();
        this.referenceBeanCache.clear();
        this.localReferenceBeanInvocationHandlerCache.clear();
        this.injectedFieldReferenceBeanCache.clear();
        this.injectedMethodReferenceBeanCache.clear();
    }
}

