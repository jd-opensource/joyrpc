package io.joyrpc.spring;

import io.joyrpc.cluster.discovery.config.Configure;
import io.joyrpc.config.AbstractConsumerConfig;
import io.joyrpc.config.RegistryConfig;
import io.joyrpc.spring.event.ConsumerReferDoneEvent;
import io.joyrpc.util.Shutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.*;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 消费者
 *
 * @param <T>
 */
public class ConsumerSpring<T> implements InitializingBean, FactoryBean,
        ApplicationContextAware, DisposableBean, BeanNameAware, ApplicationListener<ContextRefreshedEvent>, ApplicationEventPublisherAware {

    /**
     * slf4j logger for this class
     */
    private final static Logger logger = LoggerFactory.getLogger(ConsumerSpring.class);
    /**
     * consumer bean 计数
     */
    public transient static final AtomicInteger REFERS = new AtomicInteger(0);
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
     * 事件发布器
     */
    protected transient ApplicationEventPublisher applicationEventPublisher;
    /**
     * 等待完成
     */
    protected transient CountDownLatch latch = new CountDownLatch(1);
    /**
     * 初始化的Future
     */
    protected transient Throwable referThrowable;

    /**
     * 开关
     */
    protected transient AtomicBoolean started = new AtomicBoolean(false);

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
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public T getObject() {
        return config.getStub();
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
        //判断是否设置了配置中心
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
        //记录消费者的数量
        REFERS.incrementAndGet();
        config.validate();
        //生成代理，并创建引用
        config.refer().whenComplete((v, t) -> {
            if (t != null) {
                //出了异常
                referThrowable = t;
            }
            latch.countDown();
        });
    }


    @Override
    public void destroy() {
        if (!Shutdown.isShutdown()) {
            config.unrefer();
        }
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        //刷新事件会多次，防止重入
        if (started.compareAndSet(false, true)) {
            try {
                latch.await();
                if (referThrowable != null) {
                    logger.error(String.format("The system is about to exit, Failed refer %s/%s, caused by %s",
                            config.getInterfaceClazz(), config.getAlias(), referThrowable.getMessage()));
                    System.exit(1);
                } else if (REFERS.decrementAndGet() == 0) {
                    applicationEventPublisher.publishEvent(new ConsumerReferDoneEvent(true));
                }
            } catch (InterruptedException e) {
            }
        }
    }
}
