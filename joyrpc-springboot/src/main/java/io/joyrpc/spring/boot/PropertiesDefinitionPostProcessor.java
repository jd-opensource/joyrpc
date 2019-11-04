package io.joyrpc.spring.boot;

import io.joyrpc.config.RegistryConfig;
import io.joyrpc.config.ServerConfig;
import io.joyrpc.spring.RegistryBean;
import io.joyrpc.spring.ServerBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.util.StringUtils;

import java.util.List;

import static io.joyrpc.spring.factory.ServiceBeanDefinitionProcessor.REGISTRY_NAME;
import static io.joyrpc.spring.factory.ServiceBeanDefinitionProcessor.SERVER_NAME;

/**
 * 根据配置生成Server和Registry配置对象
 */
public class PropertiesDefinitionPostProcessor implements BeanDefinitionRegistryPostProcessor {

    private RpcProperties rpcProperties;

    public PropertiesDefinitionPostProcessor(RpcProperties rpcProperties) {
        this.rpcProperties = rpcProperties;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) throws BeansException {
        //注册默认Server
        ServerBean defaultServer = rpcProperties.getServer();
        if (defaultServer != null && !registry.containsBeanDefinition(SERVER_NAME)) {
            registry.registerBeanDefinition(SERVER_NAME, new RootBeanDefinition(ServerConfig.class, () -> defaultServer));
        }
        //注册默认注册中心
        RegistryBean defaultRegistry = rpcProperties.getRegistry();
        if (defaultRegistry != null && !registry.containsBeanDefinition(REGISTRY_NAME)) {
            registry.registerBeanDefinition(REGISTRY_NAME, new RootBeanDefinition(RegistryConfig.class, () -> defaultRegistry));
        }
        //注册多个Server配置
        String beanName;
        List<ServerBean> serverConfigs = rpcProperties.getServers();
        if (serverConfigs != null && !serverConfigs.isEmpty()) {
            int i = 0;
            for (ServerBean bean : serverConfigs) {
                beanName = bean.getId();
                if (!StringUtils.hasText(beanName)) {
                    beanName = SERVER_NAME + (i++);
                }
                if (!registry.containsBeanDefinition(beanName)) {
                    registry.registerBeanDefinition(beanName, new RootBeanDefinition(ServerConfig.class, () -> bean));
                }
            }
        }
        //注册多个注册中心，名称为:registry0,registry1.....
        List<RegistryBean> registryConfigs = rpcProperties.getRegistries();
        if (registryConfigs != null && !registryConfigs.isEmpty()) {
            int i = 0;
            for (RegistryBean bean : registryConfigs) {
                beanName = bean.getId();
                if (!StringUtils.hasText(beanName)) {
                    beanName = REGISTRY_NAME + (i++);
                }
                if (!registry.containsBeanDefinition(beanName)) {
                    registry.registerBeanDefinition(beanName, new RootBeanDefinition(RegistryConfig.class, () -> bean));
                }
            }
        }

    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
