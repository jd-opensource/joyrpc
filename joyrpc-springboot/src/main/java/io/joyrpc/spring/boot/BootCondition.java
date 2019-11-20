package io.joyrpc.spring.boot;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class BootCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Class bootBinderClass = null;
        try {
            bootBinderClass = Class.forName("org.springframework.boot.context.properties.bind.Binder");
        } catch (ClassNotFoundException e) {

        }
        return bootBinderClass != null;
    }
}
