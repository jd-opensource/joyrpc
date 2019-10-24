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


import org.springframework.core.env.Environment;


class AnnotationBeanNameBuilder {

    /**
     * 接口名称
     */
    protected String interfaceClassName;

    /**
     * 环境
     */
    protected Environment environment;

    /**
     * 分组
     */
    protected String alias;

    /**
     * 名称
     */
    protected String name;

    public static AnnotationBeanNameBuilder builder() {
        return new AnnotationBeanNameBuilder();
    }

    public AnnotationBeanNameBuilder alias(String alias) {
        this.alias = alias;
        return this;
    }

    public AnnotationBeanNameBuilder interfaceClassName(String interfaceClassName) {
        this.interfaceClassName = interfaceClassName;
        return this;
    }

    public AnnotationBeanNameBuilder environment(Environment environment) {
        this.environment = environment;
        return this;
    }

    public AnnotationBeanNameBuilder name(String name) {
        this.name = name;
        return this;
    }

    public String build() {
        String result = name;
        if (result == null || result.isEmpty()) {
            StringBuilder builder = new StringBuilder("AnnotationBean");
            if (interfaceClassName != null && !interfaceClassName.isEmpty()) {
                builder.append(interfaceClassName);
            }
            if (alias != null && !alias.isEmpty()) {
                builder.append(alias);
            }
            result = builder.toString();
        }
        return environment.resolvePlaceholders(result);
    }
}
