package io.joyrpc.spring;

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

import io.joyrpc.config.ProviderConfig;
import io.joyrpc.config.RegistryConfig;
import io.joyrpc.config.ServerConfig;
import io.joyrpc.spring.event.ConsumerReferDoneEvent;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.util.Switcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 服务提供者
 */
public class ProviderBean<T> extends ProviderConfig<T> implements InitializingBean, DisposableBean,
        ApplicationContextAware, ApplicationListener, BeanNameAware {

    /**
     * slf4j logger for this class
     */
    private final static Logger logger = LoggerFactory.getLogger(ProviderBean.class);
    /**
     * Spring容器
     */
    protected transient ApplicationContext applicationContext;

    protected CompletableFuture<Void> exportFuture;
    /**
     * 开关
     */
    protected Switcher switcher = new Switcher();

    /**
     * 默认构造函数，不允许从外部new
     */
    public ProviderBean() {

    }

    @Override
    public void setBeanName(String name) {
        this.id = name;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ConsumerReferDoneEvent || (event instanceof ContextRefreshedEvent && referCounter.get() == 0)) {
            //需要先判断条件，再打开
            switcher.open(() -> {
                logger.info(String.format("open provider with beanName %s after spring context refreshed.", id));
                try {
                    //TODO open的异常没有抛出来
                    exportFuture.whenComplete((v, t) -> open());
                } catch (Exception e) {
                    throw new InitializationException(String.format("Export %s not successes", serviceUrl), e);
                }
            });
        }
    }

    @Override
    public void afterPropertiesSet() {
        //如果没有配置协议，则默认发全部协议
        if (getServerConfig() == null) {
            Map<String, ServerConfig> beans = applicationContext.getBeansOfType(ServerConfig.class, false, false);
            if (!beans.isEmpty()) {
                setServerConfig(beans.values().iterator().next());
            }
        }
        //如果没有配置注册中心，则默认发布到全部注册中心
        if (getRegistry() == null) {
            Map<String, RegistryConfig> beans = applicationContext.getBeansOfType(RegistryConfig.class, false, false);
            if (!beans.isEmpty()) {
                setRegistry(new ArrayList<>(beans.values()));
            }
        }
        //没有open，服务不可用
        exportFuture = export();
    }

    @Override
    public void destroy() {
        logger.info(String.format("destroy provider with beanName %s", id));
        unexport();
    }

}
