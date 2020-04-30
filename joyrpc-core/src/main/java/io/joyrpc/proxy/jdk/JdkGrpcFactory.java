package io.joyrpc.proxy.jdk;

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

import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.proxy.AbstractGrpcFactory;
import io.joyrpc.proxy.GrpcFactory;
import io.joyrpc.util.ClassUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.function.Supplier;

/**
 * JDK的GrpcType工厂
 */
@Extension(value = "jdk", order = GrpcFactory.ORDER_JDK)
@ConditionalOnClass("javax.tools.ToolProvider")
public class JdkGrpcFactory extends AbstractGrpcFactory implements Serializable {

    protected JdkCompiler compiler = new JdkCompiler();

    @Override
    protected Class<?> buildResponseClass(Class<?> clz, Method method, Supplier<String> suffix) throws Exception {
        String name = suffix.get();
        String simpleName = clz.getSimpleName() + "$" + name;
        String fullName = clz.getName() + "$" + name;
        String typeName = method.getGenericReturnType().getTypeName();
        StringBuilder builder = new StringBuilder(200).
                append("package ").append(clz.getPackage().getName()).append(";\n").
                append("public class ").append(simpleName).append(" implements java.io.Serializable{\n").
                append("\t").append("private ").append(typeName).append(" result;\n").
                append("\t").append("public ").append(typeName).append(" getResult(){\n").
                append("\t\t").append("return result;").append("\n").
                append("\t}\n").
                append("\t").append("public void setResult(").append(typeName).append(" result){\n").
                append("\t\t").append("this.result=result;").append("\n").
                append("\t}\n").
                append('}');
        return ClassUtils.forName(fullName, (n) -> {
            try {
                return compiler.compile(n, builder);
            } catch (Throwable e) {
                throw new RuntimeException(e.getMessage() + " java source:\n" + builder.toString());
            }
        });
    }

    @Override
    protected Class<?> buildRequestClass(Class<?> clz, Method method, Supplier<String> suffix) throws Exception {
        String name = suffix.get();
        String simpleName = clz.getSimpleName() + "$" + name;
        String fullName = clz.getName() + "$" + name;
        StringBuilder builder = new StringBuilder(200).
                append("package ").append(clz.getPackage().getName()).append(";\n").
                append("public class ").append(simpleName).append(" implements java.io.Serializable,io.joyrpc.proxy.MethodArgs{\n");
        //添加字段
        for (Parameter parameter : method.getParameters()) {
            builder.append("\t").append("private ").append(parameter.getParameterizedType().getTypeName()).append(' ').append(parameter.getName()).append(";\n");
        }
        //添加Getter&Setter
        Type type;
        String upperName;
        String typeName;
        for (Parameter parameter : method.getParameters()) {
            type = parameter.getParameterizedType();
            name = parameter.getName();
            upperName = name.substring(0, 1).toUpperCase() + name.substring(1);
            typeName = type.getTypeName();
            builder.append("\t").append("public ").append(typeName).append(" get").append(upperName).append("(){\n").
                    append("\t\t").append("return ").append(name).append(";\n").
                    append("\t}\n").
                    append("\t").append("public void set").append(upperName).append("(").append(typeName).append(" ").append(name).append("){\n").
                    append("\t\t").append("this.").append(name).append("=").append(name).append(";\n").
                    append("\t}\n");
        }
        //添加toArgs方法
        builder.append("\t").append("public Object[] toArgs(){\n").
                append("\t\t").append("return new Object[]{");
        int i = 0;
        for (Parameter parameter : method.getParameters()) {
            if (i++ > 0) {
                builder.append(',');
            }
            builder.append(parameter.getName());
        }
        builder.append("};\n").append("\t}\n");
        builder.append('}');
        return ClassUtils.forName(fullName, (n) -> {
            try {
                return compiler.compile(n, builder);
            } catch (Throwable e) {
                throw new RuntimeException(e.getMessage() + " java source:\n" + builder.toString());
            }
        });
    }
}
