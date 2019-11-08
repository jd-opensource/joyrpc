package io.joyrpc.spring.boot.annotation;

import io.joyrpc.annotation.Consumer;
import io.joyrpc.annotation.Provider;
import io.joyrpc.extension.Extension;
import io.joyrpc.spring.ConsumerBean;
import io.joyrpc.spring.ProviderBean;
import org.springframework.core.env.Environment;

/**
 * 默认注解提供者
 */
@Extension("default")
public class DefaultAnnotationProvider implements AnnotationProvider<Provider, Consumer> {

    @Override
    public Class<Provider> getProviderAnnotationClass() {
        return Provider.class;
    }

    @Override
    public Class<Consumer> getConsumerAnnotationClass() {
        return Consumer.class;
    }

    @Override
    public ProviderBean toProviderBean(final Provider provider, final Environment environment) {
        ProviderBean config = new ProviderBean();
        //这里不为空
        config.setId(environment.resolvePlaceholders(provider.name()));
        config.setAlias(environment.resolvePlaceholders(provider.alias()));
        Class interfaceClass = provider.interfaceClass();
        if (interfaceClass != void.class) {
            config.setInterfaceClass(interfaceClass);
            config.setInterfaceClazz(interfaceClass.getName());
        }
        return config;
    }

    @Override
    public ConsumerBean toConsumerBean(final Consumer consumer, final Environment environment) {
        ConsumerBean config = new ConsumerBean();
        config.setId(environment.resolvePlaceholders(consumer.name()));
        config.setAlias(environment.resolvePlaceholders(consumer.alias()));
        return config;
    }
}
