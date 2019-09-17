package io.joyrpc.spring.factory;

/*-
 * #%L
 * joyrpc
 * %%
 * Copyright (C) 2019 joyrpc.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import io.joyrpc.spring.annotation.Consumer;
import io.joyrpc.spring.annotation.Provider;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;


class AnnotationBeanNameBuilder {

    private static final String SEPARATOR = ":";

    private String interfaceClassName;

    private Environment environment;

    private String alias;

    private String name;

    private AnnotationBeanNameBuilder(String interfaceClassName, Environment environment) {
        this.interfaceClassName = interfaceClassName;
        this.environment = environment;
    }

    private AnnotationBeanNameBuilder(Provider service, Class<?> interfaceClass, Environment environment) {
        this(interfaceClass.getName(), environment);
        this.alias(service.alias());
        this.name = service.name();
    }

    private AnnotationBeanNameBuilder(Consumer reference, Class<?> interfaceClass, Environment environment) {
        this(interfaceClass.getName(), environment);
        this.alias(reference.alias());
    }

    public static AnnotationBeanNameBuilder create(Provider service, Class<?> interfaceClass, Environment environment) {
        return new AnnotationBeanNameBuilder(service, interfaceClass, environment);
    }

    public static AnnotationBeanNameBuilder create(Consumer reference, Class<?> interfaceClass, Environment environment) {
        return new AnnotationBeanNameBuilder(reference, interfaceClass, environment);
    }

    private static void append(StringBuilder builder, String value) {
        if (StringUtils.hasText(value)) {
            builder.append(SEPARATOR).append(value);
        }
    }

    public AnnotationBeanNameBuilder alias(String alias) {
        this.alias = alias;
        return this;
    }

    public String build() {
        String rawBeanName = name;
        if (StringUtils.isEmpty(rawBeanName)) {
            StringBuilder beanNameBuilder = new StringBuilder("AnnotationBean");
            append(beanNameBuilder, interfaceClassName);
            append(beanNameBuilder, alias);
            rawBeanName = beanNameBuilder.toString();
        }
        return environment.resolvePlaceholders(rawBeanName);
    }
}
