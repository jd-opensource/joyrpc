package io.joyrpc.annotation;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

/**
 * 别名
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE, METHOD})
@Inherited
public @interface Alias {

    /**
     * 名称
     *
     * @return
     */
    String value();
}
