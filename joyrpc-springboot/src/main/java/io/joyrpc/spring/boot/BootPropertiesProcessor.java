package io.joyrpc.spring.boot;

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

import io.joyrpc.config.AbstractIdConfig;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.spring.factory.ConfigPropertiesProcessor;
import io.joyrpc.spring.util.PropertySourcesUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static io.joyrpc.spring.boot.BootRpcProperties.PREFIX;
import static io.joyrpc.spring.factory.ServiceBeanDefinitionProcessor.REGISTRY_NAME;
import static io.joyrpc.spring.factory.ServiceBeanDefinitionProcessor.SERVER_NAME;

/**
 * 根据配置生成Server和Registry配置对象
 */
@Extension("boot")
@ConditionalOnClass({"org.springframework.boot.autoconfigure.EnableAutoConfiguration"})
public class BootPropertiesProcessor implements ConfigPropertiesProcessor {

    protected static final String SERVERS_NAME_PREFIX = "ServerBean-";

    protected static final String REGISTRY_NAME_PREFIX = "RegistryBean-";

    @Override
    public void processProperties(final BeanDefinitionRegistry registry, final Environment environment) throws BeansException {
        BootRpcProperties properties = new BootRpcProperties();
        //读取rpc为前缀的配置
        Map<String, Object> objectMap = PropertySourcesUtils.getSubProperties((ConfigurableEnvironment) environment, PREFIX);
        //绑定数据
        DataBinder dataBinder = new DataBinder(properties);
        MutablePropertyValues propertyValues = new MutablePropertyValues(objectMap);
        dataBinder.bind(propertyValues);
        //注册
        register(registry, properties.getRegistry(), c -> REGISTRY_NAME);
        register(registry, properties.getServer(), c -> SERVER_NAME);
        register(registry, properties.getServers(), SERVERS_NAME_PREFIX);
        register(registry, properties.getRegistries(), REGISTRY_NAME_PREFIX);
    }

    /**
     * 注册
     *
     * @param registry      注册表
     * @param configs       多个配置
     * @param defNamePrefix 默认名称
     */
    protected <T extends AbstractIdConfig> void register(final BeanDefinitionRegistry registry,
                                                         final List<T> configs, final String defNamePrefix) {
        if (configs != null) {
            AtomicInteger counter = new AtomicInteger(0);
            for (T config : configs) {
                register(registry, config, c -> defNamePrefix + "-" + counter.getAndIncrement());
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

}
