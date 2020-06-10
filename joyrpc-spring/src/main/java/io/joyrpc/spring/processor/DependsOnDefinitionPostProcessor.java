package io.joyrpc.spring.processor;

import io.joyrpc.spring.Counter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * 服务bean实例化DependsOn处理Processor
 */
public interface DependsOnDefinitionPostProcessor extends BeanDefinitionRegistryPostProcessor {

    default void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Counter.getOrCreate(registry).doDependsOn();
    }

    default void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

}
