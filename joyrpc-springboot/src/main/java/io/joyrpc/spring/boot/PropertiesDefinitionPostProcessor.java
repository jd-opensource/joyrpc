package io.joyrpc.spring.boot;

import io.joyrpc.config.RegistryConfig;
import io.joyrpc.config.ServerConfig;
import io.joyrpc.spring.ServerBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.util.StringUtils;

import java.util.List;

import static io.joyrpc.spring.factory.ServiceBeanDefinitionProcessor.ANNOTATION_DEFAULT_REGISTRY;
import static io.joyrpc.spring.factory.ServiceBeanDefinitionProcessor.ANNOTATION_DEFAULT_SERVER;

public class PropertiesDefinitionPostProcessor implements BeanDefinitionRegistryPostProcessor {

    private RpcProperties rpcProperties;

    public PropertiesDefinitionPostProcessor(RpcProperties rpcProperties) {
        this.rpcProperties = rpcProperties;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        ServerConfig defaultServer = rpcProperties.getServer();
        if (defaultServer != null && !registry.containsBeanDefinition(ANNOTATION_DEFAULT_SERVER)) {
            registry.registerBeanDefinition(ANNOTATION_DEFAULT_SERVER, new RootBeanDefinition(ServerConfig.class, () -> defaultServer));
        }
        RegistryConfig defaultRegistry = rpcProperties.getRegistry();
        if (defaultRegistry != null && !registry.containsBeanDefinition(ANNOTATION_DEFAULT_REGISTRY)) {
            registry.registerBeanDefinition(ANNOTATION_DEFAULT_REGISTRY, new RootBeanDefinition(RegistryConfig.class, () -> defaultRegistry));
        }
        List<ServerBean> serverConfigs = rpcProperties.getServers();
        if (serverConfigs != null && !serverConfigs.isEmpty()) {
            int i = 0;
            for (ServerBean serverBean : serverConfigs) {
                String beanName = serverBean.getId();
                if (!StringUtils.hasText(beanName)) {
                    beanName = "annotation-server-" + (i++);
                }
                if (!registry.containsBeanDefinition(beanName)) {
                    registry.registerBeanDefinition(beanName, new RootBeanDefinition(ServerConfig.class, () -> serverBean));
                }
            }
        }
        List<RegistryConfig> registryConfigs = rpcProperties.getRegistries();
        if (registryConfigs != null && !registryConfigs.isEmpty()) {
            int i = 0;
            for (RegistryConfig registryConfig : registryConfigs) {
                String beanName = registryConfig.getId();
                if (!StringUtils.hasText(beanName)) {
                    beanName = "annotation-registry-" + (i++);
                }
                if (!registry.containsBeanDefinition(beanName)) {
                    registry.registerBeanDefinition(beanName, new RootBeanDefinition(RegistryConfig.class, () -> registryConfig));
                }
            }
        }

    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
