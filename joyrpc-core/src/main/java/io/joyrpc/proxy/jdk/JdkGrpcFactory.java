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
import io.joyrpc.proxy.MethodArgs;
import io.joyrpc.util.ClassUtils;
import io.joyrpc.util.GrpcType;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * JDK的GrpcType工厂
 */
@Extension(value = "jdk", order = GrpcFactory.ORDER_JDK)
@ConditionalOnClass("javax.tools.ToolProvider")
public class JdkGrpcFactory extends AbstractGrpcFactory implements Serializable {

    protected JdkCompiler compiler = new JdkCompiler();

    @Override
    protected Class<?> buildResponseClass(final Class<?> clz, final Method method, final Naming naming) throws Exception {


        String simpleName = naming.getSimpleName();
        String fullName = naming.getFullName();
        String typeName = method.getGenericReturnType().getTypeName();
        String field = GrpcType.F_RESULT;
        String upperField = field.substring(0, 1).toUpperCase() + field.substring(1);
        StringBuilder builder = new StringBuilder(200).
                append("package ").append(clz.getPackage().getName()).append(";\n").
                append("public class ").append(simpleName).append(" implements java.io.Serializable," + MethodArgs.class.getName() + "{\n").
                append("\t").append("private ").append(typeName).append(' ').append(field).append(";\n").
                append("\t").append("public ").append(typeName).append(" get").append(upperField).append("(){\n").
                append("\t\t").append("return ").append(field).append(";").append("\n").
                append("\t}\n").
                append("\t").append("public void set").append(upperField).append("(").append(typeName).append(' ').append(field).append("){\n").
                append("\t\t").append("this.").append(field).append('=').append(field).append(";\n").
                append("\t}\n").
                append("\t").append("public Object[] toArgs(){\n").
                append("\t\t").append("return new Object[]{").append(field).append("};\n").
                append("\t}\n").
                append("\t").append("public void toFields(Object[] args){\n").append("\t\t").append(field).append("=(").append(typeName).append(")args[0];\n").
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
    protected Class<?> buildRequestClass(final Class<?> clz, final Method method, final Naming naming) throws Exception {
        String simpleName = naming.getSimpleName();
        String fullName = naming.getFullName();
        StringBuilder builder = new StringBuilder(1024).
                append("package ").append(clz.getPackage().getName()).append(";\n").
                append("public class ").append(simpleName).append(" implements java.io.Serializable," + MethodArgs.class.getName() + "{\n");
        //添加字段
        Parameter[] parameters = method.getParameters();
        String[] typeNames = new String[parameters.length];
        int i = 0;
        for (Parameter parameter : parameters) {
            typeNames[i] = parameter.getParameterizedType().getTypeName();
            builder.append("\t").append("private ").append(typeNames[i]).append(' ').append(parameter.getName()).append(";\n");
            i++;
        }
        //添加Getter&Setter
        String name;
        String upperName;
        StringBuilder toFields = new StringBuilder(200);
        StringBuilder toArgs = new StringBuilder(100);
        i = 0;
        for (Parameter parameter : parameters) {
            name = parameter.getName();
            upperName = name.substring(0, 1).toUpperCase() + name.substring(1);
            builder.append("\t").append("public ").append(typeNames[i]).append(" get").append(upperName).append("(){\n").
                    append("\t\t").append("return ").append(name).append(";\n").
                    append("\t}\n").
                    append("\t").append("public void set").append(upperName).append("(").append(typeNames[i]).append(" ").append(name).append("){\n").
                    append("\t\t").append("this.").append(name).append("=").append(name).append(";\n").
                    append("\t}\n");
            if (i > 0) {
                toArgs.append(',');
            }
            toArgs.append(parameter.getName());
            toFields.append("\t\t").append(name).append("=(").append(typeNames[i]).append(")args[").append(i).append("];\n");
            i++;
        }
        //添加toArgs方法
        builder.append("\t").append("public Object[] toArgs(){\n").
                append("\t\t").append("return new Object[]{").
                append(toArgs.toString()).
                append("};\n\t}\n");
        //添加toFields方法
        builder.append("\t").append("public void toFields(Object[] args){\n").append(toFields.toString()).append("\t};\n");
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
