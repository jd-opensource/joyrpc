package io.joyrpc.spring.factory;

import io.joyrpc.extension.Extensible;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;

/**
 * 含有consumer与provider注解的bean定义的处理类插件
 */
@Extensible("serviceBeanDefinitionProcessor")
public interface ServiceBeanDefinitionProcessor {
    /**
     * 服务名称
     */
    String SERVER_NAME = "server";
    /**
     * 注册中心名称
     */
    String REGISTRY_NAME = "registry";

    /**
     * 处理Bean定义
     *
     * @param beanDefinition
     * @param registry
     * @param environment
     * @param classLoader
     */
    void processBean(BeanDefinition beanDefinition, BeanDefinitionRegistry registry,
                     Environment environment, ClassLoader classLoader);

}
