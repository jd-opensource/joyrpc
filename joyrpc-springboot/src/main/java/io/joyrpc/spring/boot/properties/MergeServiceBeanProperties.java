package io.joyrpc.spring.boot.properties;

/*-
 * #%L
 * joyrpc
 * %%
 * Copyright (C) 2019 joyrpc.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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

import static io.joyrpc.constants.Constants.PORT_OPTION;

/**
 * 合并注解与配置
 */
public class MergeServiceBeanProperties {

    /**
     * 环境
     */
    protected Environment env;

    /**
     * onsumerBean集合
     */
    protected Map<String, ConsumerBean> consumers = new HashMap<>();

    /**
     * ProviderBean 集合
     */
    protected Map<String, ProviderBean> providers = new HashMap<>();

    /**
     * 构造方法
     *
     * @param env
     * @param rpcProperties
     */
    public MergeServiceBeanProperties(Environment env, RpcProperties rpcProperties) {
        this.env = env;
        addAll(rpcProperties.getConsumers());
        addAll(rpcProperties.getProviders());
    }

    /**
     * 添加一个config bean列表
     *
     * @param serviceBeans
     * @param <T>
     */
    public <T extends AbstractInterfaceConfig> void addAll(List<T> serviceBeans) {
        if (serviceBeans != null && !serviceBeans.isEmpty()) {
            AtomicInteger i = new AtomicInteger();
            serviceBeans.forEach(bean -> add(bean, i.getAndIncrement()));
        }
    }

    /**
     * 添加一条config bean
     *
     * @param serviceBean
     * @param <T>
     */
    public <T extends AbstractInterfaceConfig> void add(T serviceBean) {
        add(serviceBean, -1);
    }

    /**
     * 添加一条config bean
     *
     * @param serviceBean
     * @param order
     * @param <T>
     */
    public <T extends AbstractInterfaceConfig> void add(T serviceBean, int order) {
        boolean isProvider = serviceBean instanceof ProviderBean;
        String reName = isProvider ? "provider-" : "consumer-";
        if (!StringUtils.hasText(serviceBean.getId())) {
            if (isProvider
                    && !StringUtils.hasText(((ProviderBean) serviceBean).getServerRef())
                    && StringUtils.hasText(serviceBean.getInterfaceClazz())
                    && StringUtils.hasText(serviceBean.getAlias())) {
                reName += serviceBean.getInterfaceClazz() + "-" + serviceBean.getAlias() + "-" + PORT_OPTION.getValue();
            } else if (!isProvider
                    && StringUtils.hasText(serviceBean.getInterfaceClazz())
                    && StringUtils.hasText(serviceBean.getAlias())) {
                reName += serviceBean.getInterfaceClazz() + "-" + serviceBean.getAlias();
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

    /**
     * 获取一个config bean
     *
     * @param name
     * @param beanType
     * @param <T>
     * @return
     */
    public <T extends AbstractInterfaceConfig> T get(String name, Class<T> beanType) {
        if (beanType.equals(ConsumerBean.class)) {
            return (T) consumers.get(name);
        } else {
            return (T) providers.get(name);
        }
    }

    /**
     * 获取一个config bean，若不存在，则创建
     *
     * @param name
     * @param beanType
     * @param supplier
     * @param <T>
     * @return
     */
    public <T extends AbstractInterfaceConfig> T compute(String name, Class<T> beanType, Supplier<T> supplier) {
        T config = get(name, beanType);
        if (config == null) {
            config = supplier.get();
            config.setId(name);
            add(config);
        }
        return config;
    }

    /**
     * 合并provider注解信息
     *
     * @param name
     * @param interfaceClazz
     * @param alias
     * @param refName
     * @return
     */
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

    /**
     * 合并consumer注解信息
     *
     * @param name
     * @param interfaceClazz
     * @param alias
     * @return
     */
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

    /**
     * 获取所有的ConsumerBean
     *
     * @return
     */
    public Map<String, ConsumerBean> getConsumers() {
        return consumers;
    }

    /**
     * 获取所有的ProviderBean
     *
     * @return
     */
    public Map<String, ProviderBean> getProviders() {
        return providers;
    }

}
