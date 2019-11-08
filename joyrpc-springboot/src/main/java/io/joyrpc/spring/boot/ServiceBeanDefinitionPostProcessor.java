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
import io.joyrpc.config.AbstractInterfaceConfig;
import io.joyrpc.spring.ConsumerBean;
import io.joyrpc.spring.ProviderBean;
import io.joyrpc.spring.boot.annotation.AnnotationProvider;
import io.joyrpc.util.Pair;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static io.joyrpc.spring.boot.Plugin.ANNOTATION_PROVIDER;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.context.annotation.AnnotationConfigUtils.registerAnnotationConfigProcessors;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.util.ClassUtils.resolveClassName;

/**
 * 注解扫描处理类
 */
public class ServiceBeanDefinitionPostProcessor implements BeanDefinitionRegistryPostProcessor,
        BeanPostProcessor, BeanClassLoaderAware {

    /**
     * 服务名称
     */
    public static final String SERVER_NAME = "server";
    /**
     * 注册中心名称
     */
    public static final String REGISTRY_NAME = "registry";

    public static final String BEAN_NAME = "serviceBeanDefinitionPostProcessor";
    public static final String RPC_PREFIX = "rpc.";
    public static final String PROVIDER_PREFIX = "provider-";
    public static final String CONSUMER_PREFIX = "consumer-";

    protected final ConfigurableEnvironment environment;

    protected final ResourceLoader resourceLoader;

    protected final ApplicationContext applicationContext;

    protected RpcProperties rpcProperties;

    protected ClassLoader classLoader;
    /**
     * ConsumerBean集合
     */
    protected Map<String, ConsumerBean> consumers = new HashMap<>();
    /**
     * ProviderBean 集合
     */
    protected Map<String, ProviderBean> providers = new HashMap<>();
    /**
     * 字段或方法上对应的消费者
     */
    protected Map<Member, ConsumerBean> members = new HashMap<>();
    /**
     * Consumer名称计数器
     */
    protected Map<String, AtomicInteger> consumerNameCounters = new HashMap<>();
    /**
     * Provider名称计数器
     */
    protected Map<String, AtomicInteger> providerNameCounters = new HashMap<>();

    /**
     * 构造方法
     */
    public ServiceBeanDefinitionPostProcessor(final ApplicationContext applicationContext,
                                              final ConfigurableEnvironment environment,
                                              final ResourceLoader resourceLoader) {
        this.applicationContext = applicationContext;
        this.environment = environment;
        this.resourceLoader = resourceLoader;
        this.rpcProperties = configure();
        //添加消费者
        if (rpcProperties.getConsumers() != null) {
            rpcProperties.getConsumers().forEach(c -> addConfig(c, consumers, consumerNameCounters));
        }
        //添加服务提供者
        if (rpcProperties.getProviders() != null) {
            rpcProperties.getProviders().forEach(c -> addConfig(c, providers, providerNameCounters));
        }
    }

    /**
     * 配置
     */
    protected RpcProperties configure() {
        RpcProperties result = new RpcProperties();
        //读取rpc为前缀的配置
        Map<String, Object> objectMap = getProperties();
        //绑定数据
        DataBinder dataBinder = new DataBinder(result);
        MutablePropertyValues propertyValues = new MutablePropertyValues(objectMap);
        dataBinder.bind(propertyValues);
        return result;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) throws BeansException {
        //扫描知道包下面的消费者和服务提供者注解
        Set<String> packages = new LinkedHashSet<>();
        if (rpcProperties.getPackages() != null) {
            rpcProperties.getPackages().forEach(pkg -> {
                if (StringUtils.hasText(pkg)) {
                    packages.add(environment.resolvePlaceholders(pkg.trim()));
                }
            });
        }
        processPackages(packages, registry);
        //注册Bean
        register(registry);
    }

    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        //查找所有的Consumer注解，检查是否注入了
        processConsumerAnnotation(bean.getClass(),
                (f, c) -> {
                    ConsumerBean config = members.get(f);
                    if (config != null) {
                        ReflectionUtils.makeAccessible(f);
                        ReflectionUtils.setField(f, bean, config.proxy());
                    }
                },
                (m, c) -> {
                    ConsumerBean config = members.get(m);
                    if (config != null) {
                        ReflectionUtils.invokeMethod(m, bean, config.proxy());
                    }
                });
        return bean;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * 添加配置者
     *
     * @param config
     * @param configs
     * @param counters
     */
    protected <T extends AbstractInterfaceConfig> void addConfig(final T config, final Map<String, T> configs,
                                                                 final Map<String, AtomicInteger> counters) {
        if (config == null) {
            return;
        }
        String name = computeName(config, counters);
        if (!StringUtils.isEmpty(name)) {
            if (configs.putIfAbsent(name, config) != null) {
                //名称冲突
                throw new BeanInitializationException("duplication bean name " + name);
            }
        }
    }

    /**
     * 计算名称
     *
     * @param config
     * @param counters
     * @param <T>
     * @return
     */
    protected <T extends AbstractInterfaceConfig> String computeName(final T config, final Map<String, AtomicInteger> counters) {
        String name = config.getId();
        String interfaceClazz = config.getInterfaceClazz();
        if (StringUtils.isEmpty(name) && !StringUtils.isEmpty(interfaceClazz)) {
            String namePrefix = config instanceof ProviderBean ? PROVIDER_PREFIX : CONSUMER_PREFIX;
            name = namePrefix + Introspector.decapitalize(ClassUtils.getShortName(interfaceClazz));
            if (counters != null) {
                AtomicInteger counter = counters.computeIfAbsent(name, n -> new AtomicInteger(0));
                int index = counter.get();
                name = index == 0 ? name : name + "-" + index;
            }
            config.setId(name);
        }
        return name;
    }

    /**
     * 通过注解添加消费者
     *
     * @param consumer
     * @param interfaceClazz
     */
    protected ConsumerBean addConsumer(final ConsumerBean consumer, final Class interfaceClazz) {
        consumer.setInterfaceClass(interfaceClazz);
        consumer.setInterfaceClazz(interfaceClazz.getName());
        //注解的不自动添加计数器
        String name = computeName(consumer, null);
        ConsumerBean old = consumers.putIfAbsent(name, consumer);
        if (old != null) {
            old.setInterfaceClazz(consumer.getInterfaceClazz());
            old.setInterfaceClass(interfaceClazz);
            if (StringUtils.isEmpty(old.getAlias())) {
                old.setAlias(consumer.getAlias());
            }
        }
        return old != null ? old : consumer;
    }

    /**
     * 通过注解添加服务提供者
     *
     * @param provider
     * @param interfaceClazz
     * @param refName
     */
    protected ProviderBean addProvider(final ProviderBean provider, final Class interfaceClazz, final String refName) {
        //这里不为空
        provider.setInterfaceClass(interfaceClazz);
        provider.setInterfaceClazz(interfaceClazz.getName());
        provider.setRefName(refName);
        //注解的不自动添加计数器
        ProviderBean old = providers.putIfAbsent(provider.getId(), provider);
        if (old != null) {
            if (StringUtils.isEmpty(old.getInterfaceClazz())) {
                old.setInterfaceClazz(provider.getInterfaceClazz());
            }
            if (StringUtils.isEmpty(old.getAlias())) {
                old.setAlias(provider.getAlias());
            }
            if (StringUtils.isEmpty(old.getRefName())) {
                old.setRefName(refName);
            }
        }
        return old != null ? old : provider;
    }


    /**
     * 注册
     */
    protected void register(final BeanDefinitionRegistry registry) {
        //注册
        register(registry, rpcProperties.getRegistry(), REGISTRY_NAME);
        register(registry, rpcProperties.getRegistries(), REGISTRY_NAME);
        register(registry, rpcProperties.getServer(), SERVER_NAME);
        register(registry, rpcProperties.getServers(), SERVER_NAME);
        consumers.forEach((name, c) -> register(c, registry));
        providers.forEach((name, p) -> register(p, registry));
    }

    /**
     * 注册消费者
     *
     * @param config
     * @param registry
     */
    protected void register(final ConsumerBean config, final BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder builder = genericBeanDefinition(ConsumerBean.class, () -> config);
        //引用reistry
        if (!StringUtils.isEmpty(config.getRegistryName())) {
            builder.addPropertyReference("registry", environment.resolvePlaceholders(config.getRegistryName()));
        }
        //注册
        registry.registerBeanDefinition(config.getName(), builder.getBeanDefinition());
    }

    /**
     * 注册服务提供者
     *
     * @param config
     * @param registry
     */
    protected void register(final ProviderBean config, final BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder builder = genericBeanDefinition(ProviderBean.class, () -> config);
        //引用ref
        builder.addPropertyReference("ref", config.getRefName());
        //引用注册中心
        List<String> registryNames = config.getRegistryNames();
        if (!CollectionUtils.isEmpty(registryNames)) {
            ManagedList<RuntimeBeanReference> runtimeBeanReferences = new ManagedList<>();
            for (String registryName : registryNames) {
                runtimeBeanReferences.add(new RuntimeBeanReference(environment.resolvePlaceholders(registryName)));
            }
            builder.addPropertyValue("registry", runtimeBeanReferences);
        }
        //引用Server
        if (!StringUtils.isEmpty(config.getServerName())) {
            builder.addPropertyReference("serverConfig", config.getServerName());
        }
        //注册
        registry.registerBeanDefinition(config.getName(), builder.getBeanDefinition());
    }

    /**
     * 处理rpc扫描的包下的class类
     *
     * @param packages
     * @param registry
     */
    protected void processPackages(Set<String> packages, BeanDefinitionRegistry registry) {
        //构造
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry, false, environment, resourceLoader);
        registerAnnotationConfigProcessors(registry);
        scanner.addIncludeFilter(new AnnotationFilter());
        //获取配置的rpc扫描包下的所有bean定义
        for (String basePackage : packages) {
            Set<BeanDefinition> definitions = scanner.findCandidateComponents(basePackage);
            if (!CollectionUtils.isEmpty(definitions)) {
                for (BeanDefinition definition : definitions) {
                    processConsumerAnnotation(definition);
                    processProviderAnnotation(definition, registry);
                }
            }
        }

    }

    /**
     * 处理消费者注解
     *
     * @param definition
     */
    protected void processConsumerAnnotation(final BeanDefinition definition) {
        Class<?> beanClass = resolveClassName(definition.getBeanClassName(), classLoader);
        processConsumerAnnotation(beanClass,
                (f, c) -> members.put(f, addConsumer(c, f.getType())),
                (m, c) -> members.put(m, addConsumer(c, m.getParameterTypes()[1])));
    }

    /**
     * 处理消费者注解
     *
     * @param fieldConsumer
     * @param methodConsumer
     */
    protected void processConsumerAnnotation(final Class beanClass,
                                             final BiConsumer<Field, ConsumerBean> fieldConsumer,
                                             final BiConsumer<Method, ConsumerBean> methodConsumer) {

        Class targetClass = beanClass;
        Pair<AnnotationProvider, Annotation> pair;
        while (targetClass != null && targetClass != Object.class) {
            //处理字段上的注解
            for (Field field : targetClass.getDeclaredFields()) {
                if (!Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
                    pair = getAnnotation(field);
                    if (pair != null) {
                        fieldConsumer.accept(field, pair.getKey().toConsumerBean(pair.getValue(), environment));
                    }
                }
            }
            //处理方法上的注解
            for (Method method : targetClass.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())
                        && method.getParameterCount() == 1
                        && method.getName().startsWith("set")) {
                    pair = getAnnotation(method);
                    if (pair != null) {
                        methodConsumer.accept(method, pair.getKey().toConsumerBean(pair.getValue(), environment));
                    }
                }
            }
            targetClass = targetClass.getSuperclass();
        }
    }

    /**
     * 处理服务提供者注解
     *
     * @param definition
     */
    protected void processProviderAnnotation(final BeanDefinition definition, BeanDefinitionRegistry registry) {
        Class<?> providerClass = resolveClassName(definition.getBeanClassName(), classLoader);
        //查找服务提供者注解
        Class targetClass = providerClass;
        Pair<AnnotationProvider, Annotation> pair = null;
        while (targetClass != null && targetClass != Object.class) {
            pair = getAnnotation(targetClass);
            if (pair != null) {
                break;
            }
            targetClass = targetClass.getSuperclass();
        }
        if (pair != null) {
            ProviderBean provider = pair.getKey().toProviderBean(pair.getValue(), environment);
            //获取接口类名
            Class interfaceClazz = getInterfaceClass(provider, providerClass);
            if (interfaceClazz == null || interfaceClazz == void.class) {
                //没有找到接口
                throw new BeanInitializationException("there is not any interface in class " + providerClass);
            }
            //获取服务实现类的Bean名称
            String refName = getComponentName(providerClass);
            if (refName == null) {
                refName = Introspector.decapitalize(ClassUtils.getShortName(providerClass.getName()));
                if (!registry.containsBeanDefinition(refName)) {
                    //注册服务实现类
                    registry.registerBeanDefinition(refName, definition);
                }
            }
            if (StringUtils.isEmpty(provider.getId())) {
                provider.setId(PROVIDER_PREFIX + refName);
            }
            //添加provider
            addProvider(provider, interfaceClazz, refName);
        }
    }

    /**
     * 获取组件名称
     *
     * @param providerClass
     * @return
     */
    protected String getComponentName(final Class<?> providerClass) {
        String name = null;
        Component component = findAnnotation(providerClass, Component.class);
        if (component != null) {
            name = component.value();
        }
        if (StringUtils.isEmpty(name)) {
            Service service = findAnnotation(providerClass, Service.class);
            if (service != null) {
                name = service.value();
            }
        }
        return name;
    }

    /**
     * 获取接口
     *
     * @param provider
     * @param providerClass
     * @return
     */
    protected Class getInterfaceClass(final ProviderBean provider, final Class<?> providerClass) {
        Class interfaceClazz = provider.getInterfaceClass();
        if (interfaceClazz == null || interfaceClazz == void.class) {
            Class<?>[] interfaces = providerClass.getInterfaces();
            if (interfaces.length == 1) {
                interfaceClazz = interfaces[0];
            } else if (interfaces.length > 1) {
                //多个接口，查找最佳匹配接口
                int max = -1;
                int priority;
                String providerClassName = providerClass.getSimpleName();
                String intfName;
                //计算最佳接口
                for (Class intf : interfaces) {
                    intfName = intf.getName();
                    if (intfName.startsWith("java")) {
                        priority = 0;
                    } else if (intfName.startsWith("javax")) {
                        priority = 0;
                    } else {
                        priority = providerClassName.startsWith(intf.getSimpleName()) ? 2 : 1;
                    }
                    if (priority > max) {
                        interfaceClazz = intf;
                        max = priority;
                    }
                }
            }
        }
        return interfaceClazz;
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
                register(registry, config, defNamePrefix + "-" + counter.getAndIncrement());
            }
        }
    }

    /**
     * 注册
     *
     * @param registry
     * @param config
     * @param defName
     * @param <T>
     */
    protected <T extends AbstractIdConfig> void register(final BeanDefinitionRegistry registry, final T config,
                                                         final String defName) {
        if (config == null) {
            return;
        }
        String beanName = config.getId();
        if (!StringUtils.hasText(beanName)) {
            beanName = defName;
        }
        if (!registry.containsBeanDefinition(beanName)) {
            registry.registerBeanDefinition(beanName, new RootBeanDefinition((Class<T>) config.getClass(), () -> config));
        } else {
            throw new BeanInitializationException("duplication bean name " + beanName);
        }
    }

    /**
     * 获取注解
     *
     * @param function
     * @return
     */
    protected Pair<AnnotationProvider, Annotation> getAnnotation(final Function<AnnotationProvider, Annotation> function) {
        Annotation result;
        for (AnnotationProvider provider : ANNOTATION_PROVIDER.extensions()) {
            result = function.apply(provider);
            if (result != null) {
                return Pair.of(provider, result);
            }
        }
        return null;
    }

    /**
     * 获取类上的服务提供者注解
     *
     * @param clazz
     * @return
     */
    protected Pair<AnnotationProvider, Annotation> getAnnotation(final Class clazz) {
        return getAnnotation(p -> clazz.getDeclaredAnnotation(p.getProviderAnnotationClass()));
    }

    /**
     * 获取方法上的消费者注解
     *
     * @param method
     * @return
     */
    protected Pair<AnnotationProvider, Annotation> getAnnotation(final Method method) {
        return getAnnotation(p -> method.getAnnotation(p.getConsumerAnnotationClass()));
    }

    /**
     * 获取字段上的消费者注解
     *
     * @param field
     * @return
     */
    protected Pair<AnnotationProvider, Annotation> getAnnotation(final Field field) {
        return getAnnotation(p -> field.getAnnotation(p.getConsumerAnnotationClass()));
    }

    /**
     * Get Sub {@link Properties}
     *
     * @return Map
     * @see Properties
     */
    public Map<String, Object> getProperties() {

        Map<String, Object> subProperties = new LinkedHashMap<String, Object>();

        MutablePropertySources propertySources = environment.getPropertySources();

        for (PropertySource<?> source : propertySources) {
            if (source instanceof EnumerablePropertySource) {
                for (String name : ((EnumerablePropertySource<?>) source).getPropertyNames()) {
                    if (!subProperties.containsKey(name) && name.startsWith(RPC_PREFIX)) {
                        String subName = name.substring(RPC_PREFIX.length());
                        if (!subProperties.containsKey(subName)) { // take first one
                            Object value = source.getProperty(name);
                            if (value instanceof String) {
                                // Resolve placeholder
                                value = environment.resolvePlaceholders((String) value);
                            }
                            subProperties.put(subName, value);
                        }
                    }
                }
            }
        }

        return Collections.unmodifiableMap(subProperties);

    }

    /**
     * 扫描类过滤（主要用来过滤含有某一个注解的类）
     */
    protected class AnnotationFilter implements TypeFilter {

        @Override
        public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
            ClassMetadata classMetadata = metadataReader.getClassMetadata();
            if (classMetadata.isConcrete() && !classMetadata.isAnnotation()) {
                //找到类
                Class clazz = resolveClassName(classMetadata.getClassName(), classLoader);
                //判断是否Public
                if (Modifier.isPublic(clazz.getModifiers())) {
                    Class targetClass = clazz;
                    while (targetClass != null && targetClass != Object.class) {
                        //处理类上的服务提供者注解
                        if (getAnnotation(targetClass) != null) {
                            return true;
                        }
                        //处理字段的消费者注解
                        for (Field field : targetClass.getDeclaredFields()) {
                            if (!Modifier.isFinal(field.getModifiers())
                                    && !Modifier.isStatic(field.getModifiers())
                                    && getAnnotation(field) != null) {
                                return true;
                            }
                        }
                        //处理方法上的消费者注解
                        for (Method method : clazz.getDeclaredMethods()) {
                            if (!Modifier.isStatic(method.getModifiers())
                                    && Modifier.isPublic(method.getModifiers())
                                    && method.getParameterCount() == 1
                                    && method.getName().startsWith("set")
                                    && getAnnotation(method) != null) {
                                return true;
                            }
                        }
                        targetClass = targetClass.getSuperclass();
                    }

                }
            }
            return false;
        }
    }


}
