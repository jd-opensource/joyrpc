package io.joyrpc.spring.factory;

import io.joyrpc.extension.Extensible;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;

@Extensible("configProcessor")
public interface ConfigPropertiesProcessor {

    void processProperties(BeanDefinitionRegistry registry, Environment environment);
}
