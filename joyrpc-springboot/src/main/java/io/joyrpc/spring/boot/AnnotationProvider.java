package io.joyrpc.spring.boot;

import io.joyrpc.config.ConsumerConfig;
import io.joyrpc.config.ProviderConfig;
import io.joyrpc.extension.Extensible;

import java.lang.annotation.Annotation;

/**
 * 注解提供者，便于Springboot扫描生成第三方注解生成
 */
@Extensible("AnnotationProvider")
public interface AnnotationProvider {

    /**
     * 服务提供者的注解类
     *
     * @return
     */
    Class<Annotation> getProviderAnnotationClass();

    /**
     * 消费者的注解类
     *
     * @return
     */
    Class<Annotation> getConsumerAnnotationClass();

    /**
     * 服务提供者注解转换成配置
     *
     * @param annotation
     * @return
     */
    ProviderConfig toProviderConfig(Annotation annotation);

    /**
     * 消费者注解转换成配置
     *
     * @param annotation
     * @return
     */
    ConsumerConfig toConsumerConfig(Annotation annotation);

}
