package io.joyrpc.spring.factory;

import io.joyrpc.extension.Extensible;
import io.joyrpc.spring.annotation.Consumer;
import io.joyrpc.spring.annotation.Provider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.core.env.Environment;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 含有consumer与provider注解的bean定义的处理类插件
 */
@Extensible("serviceBeanDefinitionRegistryProcessor")
public interface ServiceBeanDefinitionRegistryProcessor {

    void processBean(BeanDefinition beanDefinition, BeanDefinitionRegistry registry, Environment environment, ClassLoader classLoader);

    default void addPropertyReference(String propertyName, String beanName, Environment environment, BeanDefinitionBuilder builder) {
        String resolvedBeanName = environment.resolvePlaceholders(beanName);
        builder.addPropertyReference(propertyName, resolvedBeanName);
    }

    static String buildConsumerBeanName(Consumer consumerAnnotation, String interfaceClazz) {
        return interfaceClazz + "#" + consumerAnnotation.alias();
    }

    static String buildProviderBeanName(Provider providerAnnotation, String refBeanName) {
        if (StringUtils.hasText(providerAnnotation.name())) {
            return providerAnnotation.name();
        }
        return refBeanName + "#provider";
    }

    static ManagedList<RuntimeBeanReference> toRuntimeBeanReferences(Environment environment, String... beanNames) {
        ManagedList<RuntimeBeanReference> runtimeBeanReferences = new ManagedList<>();
        if (!ObjectUtils.isEmpty(beanNames)) {
            for (String beanName : beanNames) {
                String resolvedBeanName = environment.resolvePlaceholders(beanName);
                runtimeBeanReferences.add(new RuntimeBeanReference(resolvedBeanName));
            }
        }
        return runtimeBeanReferences;
    }
}
