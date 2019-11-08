package io.joyrpc.spring.boot;

import io.joyrpc.extension.ExtensionPoint;
import io.joyrpc.extension.ExtensionPointLazy;
import io.joyrpc.spring.boot.annotation.AnnotationProvider;

/**
 * 扩展点声明
 */
public interface Plugin {

    /**
     * 注解提供者
     */
    ExtensionPoint<AnnotationProvider, String> ANNOTATION_PROVIDER = new ExtensionPointLazy<>(AnnotationProvider.class);
}
