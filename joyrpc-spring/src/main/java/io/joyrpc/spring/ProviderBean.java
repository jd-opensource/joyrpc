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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.spring.ConsumerSpring.REFERS;

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
     * registryConfig 引用列表
     */
    protected transient List<String> registryRefs;
    /**
     * server引用
     */
    protected transient String serverRef;

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
        if (event instanceof ConsumerReferDoneEvent || (event instanceof ContextRefreshedEvent && REFERS.get() == 0)) {
            //需要先判断条件，再打开
            switcher.open(() -> {
                logger.info(String.format("open provider with beanName %s after spring context refreshed.", id));
                exportFuture.whenComplete((v, t) -> {
                    if (t != null) {
                        logger.error(String.format("Error occurs while export provider %s", id), t);
                        //export异常
                        System.exit(1);
                    } else {
                        open().whenComplete((s, e) -> {
                            if (e != null) {
                                logger.error(String.format("Error occurs while open provider %s", id), t);
                                //open异常
                                System.exit(1);
                            }
                        });
                    }
                });
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
        //先输出服务，并没有打开，服务不可用
        exportFuture = export();
    }

    @Override
    public void destroy() {
        logger.info(String.format("destroy provider with beanName %s", id));
        unexport();
    }

    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return id;
    }

    public void setName(String name) {
        this.id = name;
    }

    public List<String> getRegistryRefs() {
        return registryRefs == null ? new ArrayList<>() : registryRefs;
    }

    public void setRegistryRefs(List<String> registryRefs) {
        this.registryRefs = registryRefs;
    }

    public String getServerRef() {
        return serverRef == null ? "" : serverRef;
    }

    public void setServerRef(String serverRef) {
        this.serverRef = serverRef;
    }
}
