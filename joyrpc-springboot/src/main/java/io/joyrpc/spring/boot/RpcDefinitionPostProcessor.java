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

import io.joyrpc.config.AbstractConsumerConfig;
import io.joyrpc.config.AbstractIdConfig;
import io.joyrpc.config.AbstractInterfaceConfig;
import io.joyrpc.config.ConsumerGroupConfig;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.spring.ConsumerBean;
import io.joyrpc.spring.ConsumerGroupBean;
import io.joyrpc.spring.ProviderBean;
import io.joyrpc.spring.boot.annotation.AnnotationProvider;
import io.joyrpc.util.Pair;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

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
import static org.springframework.util.ClassUtils.getShortName;
import static org.springframework.util.ClassUtils.resolveClassName;
import static org.springframework.util.StringUtils.hasText;
import static org.springframework.util.StringUtils.isEmpty;

/**
 * 注解扫描处理类
 */
public class RpcDefinitionPostProcessor implements BeanDefinitionRegistryPostProcessor,
        BeanPostProcessor, BeanClassLoaderAware {

    /**
     * 服务名称
     */
    public static final String SERVER_NAME = "server";
    /**
     * 注册中心名称
     */
    public static final String REGISTRY_NAME = "registry";

    public static final String BEAN_NAME = "rpcDefinitionPostProcessor";
    public static final String RPC_PREFIX = "rpc";
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
    protected Map<String, ConsumerBean<?>> consumers = new HashMap<>();
    /**
     * ConsumerGroupBean集合
     */
    protected Map<String, ConsumerGroupBean<?>> groups = new HashMap<>();
    /**
     * ProviderBean 集合
     */
    protected Map<String, ProviderBean<?>> providers = new HashMap<>();
    /**
     * 字段或方法上对应的消费者
     */
    protected Map<Member, AbstractConsumerConfig<?>> members = new HashMap<>();
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
    public RpcDefinitionPostProcessor(final ApplicationContext applicationContext,
                                      final ConfigurableEnvironment environment,
                                      final ResourceLoader resourceLoader) {
        this.applicationContext = applicationContext;
        this.environment = environment;
        this.resourceLoader = resourceLoader;
        this.rpcProperties = Binder.get(environment).bind(RPC_PREFIX, RpcProperties.class).orElseGet(RpcProperties::new);
        //添加消费者
        if (rpcProperties.getConsumers() != null) {
            rpcProperties.getConsumers().forEach(c -> addConfig(c, CONSUMER_PREFIX, consumerNameCounters, consumers));
        }
        //添加消费组
        if (rpcProperties.getGroups() != null) {
            rpcProperties.getGroups().forEach(c -> addConfig(c, CONSUMER_PREFIX, consumerNameCounters, groups));
        }
        //添加服务提供者
        if (rpcProperties.getProviders() != null) {
            rpcProperties.getProviders().forEach(c -> addConfig(c, PROVIDER_PREFIX, providerNameCounters, providers));
        }
    }

    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) throws BeansException {
        //扫描知道包下面的消费者和服务提供者注解
        Set<String> packages = new LinkedHashSet<>();
        if (rpcProperties.getPackages() != null) {
            rpcProperties.getPackages().forEach(pkg -> {
                if (hasText(pkg)) {
                    packages.add(pkg.trim());
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
        //再次查找所有的Consumer注解，检查是否注入了。
        processConsumerAnnotation(bean.getClass(),
                (f, c) -> {
                    AbstractConsumerConfig<?> config = members.get(f);
                    if (config != null) {
                        ReflectionUtils.makeAccessible(f);
                        ReflectionUtils.setField(f, bean, config.proxy());
                    }
                },
                (m, c) -> {
                    AbstractConsumerConfig<?> config = members.get(m);
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
     * @param config   配置
     * @param prefix   前缀
     * @param counters 计数器
     * @param configs  配置容器
     */
    protected <T extends AbstractInterfaceConfig> void addConfig(final T config,
                                                                 final String prefix,
                                                                 final Map<String, AtomicInteger> counters,
                                                                 final Map<String, T> configs) {
        if (config == null) {
            return;
        }
        String name = computeName(config, prefix, counters);
        if (!isEmpty(name)) {
            if (configs.putIfAbsent(name, config) != null) {
                //名称冲突
                throw new BeanInitializationException("duplication bean name " + name);
            }
        }
    }

    /**
     * 计算名称
     *
     * @param config   配置
     * @param prefix   前缀
     * @param counters 计数器
     * @param <T>
     * @return
     */
    protected <T extends AbstractInterfaceConfig> String computeName(final T config,
                                                                     final String prefix,
                                                                     final Map<String, AtomicInteger> counters) {
        String name = config.getId();
        String interfaceClazz = config.getInterfaceClazz();
        if (isEmpty(name) && !isEmpty(interfaceClazz)) {
            name = prefix + Introspector.decapitalize(getShortName(interfaceClazz));
            if (counters != null) {
                AtomicInteger counter = counters.computeIfAbsent(name, n -> new AtomicInteger(0));
                int index = counter.incrementAndGet();
                name = index == 1 ? name : name + "-" + index;
            }
            config.setId(name);
        }
        return name;
    }

    /**
     * 通过注解添加消费者
     *
     * @param consumer       消费者配置
     * @param interfaceClazz 接口类
     */
    protected AbstractConsumerConfig<?> addAnnotationConsumer(final ConsumerBean<?> consumer, final Class<?> interfaceClazz) {
        consumer.setInterfaceClass(interfaceClazz);
        consumer.setInterfaceClazz(interfaceClazz.getName());
        //注解的不自动添加计数器
        String name = computeName(consumer, CONSUMER_PREFIX, null);
        //先处理消费组的配置
        ConsumerGroupBean<?> groupBean = groups.get(name);
        if (groupBean != null) {
            //定义了消费组
            groupBean.setInterfaceClazz(consumer.getInterfaceClazz());
            groupBean.setInterfaceClass(interfaceClazz);
            if (isEmpty(groupBean.getAlias())) {
                groupBean.setAlias(consumer.getAlias());
            }
            return groupBean;
        } else {
            //普通消费者
            ConsumerBean<?> old = consumers.putIfAbsent(name, consumer);
            if (old != null) {
                old.setInterfaceClazz(consumer.getInterfaceClazz());
                old.setInterfaceClass(interfaceClazz);
                if (isEmpty(old.getAlias())) {
                    old.setAlias(consumer.getAlias());
                }
            }
            return old != null ? old : consumer;
        }
    }

    /**
     * 通过注解添加服务提供者
     *
     * @param provider       服务提供者
     * @param interfaceClazz 接口类
     * @param refName        引用对象
     */
    protected ProviderBean<?> addProvider(final ProviderBean<?> provider, final Class<?> interfaceClazz, final String refName) {
        //这里不为空
        provider.setInterfaceClass(interfaceClazz);
        provider.setInterfaceClazz(interfaceClazz.getName());
        provider.setRefName(refName);
        //注解的不自动添加计数器
        ProviderBean<?> old = providers.putIfAbsent(provider.getId(), provider);
        if (old != null) {
            if (isEmpty(old.getInterfaceClazz())) {
                old.setInterfaceClazz(provider.getInterfaceClazz());
            }
            if (isEmpty(old.getAlias())) {
                old.setAlias(provider.getAlias());
            }
            if (isEmpty(old.getRefName())) {
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
        String defRegName = register(registry, rpcProperties.getRegistry(), REGISTRY_NAME);
        String defServerName = register(registry, rpcProperties.getServer(), SERVER_NAME);
        register(registry, rpcProperties.getRegistries(), REGISTRY_NAME);
        register(registry, rpcProperties.getServers(), SERVER_NAME);
        consumers.forEach((name, c) -> register(c, registry, defRegName));
        groups.forEach((name, c) -> register(c, registry, defRegName));
        providers.forEach((name, p) -> register(p, registry, defRegName, defServerName));
        //注册全局参数
        if (rpcProperties.getParameters() != null) {
            //从配置文件读取，值已经做了占位符替换
            rpcProperties.getParameters().forEach(GlobalContext::put);
        }
    }

    /**
     * 注册消费者
     *
     * @param config     消费者配置
     * @param registry   BeanDefinitionRegistry
     * @param defRegName 默认注册中心
     */
    protected void register(final ConsumerBean<?> config, final BeanDefinitionRegistry registry, final String defRegName) {
        BeanDefinitionBuilder builder = genericBeanDefinition(ConsumerBean.class, () -> config)
                .setRole(RootBeanDefinition.ROLE_INFRASTRUCTURE);
        //这些不需要被再次Proxy，设置成ROLE_INFRASTRUCTURE，忽略Spring的警告
        if (config.getRegistry() == null
                && isEmpty(config.getRegistryName())
                && !isEmpty(defRegName)) {
            //引用registry
            config.setRegistryName(defRegName);
        }
        //注册
        registry.registerBeanDefinition(config.getName(), builder.getBeanDefinition());
    }

    /**
     * 注册消费者
     *
     * @param config     消费组配置
     * @param registry   BeanDefinitionRegistry
     * @param defRegName 默认注册中心
     */
    protected void register(final ConsumerGroupBean<?> config, final BeanDefinitionRegistry registry, final String defRegName) {
        BeanDefinitionBuilder builder = genericBeanDefinition(ConsumerGroupConfig.class, () -> config)
                .setRole(RootBeanDefinition.ROLE_INFRASTRUCTURE);
        //这些不需要被再次Proxy，设置成ROLE_INFRASTRUCTURE，忽略Spring的警告
        if (config.getRegistry() == null
                && isEmpty(config.getRegistryName())
                && !isEmpty(defRegName)) {
            //引用registry
            config.setRegistryName(defRegName);
        }
        //注册
        registry.registerBeanDefinition(config.getName(), builder.getBeanDefinition());
    }

    /**
     * 注册服务提供者
     *
     * @param config        服务提供者配置
     * @param registry      注册中心
     * @param defRegName    默认注册中心引用
     * @param defServerName 默认网络服务引用
     */
    protected void register(final ProviderBean<?> config, final BeanDefinitionRegistry registry, final String defRegName,
                            final String defServerName) {
        //这些不需要被再次Proxy，设置成ROLE_INFRASTRUCTURE，忽略Spring的警告
        BeanDefinitionBuilder builder = genericBeanDefinition(ProviderBean.class, () -> config)
                .setRole(RootBeanDefinition.ROLE_INFRASTRUCTURE);
        //判断是否设置了注册中心配置
        if (CollectionUtils.isEmpty(config.getRegistry())
                && CollectionUtils.isEmpty(config.getRegistryNames())
                && !isEmpty(defRegName)) {
            //引用注册中心
            config.setRegistryNames(Arrays.asList(defRegName));
        }
        //引用Server
        if (config.getServerConfig() == null
                && isEmpty(config.getServerName())
                && !isEmpty(defServerName)) {
            config.setServerName(defServerName);
        }
        //注册
        registry.registerBeanDefinition(config.getName(), builder.getBeanDefinition());
    }

    /**
     * 处理rpc扫描的包下的class类
     *
     * @param packages 包集合
     * @param registry 注册中心
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
     * @param definition bean定义
     */
    protected void processConsumerAnnotation(final BeanDefinition definition) {
        String className = definition.getBeanClassName();
        if (!isEmpty(className)) {
            Class<?> beanClass = resolveClassName(className, classLoader);
            processConsumerAnnotation(beanClass,
                    (f, c) -> members.put(f, addAnnotationConsumer(c, f.getType())),
                    (m, c) -> members.put(m, addAnnotationConsumer(c, m.getParameterTypes()[1])));
        }
    }

    /**
     * 处理消费者注解
     *
     * @param beanClass      bean类
     * @param fieldConsumer  字段消费者
     * @param methodConsumer 方法消费者
     */
    protected void processConsumerAnnotation(final Class<?> beanClass,
                                             final BiConsumer<Field, ConsumerBean<?>> fieldConsumer,
                                             final BiConsumer<Method, ConsumerBean<?>> methodConsumer) {

        Class<?> targetClass = beanClass;
        Pair<AnnotationProvider<Annotation, Annotation>, Annotation> pair;
        while (targetClass != null && targetClass != Object.class) {
            //处理字段上的注解
            for (Field field : targetClass.getDeclaredFields()) {
                if (!Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
                    pair = getConsumerAnnotation(field);
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
                    pair = getConsumerAnnotation(method);
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
     * @param definition bean定义
     * @param registry   注册中心
     */
    protected void processProviderAnnotation(final BeanDefinition definition, BeanDefinitionRegistry registry) {
        String className = definition.getBeanClassName();
        if (isEmpty(className)) {
            return;
        }
        Class<?> providerClass = resolveClassName(className, classLoader);
        //查找服务提供者注解
        Class<?> targetClass = providerClass;
        Pair<AnnotationProvider<Annotation, Annotation>, Annotation> pair = null;
        while (targetClass != null && targetClass != Object.class) {
            pair = getProviderAnnotation(targetClass);
            if (pair != null) {
                break;
            }
            targetClass = targetClass.getSuperclass();
        }
        if (pair != null) {
            ProviderBean<?> provider = pair.getKey().toProviderBean(pair.getValue(), environment);
            //获取接口类名
            Class<?> interfaceClazz = provider.getInterfaceClass(() -> getInterfaceClass(providerClass));
            if (interfaceClazz == null) {
                //没有找到接口
                throw new BeanInitializationException("there is not any interface in class " + providerClass);
            }
            //获取服务实现类的Bean名称
            String refName = getComponentName(providerClass);
            if (refName == null) {
                refName = Introspector.decapitalize(getShortName(providerClass.getName()));
                //注册服务实现对象
                if (!registry.containsBeanDefinition(refName)) {
                    registry.registerBeanDefinition(refName, definition);
                }
            }
            if (isEmpty(provider.getId())) {
                provider.setId(PROVIDER_PREFIX + refName);
            }
            //添加provider
            addProvider(provider, interfaceClazz, refName);
        }
    }

    /**
     * 获取组件名称
     *
     * @param providerClass 服务提供接口类
     * @return 服务名称
     */
    protected String getComponentName(final Class<?> providerClass) {
        String name = null;
        Component component = findAnnotation(providerClass, Component.class);
        if (component != null) {
            name = component.value();
        }
        if (isEmpty(name)) {
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
     * @param providerClass 服务提供者类
     * @return 服务接口类
     */
    protected Class<?> getInterfaceClass(final Class<?> providerClass) {
        Class<?> interfaceClazz = null;
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
            for (Class<?> intf : interfaces) {
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
     * @param registry BeanDefinitionRegistry
     * @param config   配置
     * @param defName  默认名称
     * @param <T>
     */
    protected <T extends AbstractIdConfig> String register(final BeanDefinitionRegistry registry, final T config,
                                                           final String defName) {
        if (config == null) {
            return null;
        }
        String beanName = config.getId();
        if (isEmpty(beanName)) {
            beanName = defName;
        }
        if (!registry.containsBeanDefinition(beanName)) {
            RootBeanDefinition definition = new RootBeanDefinition((Class<T>) config.getClass(), () -> config);
            //避免Spring警告信息
            definition.setRole(RootBeanDefinition.ROLE_INFRASTRUCTURE);
            registry.registerBeanDefinition(beanName, definition);
        } else {
            throw new BeanInitializationException("duplication bean name " + beanName);
        }
        return beanName;
    }

    /**
     * 获取注解
     *
     * @param function 函数
     * @return 注解提供者和注解的键值对
     */
    protected Pair<AnnotationProvider<Annotation, Annotation>, Annotation> getAnnotation(final Function<AnnotationProvider<Annotation, Annotation>, Annotation> function) {
        Annotation result;
        for (AnnotationProvider<Annotation, Annotation> provider : ANNOTATION_PROVIDER.extensions()) {
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
     * @param clazz 类
     * @return 注解提供者和注解的键值对
     */
    protected Pair<AnnotationProvider<Annotation, Annotation>, Annotation> getProviderAnnotation(final Class<?> clazz) {
        return getAnnotation(p -> clazz.getDeclaredAnnotation(p.getProviderAnnotationClass()));
    }

    /**
     * 获取方法上的消费者注解
     *
     * @param method 方法
     * @return 注解提供者和注解的键值对
     */
    protected Pair<AnnotationProvider<Annotation, Annotation>, Annotation> getConsumerAnnotation(final Method method) {
        return getAnnotation(p -> method.getAnnotation(p.getConsumerAnnotationClass()));
    }

    /**
     * 获取字段上的消费者注解
     *
     * @param field 字段
     * @return 注解提供者和注解的键值对
     */
    protected Pair<AnnotationProvider<Annotation, Annotation>, Annotation> getConsumerAnnotation(final Field field) {
        return getAnnotation(p -> field.getAnnotation(p.getConsumerAnnotationClass()));
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
                Class<?> clazz = resolveClassName(classMetadata.getClassName(), classLoader);
                //判断是否Public
                if (Modifier.isPublic(clazz.getModifiers())) {
                    Class<?> targetClass = clazz;
                    while (targetClass != null && targetClass != Object.class) {
                        //处理类上的服务提供者注解
                        if (getProviderAnnotation(targetClass) != null) {
                            return true;
                        }
                        //处理字段的消费者注解
                        for (Field field : targetClass.getDeclaredFields()) {
                            if (!Modifier.isFinal(field.getModifiers())
                                    && !Modifier.isStatic(field.getModifiers())
                                    && getConsumerAnnotation(field) != null) {
                                return true;
                            }
                        }
                        //处理方法上的消费者注解
                        for (Method method : clazz.getDeclaredMethods()) {
                            if (!Modifier.isStatic(method.getModifiers())
                                    && Modifier.isPublic(method.getModifiers())
                                    && method.getParameterCount() == 1
                                    && method.getName().startsWith("set")
                                    && getConsumerAnnotation(method) != null) {
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
