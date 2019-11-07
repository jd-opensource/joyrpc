package io.joyrpc.spring.boot;

import io.joyrpc.annotation.Consumer;
import io.joyrpc.annotation.Provider;
import io.joyrpc.extension.Extensible;

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

    /**
     * 服务提供者实现
     */
    class ProviderAnnotation implements Annotation, Provider {
        /**
         * 名称
         */
        private String name;
        /**
         * 分组
         */
        private String alias;
        /**
         * 接口
         */
        private Class interfaceClass;

        /**
         * 构造函数
         *
         * @param name
         * @param alias
         * @param interfaceClass
         */
        public ProviderAnnotation(String name, String alias, Class interfaceClass) {
            this.name = name;
            this.alias = alias;
            this.interfaceClass = interfaceClass;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String alias() {
            return alias;
        }

        @Override
        public Class interfaceClass() {
            return interfaceClass;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Provider.class;
        }
    }

    /**
     * 消费者实现
     */
    class ConsumerAnnotation implements Annotation, Consumer {
        /**
         * 名称
         */
        private String name;
        /**
         * 分组
         */
        private String alias;

        /**
         * 构造函数
         *
         * @param name
         * @param alias
         */
        public ConsumerAnnotation(String name, String alias) {
            this.name = name;
            this.alias = alias;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String alias() {
            return alias;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Consumer.class;
        }
    }

}
