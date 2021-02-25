package io.joyrpc.spring.processor;

import io.joyrpc.spring.ConsumerBean;
import io.joyrpc.spring.ConsumerGroupBean;
import io.joyrpc.spring.GlobalParameterBean;
import io.joyrpc.spring.ProviderBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 服务bean实例化DependsOn处理Processor
 */
public class DependsOnDefinitionPostProcessor implements BeanDefinitionRegistryPostProcessor {

    public static final String BEAN_NAME = "dependsOnDefinitionPostProcessor";

    /**
     * 开关
     */
    private AtomicBoolean onDependsOn = new AtomicBoolean();

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        doDependsOn(registry);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    /**
     * dependsOn操作
     */
    protected void doDependsOn(BeanDefinitionRegistry registry) {
        if (onDependsOn.compareAndSet(false, true)) {
            Set<BeanDefinition> consumers = new HashSet<>();
            Set<BeanDefinition> consumerGroups = new HashSet<>();
            Set<BeanDefinition> providers = new HashSet<>();
            Set<String> contextNames = new HashSet<>();
            String[] names = registry.getBeanDefinitionNames();
            for (String name : names) {
                BeanDefinition definition = registry.getBeanDefinition(name);
                String beanClassName = definition.getBeanClassName();
                if (ConsumerBean.class.getName().equals(beanClassName)) {
                    consumers.add(definition);
                } else if (ProviderBean.class.getName().equals(beanClassName)) {
                    providers.add(definition);
                } else if (ConsumerGroupBean.class.getName().equals(beanClassName)) {
                    consumerGroups.add(definition);
                } else if (GlobalParameterBean.class.getName().equals(beanClassName)) {
                    contextNames.add(name);
                }
            }
            String[] dependOnNames = contextNames.toArray(new String[0]);
            consumers.forEach(definition -> definition.setDependsOn(dependOnNames));
            consumerGroups.forEach(definition -> definition.setDependsOn(dependOnNames));
            providers.forEach(definition -> definition.setDependsOn(dependOnNames));
        }
    }

}
