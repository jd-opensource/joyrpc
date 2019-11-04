package io.joyrpc.spring.factory;

import io.joyrpc.config.MethodConfig;
import io.joyrpc.config.ServerConfig;
import io.joyrpc.extension.Extension;
import io.joyrpc.spring.ProviderBean;
import io.joyrpc.spring.annotation.Provider;
import io.joyrpc.spring.util.AnnotationUtils;
import io.joyrpc.spring.util.MethodConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.support.*;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.util.ClassUtils.resolveClassName;

import static io.joyrpc.spring.factory.ServiceBeanDefinitionProcessor.*;

/**
 * 处理含有provider注解bean定义的处理类
 */
@Extension("provider")
public class ProviderDefinitionProcessor implements ServiceBeanDefinitionProcessor {

    private final Logger logger = LoggerFactory.getLogger(ProviderDefinitionProcessor.class);

    @Override
    public void processBean(BeanDefinition beanDefinition, BeanDefinitionRegistry registry, Environment environment, ClassLoader classLoader) {
        Class<?> refClass = resolveClassName(beanDefinition.getBeanClassName(), classLoader);
        Provider providerAnnotation = findAnnotation(refClass, Provider.class);
        if (providerAnnotation == null) {
            return;
        }
        //如果没有配置server，且没有注册默认的serverConfig，注册默认的serverConfig
        if (!StringUtils.hasText(providerAnnotation.server())
                && !registry.containsBeanDefinition(SERVER_NAME)) {
            registry.registerBeanDefinition(SERVER_NAME, new RootBeanDefinition(ServerConfig.class));
        }
        //注册ref
        BeanNameGenerator beanNameGenerator = resolveBeanNameGenerator(registry);
        String refBeanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
        registry.registerBeanDefinition(refBeanName, beanDefinition);
        //注册provider
        Class<?> interfaceClazz = null;
        if (void.class.equals(providerAnnotation.interfaceClass())) {
            Class<?>[] allInterfaces = refClass.getInterfaces();
            if (allInterfaces.length > 0) {
                interfaceClazz = allInterfaces[0];
            }
        } else {
            interfaceClazz = providerAnnotation.interfaceClass();
        }
        String providerBeanName = buildProviderBeanName(providerAnnotation, refBeanName);
        BeanDefinition providerDefinition = buildProviderDefinition(providerAnnotation, interfaceClazz, environment, refBeanName);
        registry.registerBeanDefinition(providerBeanName, providerDefinition);

    }

    private BeanNameGenerator resolveBeanNameGenerator(BeanDefinitionRegistry registry) {
        BeanNameGenerator beanNameGenerator = null;
        if (registry instanceof SingletonBeanRegistry) {
            SingletonBeanRegistry singletonBeanRegistry = SingletonBeanRegistry.class.cast(registry);
            beanNameGenerator = (BeanNameGenerator) singletonBeanRegistry.getSingleton(CONFIGURATION_BEAN_NAME_GENERATOR);
        }
        if (beanNameGenerator == null) {
            logger.info(String.format("BeanNameGenerator will be a instance of %s , it maybe a potential problem on bean name generation."
                    , AnnotationBeanNameGenerator.class.getName()));
            beanNameGenerator = new AnnotationBeanNameGenerator();
        }
        return beanNameGenerator;

    }

    private BeanDefinition buildProviderDefinition(Provider provider, Class<?> interfaceClass, Environment environment,
                                                   String refBeanName) {

        BeanDefinitionBuilder builder = rootBeanDefinition(ProviderBean.class);

        BeanDefinition beanDefinition = builder.getBeanDefinition();

        MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();

        String[] ignoreAttributeNames = new String[]{"ref", "id", "registry", "server", "methods", "interfaceClazz", "parameters"};
        propertyValues.addPropertyValues(new MutablePropertyValues(AnnotationUtils.getAttributes(provider, environment, true, ignoreAttributeNames)));

        addPropertyReference("ref", refBeanName, environment, builder);

        builder.addPropertyValue("id", buildProviderBeanName(provider, refBeanName));

        String[] registryConfigBeanNames = provider.registry();
        if (registryConfigBeanNames.length > 0) {
            List<RuntimeBeanReference> registryRuntimeBeanReferences = toRuntimeBeanReferences(environment, registryConfigBeanNames);
            if (!registryRuntimeBeanReferences.isEmpty()) {
                builder.addPropertyValue("registry", registryRuntimeBeanReferences);
            }
        }

        String serverBeanName = provider.server();
        if (StringUtils.hasText(serverBeanName)) {
            addPropertyReference("serverConfig", serverBeanName, environment, builder);
        } else {

        }

        Map<String, MethodConfig> methodConfigs = MethodConfigUtils.constructMethodConfig(provider.methods());
        builder.addPropertyValue("methods", methodConfigs);

        builder.addPropertyValue("interfaceClazz", interfaceClass.getName());

        builder.addPropertyValue("parameters", MethodConfigUtils.toStringMap(provider.parameters()));

        return builder.getBeanDefinition();

    }


}
