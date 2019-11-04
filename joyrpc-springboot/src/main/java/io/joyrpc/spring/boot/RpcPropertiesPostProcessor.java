package io.joyrpc.spring.boot;

import io.joyrpc.config.AbstractIdConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static io.joyrpc.spring.factory.ServiceBeanDefinitionProcessor.REGISTRY_NAME;
import static io.joyrpc.spring.factory.ServiceBeanDefinitionProcessor.SERVER_NAME;

/**
 * 根据配置生成Server和Registry配置对象
 */
public class RpcPropertiesPostProcessor implements BeanDefinitionRegistryPostProcessor {

    protected RpcProperties rpcProperties;

    public RpcPropertiesPostProcessor(RpcProperties rpcProperties) {
        this.rpcProperties = rpcProperties;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) throws BeansException {
        register(registry, rpcProperties.getServer(), rpcProperties.getServers(), SERVER_NAME);
        register(registry, rpcProperties.getRegistry(), rpcProperties.getRegistries(), REGISTRY_NAME);
    }

    /**
     * 注册
     *
     * @param registry 注册表
     * @param def      默认配置
     * @param configs  多个配置
     * @param defName  默认名称
     */
    protected <T extends AbstractIdConfig> void register(final BeanDefinitionRegistry registry, final T def,
                                                         final List<T> configs, final String defName) {
        register(registry, def, c -> defName);
        if (configs != null) {
            AtomicInteger counter = new AtomicInteger(0);
            for (T config : configs) {
                register(registry, config, c -> defName + counter.getAndIncrement());
            }
        }
    }

    /**
     * 注册
     *
     * @param registry
     * @param config
     * @param function
     * @param <T>
     */
    protected <T extends AbstractIdConfig> void register(final BeanDefinitionRegistry registry, final T config,
                                                         final Function<T, String> function) {
        if (config == null) {
            return;
        }
        String beanName = config.getId();
        if (!StringUtils.hasText(beanName)) {
            beanName = function.apply(config);
        }
        if (!registry.containsBeanDefinition(beanName)) {
            //TODO 要验证是否正确注入了环境变量
            registry.registerBeanDefinition(beanName, new RootBeanDefinition((Class<T>) config.getClass(), () -> config));
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
