package io.joyrpc.spring.boot.properties;

import io.joyrpc.config.AbstractInterfaceConfig;
import io.joyrpc.spring.ConsumerBean;
import io.joyrpc.spring.ProviderBean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class MergeServiceBeanProperties {

    protected Environment env;

    protected Map<String, ConsumerBean> consumers = new HashMap<>();

    protected Map<String, ProviderBean> providers = new HashMap<>();

    public MergeServiceBeanProperties(Environment env, RpcProperties rpcProperties) {
        this.env = env;
        addAll(rpcProperties.getConsumers());
        addAll(rpcProperties.getProviders());
    }

    public <T extends AbstractInterfaceConfig> void addAll(List<T> serviceBeans) {
        if (serviceBeans != null && !serviceBeans.isEmpty()) {
            AtomicInteger i = new AtomicInteger();
            serviceBeans.forEach(bean -> add(bean, i.getAndIncrement()));
        }
    }

    public <T extends AbstractInterfaceConfig> void add(T serviceBean) {
        add(serviceBean, -1);
    }

    public <T extends AbstractInterfaceConfig> void add(T serviceBean, int order) {
        String reName = serviceBean instanceof ConsumerBean ? "consumer-" : "provider-";
        if (!StringUtils.hasText(serviceBean.getId())) {
            if (StringUtils.hasText(serviceBean.getInterfaceClazz())
                    && StringUtils.hasText(serviceBean.getAlias())) {
                reName += serviceBean.getInterfaceClazz() + serviceBean.getAlias();
            } else if (order > -1) {
                reName += String.valueOf(order);
            }
            serviceBean.setId(reName);
        }
        if (serviceBean instanceof ConsumerBean) {
            consumers.put(((ConsumerBean) serviceBean).getName(), (ConsumerBean) serviceBean);
        } else {
            providers.put(((ProviderBean) serviceBean).getName(), (ProviderBean) serviceBean);
        }
    }

    public <T extends AbstractInterfaceConfig> T get(String name, Class<T> beanType) {
        if (beanType.equals(ConsumerBean.class)) {
            return (T) consumers.get(name);
        } else {
            return (T) providers.get(name);
        }
    }

    public <T extends AbstractInterfaceConfig> T compute(String name, Class<T> beanType, Supplier<T> supplier) {
        T config = get(name, beanType);
        if (config == null) {
            config = supplier.get();
            config.setId(name);
            add(config);
        }
        return config;
    }

    public ProviderBean mergeProvider(String name, String interfaceClazz, String alias, String refName) {
        ProviderBean providerBean = compute(name, ProviderBean.class, ProviderBean::new);
        if (!StringUtils.hasText(providerBean.getAlias())) {
            providerBean.setAlias(alias);
        }
        if (!StringUtils.hasText(providerBean.getInterfaceClazz())) {
            providerBean.setInterfaceClazz(interfaceClazz);
        }
        if (!StringUtils.hasText(providerBean.getRefRef())) {
            providerBean.setRefRef(refName);
        }
        return providerBean;
    }

    public ConsumerBean mergeConsumer(String name, String interfaceClazz, String alias) {
        ConsumerBean consumerBean = compute(name, ConsumerBean.class, ConsumerBean::new);
        if (!StringUtils.hasText(consumerBean.getAlias())) {
            consumerBean.setAlias(alias);
        }
        if (!StringUtils.hasText(consumerBean.getInterfaceClazz())) {
            consumerBean.setInterfaceClazz(interfaceClazz);
        }
        return consumerBean;
    }

    public Map<String, ConsumerBean> getConsumers() {
        return consumers;
    }

    public Map<String, ProviderBean> getProviders() {
        return providers;
    }

}
