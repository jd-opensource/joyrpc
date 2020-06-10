package io.joyrpc.spring;

import io.joyrpc.cluster.discovery.config.Configure;
import io.joyrpc.config.AbstractConsumerConfig;
import io.joyrpc.config.RegistryConfig;
import io.joyrpc.spring.event.ConsumerDoneEvent;
import io.joyrpc.util.Shutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 消费者
 *
 * @param <T>
 */
public class ConsumerSpring<T> implements InitializingBean, FactoryBean,
        ApplicationContextAware, DisposableBean, BeanNameAware, ApplicationListener {

    /**
     * slf4j logger for this class
     */
    private static final Logger logger = LoggerFactory.getLogger(ConsumerSpring.class);
    /**
     * 抽象消费者类
     */
    protected AbstractConsumerConfig<T> config;

    /**
     * registry引用
     */
    protected String registryName;
    /**
     * 配置中心名称
     */
    protected String configureName;
    /**
     * spring上下文
     */
    protected transient ApplicationContext applicationContext;
    /**
     * 开关
     */
    protected transient AtomicBoolean startDone = new AtomicBoolean();
    /**
     * refer后返回的future
     */
    protected transient CompletableFuture<T> referFuture;
    /**
     * 服务bean计数器
     */
    protected transient Counter counter;

    /**
     * 构造函数
     *
     * @param config
     */
    public ConsumerSpring(AbstractConsumerConfig<T> config) {
        this.config = config;
    }

    public String getRegistryName() {
        return registryName;
    }

    public void setRegistryName(String registryName) {
        this.registryName = registryName;
    }

    public String getConfigureName() {
        return configureName;
    }

    public void setConfigureName(String configureName) {
        this.configureName = configureName;
    }

    @Override
    public void setBeanName(String name) {
        config.setId(name);
    }

    @Override
    public void setApplicationContext(final ApplicationContext appContext) {
        this.applicationContext = appContext;
    }

    @Override
    public T getObject() throws ExecutionException, InterruptedException {
        try {
            return referFuture.get();
        } catch (Exception e) {
            //出了异常
            logger.error(String.format("The system is about to exit, Failed refer %s/%s, caused by %s",
                    config.getServiceName(), config.getAlias(), e.getMessage()));
            throw e;
        }
    }

    @Override
    public Class<?> getObjectType() {
        // 如果spring注入在前，reference操作在后，则会提前走到此方法，此时interface为空
        try {
            return config.getProxyClass();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() {
        //如果没有配置注册中心，则默认订阅全部注册中心
        setupRegistry();
        //判断是否设置了配置中心
        setupConfigure();
        config.validate();
        //记录消费者的数量
        counter = Counter.getOrCreate((BeanDefinitionRegistry) applicationContext);
        counter.incConsumer();
        //生成代理，并创建引用
        referFuture = config.refer();
    }

    /**
     * 构建配置器
     */
    protected void setupConfigure() {
        if (config.getConfigure() == null) {
            if (!StringUtils.isEmpty(configureName)) {
                config.setConfigure(applicationContext.getBean(configureName, Configure.class));
            } else {
                Map<String, Configure> beans = applicationContext.getBeansOfType(Configure.class, false, false);
                if (!beans.isEmpty()) {
                    Map.Entry<String, Configure> entry = beans.entrySet().iterator().next();
                    config.setConfigure(entry.getValue());
                    logger.info(String.format("detect configure: %s for %s", entry.getKey(), config.getId()));
                }
            }
        }
    }

    /**
     * 构建注册中心
     */
    protected void setupRegistry() {
        if (config.getRegistry() == null) {
            if (!StringUtils.isEmpty(registryName)) {
                config.setRegistry(applicationContext.getBean(registryName, RegistryConfig.class));
            } else {
                Map<String, RegistryConfig> beans = applicationContext.getBeansOfType(RegistryConfig.class, false, false);
                if (!beans.isEmpty()) {
                    Map.Entry<String, RegistryConfig> entry = beans.entrySet().iterator().next();
                    config.setRegistry(entry.getValue());
                    logger.info(String.format("detect registryConfig: %s for %s", entry.getKey(), config.getId()));
                }
            }
        }
    }

    @Override
    public void destroy() {
        if (!Shutdown.isShutdown()) {
            config.unrefer();
        }
    }

    @Override
    public void onApplicationEvent(final ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            //判断是否启动过，防止重入
            if (startDone.compareAndSet(false, true)) {
                referFuture.whenComplete((v, t) -> {
                    if (t != null) {
                        //出了异常
                        logger.error(String.format("The system is about to exit, Failed refer %s/%s, caused by %s",
                                config.getServiceName(), config.getAlias(), t.getMessage()));
                        System.exit(1);
                    } else {
                        //消费者全部启动完成，异步通知，同步调用会造成Spring的锁阻塞
                        counter.successConsumer(() -> CompletableFuture.runAsync(
                                () -> applicationContext.publishEvent(new ConsumerDoneEvent(this))));
                    }
                });
                //主线程等待
                counter.startAndWaitAtLast();
            }
        }
    }

}
