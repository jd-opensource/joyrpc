package io.joyrpc.spring.boot;

import io.joyrpc.extension.Extensible;
import io.joyrpc.spring.annotation.Consumer;
import io.joyrpc.spring.annotation.Provider;

import java.lang.annotation.Annotation;

/**
 * 注解提供者，便于Springboot扫描生成第三方注解生成
 */
@Extensible("AnnotationProvider")
public interface AnnotationProvider {

    /**
     * 服务提供者的注解
     *
     * @return
     */
    Annotation getProvider();

    /**
     * 消费者的注解
     *
     * @return
     */
    Annotation getConsumer();

    /**
     * 服务提供者注解转换
     *
     * @param annotation
     * @return
     */
    Provider toProvider(Annotation annotation);

    /**
     * 消费者注解转换
     *
     * @param annotation
     * @return
     */
    Consumer toConsumer(Annotation annotation);

}
