package io.joyrpc.spring.factory;

import io.joyrpc.spring.annotation.Consumer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import static io.joyrpc.spring.factory.ServiceBeanDefinitionRegistryProcessor.*;

/**
 * congsumer注解注入
 */
public class ConsumerInjectedPostProcessor extends AnnotationInjectedBeanPostProcessor<Consumer> implements ApplicationContextAware {

    public static final String BEAN_NAME = "consumerInjectedPostProcessor";

    private ApplicationContext applicationContext;

    @Override
    protected Object doGetInjectedBean(Consumer annotation, Object bean, String beanName, Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) throws Exception {
        return applicationContext.getBean(buildConsumerBeanName(annotation, injectedType.getName()), injectedType);
    }

    @Override
    protected String buildInjectedObjectCacheKey(Consumer annotation, Class<?> injectedType) {
        return buildConsumerBeanName(annotation, injectedType.getName());
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
