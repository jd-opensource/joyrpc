package io.joyrpc.spring.processor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * 服务bean实例化DependsOn处理Processor
 */
public class DependsOnDefinitionPostProcessor implements BeanDefinitionRegistryPostProcessor {

    public static final String BEAN_NAME = "dependsOnDefinitionPostProcessor";

    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        InterfaceBeanDependsOn.getOrCreate(registry).doDependsOn();
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

}
