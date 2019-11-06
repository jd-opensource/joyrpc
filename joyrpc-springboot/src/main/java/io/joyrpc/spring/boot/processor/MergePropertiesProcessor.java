package io.joyrpc.spring.boot.processor;

import io.joyrpc.spring.ConsumerBean;
import io.joyrpc.spring.ProviderBean;
import io.joyrpc.spring.boot.properties.MergeServiceBeanProperties;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.List;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

public class MergePropertiesProcessor {

    public void processProperties(final BeanDefinitionRegistry registry, final MergeServiceBeanProperties properties,
                                  Environment env) {
        properties.getConsumers().forEach((name, consumerBean) -> {
            consumerProcessor(registry, consumerBean, env);
        });
        properties.getProviders().forEach((name, providerBean) -> {
            providerProcessor(registry, providerBean, env);
        });
    }

    protected void consumerProcessor(final BeanDefinitionRegistry registry, final ConsumerBean consumerBean,
                                     final Environment env) {
        BeanDefinitionBuilder builder = genericBeanDefinition(ConsumerBean.class, () -> consumerBean);
        //引用reistry
        if (StringUtils.hasText(consumerBean.getRegistryRef())) {
            builder.addPropertyReference("registry", env.resolvePlaceholders(consumerBean.getRegistryRef()));
        }
        //注册
        registry.registerBeanDefinition(consumerBean.getName(), builder.getBeanDefinition());
    }

    protected void providerProcessor(final BeanDefinitionRegistry registry, final ProviderBean providerBean,
                                     final Environment env) {
        BeanDefinitionBuilder builder = genericBeanDefinition(ProviderBean.class, () -> providerBean);
        //引用ref
        builder.addPropertyReference("ref", providerBean.getRefRef());
        //引用注册中心
        List<String> registryNames = providerBean.getRegistryRefs();
        if (registryNames.size() > 0) {
            ManagedList<RuntimeBeanReference> runtimeBeanReferences = new ManagedList<>();
            for (String registryName : registryNames) {
                runtimeBeanReferences.add(new RuntimeBeanReference(env.resolvePlaceholders(registryName)));
            }
            builder.addPropertyValue("registry", runtimeBeanReferences);
        }
        //引用Server
        if (!providerBean.getServerRef().isEmpty()) {
            builder.addPropertyReference("serverConfig", providerBean.getServerRef());
        }
        //注册
        registry.registerBeanDefinition(providerBean.getName(), builder.getBeanDefinition());
    }
}
