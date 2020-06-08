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

import io.joyrpc.annotation.Alias;
import io.joyrpc.cluster.discovery.config.Configure;
import io.joyrpc.config.*;
import io.joyrpc.spring.event.ConsumerDoneEvent;
import io.joyrpc.spring.event.ContextDoneEvent;
import io.joyrpc.spring.event.ProviderDoneEvent;
import io.joyrpc.util.ClassUtils;
import io.joyrpc.util.Shutdown;
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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 服务提供者
 */
public class ProviderBean<T> extends ProviderConfig<T> implements InitializingBean, DisposableBean,
        ApplicationContextAware, ApplicationListener, BeanNameAware {

    /**
     * 参数配置
     */
    protected List<ParameterConfig> params;
    /**
     * slf4j logger for this class
     */
    private final static Logger logger = LoggerFactory.getLogger(ProviderBean.class);
    /**
     * Spring容器
     */
    protected transient ApplicationContext applicationContext;

    protected transient CompletableFuture<Void> exportFuture;
    /**
     * registryConfig 引用列表
     */
    protected transient List<String> registryNames;
    /**
     * server引用
     */
    protected String serverName;
    /**
     * ref引用
     */
    protected String refName;
    /**
     * 预热引用
     */
    protected String warmupName;
    /**
     * 消费者就绪开关
     */
    protected transient AtomicBoolean consumerDone = new AtomicBoolean();
    /**
     * 启动开关
     */
    protected transient AtomicBoolean startDone = new AtomicBoolean();
    /**
     * 服务bean计数器
     */
    protected transient Counter counter;
    /**
     * 配置中心名称
     */
    protected String configureName;

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
    public void afterPropertiesSet() {
        counter = Counter.getOrCreate(applicationContext);
        setupServer();
        setupRegistry();
        setupConfigure();
        setupRef();
        setupWarmup();
        validate();
        counter.incProvider();
    }

    @Override
    public void onApplicationEvent(final ApplicationEvent event) {
        if(event instanceof  ContextRefreshedEvent && !counter.hasConsumer()){
            onConsumerDone();
            if (startDone.compareAndSet(false, true)) {
                //主线程等待
                counter.startAndWaitAtLast();
            }
        }else if(event instanceof ConsumerDoneEvent){
            //等待消费者初始化完成，做到优雅启动
            //该事件通知线程不是主线程，不用startAndWait
            onConsumerDone();
        }
    }

    /**
     * 消费者就绪，启动服务监听
     */
    protected void onConsumerDone() {
        if (consumerDone.compareAndSet(false, true)) {
            exportFuture = export();
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
                        } else {
                            //启动完成，如果是最后一个，则触发打开阻塞
                            counter.successProvider(() -> CompletableFuture.runAsync(
                                    () -> applicationContext.publishEvent(new ProviderDoneEvent(this))));
                        }
                    });
                }
            });
        }
    }

    /**
     * 设置预热
     */
    protected void setupWarmup() {
        //接口实现
        if (warmup == null && !StringUtils.isEmpty(warmupName)) {
            warmup = applicationContext.getBean(warmupName, Warmup.class);
        }
    }

    /**
     * 设置实现对象
     */
    protected void setupRef() {
        //接口实现
        if (ref == null && !StringUtils.isEmpty(refName)) {
            ref = (T) applicationContext.getBean(refName);
        }
    }

    /**
     * 设置配置中心
     */
    protected void setupConfigure() {
        //判断是否设置了配置中心
        if (configure == null) {
            if (!StringUtils.isEmpty(configureName)) {
                configure = applicationContext.getBean(configureName, Configure.class);
            } else {
                Map<String, Configure> beans = applicationContext.getBeansOfType(Configure.class, false, false);
                if (beans != null && !beans.isEmpty()) {
                    Map.Entry<String, Configure> entry = beans.entrySet().iterator().next();
                    configure = entry.getValue();
                    logger.info(String.format("detect configure: %s for %s", entry.getKey(), getId()));
                }
            }
        }
    }

    /**
     * 设置注册中心
     */
    protected void setupRegistry() {
        //如果没有配置注册中心，则默认发布到全部注册中心
        if (StringUtils.isEmpty(registry)) {
            if (!StringUtils.isEmpty(registryNames)) {
                registry = registryNames.stream().map(name -> applicationContext.getBean(name, RegistryConfig.class)).collect(Collectors.toList());
            } else {
                Map<String, RegistryConfig> beans = applicationContext.getBeansOfType(RegistryConfig.class, false, false);
                if (!beans.isEmpty()) {
                    List<RegistryConfig> registryList = new ArrayList<>(beans.size());
                    List<String> nameList = new ArrayList<>(beans.size());
                    for (Map.Entry<String, RegistryConfig> entry : beans.entrySet()) {
                        registryList.add(entry.getValue());
                        nameList.add(entry.getKey());
                    }
                    setRegistry(registryList);
                    logger.info(String.format("detect registryConfig: %s for %s", String.join(",", nameList), getId()));
                }
            }
        }
    }

    /**
     * 设置服务参数
     */
    protected void setupServer() {
        //如果没有配置网络参数
        if (serverConfig == null) {
            //判断是否有引用对象配置
            if (!StringUtils.isEmpty(serverName)) {
                serverConfig = applicationContext.getBean(serverName, ServerConfig.class);
            } else {
                Map<String, ServerConfig> beans = applicationContext.getBeansOfType(ServerConfig.class, false, false);
                if (beans != null && !beans.isEmpty()) {
                    setServerConfig(beans.values().iterator().next());
                    Map.Entry<String, ServerConfig> entry = beans.entrySet().iterator().next();
                    setServerConfig(entry.getValue());
                    logger.info(String.format("detect serverConfig: %s for %s", entry.getKey(), getId()));
                }
            }
        }
    }

    @Override
    public void destroy() {
        if (!Shutdown.isShutdown()) {
            unexport();
        }
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

    public List<String> getRegistryNames() {
        return registryNames == null ? new ArrayList<>() : registryNames;
    }

    @Alias("registry")
    public void setRegistryNames(List<String> registryNames) {
        this.registryNames = registryNames;
    }

    public String getServerName() {
        return serverName == null ? "" : serverName;
    }

    @Alias("server")
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getRefName() {
        return refName;
    }

    @Alias("ref")
    public void setRefName(String refName) {
        this.refName = refName;
    }

    public String getWarmupName() {
        return warmupName;
    }

    @Alias("warmup")
    public void setWarmupName(String warmupName) {
        this.warmupName = warmupName;
    }

    public String getConfigureName() {
        return configureName;
    }

    @Alias("configure")
    public void setConfigureName(String configureName) {
        this.configureName = configureName;
    }

    /**
     * 获取接口类
     *
     * @param supplier 接口名称提供者
     * @return 接口类
     */
    public Class<?> getInterfaceClass(final Supplier<Class<?>> supplier) {
        if (interfaceClass == null) {
            if (interfaceClazz == null || interfaceClazz.isEmpty()) {
                interfaceClass = supplier.get();
                interfaceClazz = interfaceClass != null ? interfaceClass.getName() : interfaceClazz;
            } else {
                try {
                    interfaceClass = ClassUtils.forName(interfaceClazz);
                } catch (ClassNotFoundException e) {
                    interfaceClass = supplier.get();
                    interfaceClazz = interfaceClass != null ? interfaceClass.getName() : interfaceClazz;
                }
            }
        }
        return interfaceClass;
    }

    public List<ParameterConfig> getParams() {
        return params;
    }

    public void setParams(List<ParameterConfig> params) {
        this.params = params;
        if (params != null) {
            params.forEach(param -> {
                if (param != null
                        && io.joyrpc.util.StringUtils.isNotEmpty(param.getKey())
                        && io.joyrpc.util.StringUtils.isNotEmpty(param.getValue())) {
                    String key = param.isHide() && !param.getKey().startsWith(".") ? "." + param.getKey() : param.getKey();
                    setParameter(key, param.getValue());
                }
            });
        }
    }
}
