package io.joyrpc.spring;

import io.joyrpc.config.AbstractConsumerConfig;
import io.joyrpc.config.RegistryConfig;
import io.joyrpc.spring.event.ConsumerReferDoneEvent;
import io.joyrpc.util.Shutdown;
import io.joyrpc.util.Switcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.*;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 消费者
 *
 * @param <T>
 */
public class ConsumerSpring<T> implements InitializingBean, FactoryBean,
        ApplicationContextAware, DisposableBean, BeanNameAware, ApplicationListener<ContextRefreshedEvent>, ApplicationEventPublisherAware {

    /**
     * consumer bean 计数
     */
    public transient static final AtomicInteger REFERS = new AtomicInteger(0);
    /**
     * slf4j logger for this class
     */
    private final static Logger logger = LoggerFactory.getLogger(ConsumerGroupBean.class);
    /**
     * 抽象消费者类
     */
    protected AbstractConsumerConfig<T> config;
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
    protected Switcher switcher = new Switcher();

    /**
     * 构造函数
     *
     * @param config
     */
    public ConsumerSpring(AbstractConsumerConfig<T> config) {
        this.config = config;
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
    public Class getObjectType() {
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
            Map<String, RegistryConfig> registries = applicationContext.getBeansOfType(RegistryConfig.class, false, false);
            if (registries != null && !registries.isEmpty()) {
                config.setRegistry(registries.values().iterator().next());
            }
        }
        //记录消费者的数量
        REFERS.incrementAndGet();
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
            logger.info("destroy consumer group with bean name : {}", config.getId());
            config.unrefer();
        }
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        //刷新事件会多次，防止重入
        switcher.open(() -> {
            try {
                latch.await();
                if (referThrowable != null) {
                    logger.error(String.format("Error occurs while referring consumer bean %s", config.getId()), referThrowable);
                    System.exit(1);
                } else if (REFERS.decrementAndGet() == 0) {
                    applicationEventPublisher.publishEvent(new ConsumerReferDoneEvent(true));
                }
            } catch (InterruptedException e) {
            }
        });
    }
}
