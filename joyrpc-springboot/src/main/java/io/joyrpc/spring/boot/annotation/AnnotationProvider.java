package io.joyrpc.spring.boot.annotation;

import io.joyrpc.extension.Extensible;
import io.joyrpc.spring.ConsumerBean;
import io.joyrpc.spring.ProviderBean;
import org.springframework.core.env.Environment;

import java.lang.annotation.Annotation;

/**
 * 注解提供者，便于Springboot扫描生成第三方注解生成
 */
@Extensible("AnnotationProvider")
public interface AnnotationProvider<P extends Annotation, C extends Annotation> {

    /**
     * 服务提供者的注解类
     *
     * @return
     */
    Class<P> getProviderAnnotationClass();

    /**
     * 消费者的注解类
     *
     * @return
     */
    Class<C> getConsumerAnnotationClass();

    /**
     * 服务提供者注解转换成配置
     *
     * @param provider
     * @param environment
     * @return
     */
    ProviderBean toProviderBean(P provider, Environment environment);

    /**
     * 消费者注解转换成配置
     *
     * @param consumer
     * @param environment
     * @return
     */
    ConsumerBean toConsumerBean(C consumer, Environment environment);

}
