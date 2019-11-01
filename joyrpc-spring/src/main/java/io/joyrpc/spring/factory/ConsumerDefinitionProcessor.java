package io.joyrpc.spring.factory;

import io.joyrpc.config.MethodConfig;
import io.joyrpc.extension.Extension;
import io.joyrpc.spring.ConsumerBean;
import io.joyrpc.spring.annotation.Consumer;
import io.joyrpc.spring.util.AnnotationUtils;
import io.joyrpc.spring.util.MethodConfigUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.lang.reflect.Modifier;
import java.util.Map;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.springframework.util.ClassUtils.resolveClassName;
import static org.springframework.util.ReflectionUtils.doWithFields;
import static org.springframework.util.ReflectionUtils.doWithMethods;
import static io.joyrpc.spring.factory.ServiceBeanDefinitionProcessor.*;

/**
 * 处理含有consumer注解bean定义的处理类
 */
@Extension("consumer")
public class ConsumerDefinitionProcessor implements ServiceBeanDefinitionProcessor {

    @Override
    public void processBean(BeanDefinition beanDefinition, BeanDefinitionRegistry registry, Environment environment, ClassLoader classLoader) {
        Class<?> beanClass = resolveClassName(beanDefinition.getBeanClassName(), classLoader);
        //处理属上的consumer注解
        doWithFields(
                beanClass,
                field -> registryBean(field.getType(), field.getAnnotation(Consumer.class), registry, environment),
                field -> !Modifier.isFinal(field.getModifiers())
                        && !Modifier.isStatic(field.getModifiers())
                        && field.getAnnotation(Consumer.class) != null
        );

        //处理方法上的consumer注解
        doWithMethods(
                beanClass,
                method -> registryBean(method.getParameterTypes()[1], method.getAnnotation(Consumer.class), registry, environment),
                method -> method.getName().startsWith("set")
                        && method.getParameterCount() == 1
                        && method.getAnnotation(Consumer.class) != null
        );
    }

    protected void registryBean(Class interfaceClazz, Consumer consumerAnnotation, BeanDefinitionRegistry registry, Environment environment) {
        String consumerBeanName = buildConsumerBeanName(consumerAnnotation, interfaceClazz.getName());
        if (!registry.containsBeanDefinition(consumerBeanName)) {
            BeanDefinition consumerDefinition = buildConsumerDefinition(consumerAnnotation, interfaceClazz, environment);
            registry.registerBeanDefinition(consumerBeanName, consumerDefinition);
        }
    }


    private BeanDefinition buildConsumerDefinition(Consumer consumerAnnotation, Class<?> interfaceClass, Environment environment) {

        BeanDefinitionBuilder builder = rootBeanDefinition(ConsumerBean.class);

        BeanDefinition beanDefinition = builder.getBeanDefinition();

        MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();

        String[] ignoreAttributeNames = new String[]{"id", "registry", "methods", "interfaceClazz", "parameters"};
        propertyValues.addPropertyValues(new MutablePropertyValues(AnnotationUtils.getAttributes(consumerAnnotation, environment, true, ignoreAttributeNames)));


        builder.addPropertyValue("id", ServiceBeanDefinitionProcessor.buildConsumerBeanName(consumerAnnotation, interfaceClass.getName()));


        String registryBeanName = consumerAnnotation.registry();
        if (StringUtils.hasText(registryBeanName)) {
            addPropertyReference("registry", registryBeanName, environment, builder);
        }

        Map<String, MethodConfig> methodConfigs = MethodConfigUtils.constructMethodConfig(consumerAnnotation.methods());
        builder.addPropertyValue("methods", methodConfigs);

        builder.addPropertyValue("interfaceClazz", interfaceClass.getName());

        builder.addPropertyValue("parameters", MethodConfigUtils.toStringMap(consumerAnnotation.parameters()));

        return builder.getBeanDefinition();

    }

}
