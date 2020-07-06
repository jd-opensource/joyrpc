package io.joyrpc.proxy.javassist;

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
import io.joyrpc.proxy.MethodArgs;
import io.joyrpc.util.GrpcType;
import javassist.*;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

import static io.joyrpc.proxy.GrpcFactory.ORDER_JAVASSIST;

@Extension(value = "javassist", order = ORDER_JAVASSIST)
@ConditionalOnClass("javassist.ClassPool")
public class JavassistGrpcFactory extends AbstractGrpcFactory {

    @Override
    protected Class<?> buildRequestClass(final Class<?> clz, final Method method, final Naming naming) throws Exception {
        //ClassPool：CtClass对象的容器
        ClassPool pool = ClassPool.getDefault();
        //通过ClassPool生成一个public新类
        CtClass ctClass = pool.makeClass(naming.getFullName());
        ctClass.setInterfaces(new CtClass[]{pool.getCtClass(Serializable.class.getName()), pool.getCtClass(MethodArgs.class.getName())});
        //添加字段
        CtField ctField;
        String name;
        String typeName;
        Type type;
        int i = 0;
        StringBuilder toArgs = new StringBuilder(100).append("public Object[] toArgs(){\n\treturn new Object[]{");
        StringBuilder toFields = new StringBuilder(200).append("public void toFields(Object[] args){\n");
        for (Parameter parameter : method.getParameters()) {
            name = parameter.getName();
            type = parameter.getParameterizedType();
            typeName = type.getTypeName();
            ctField = new CtField(pool.getCtClass(typeName), name, ctClass);
            ctField.setModifiers(Modifier.PRIVATE);
            ctClass.addField(ctField);
            if (i > 0) {
                toArgs.append(',');
            }
            toArgs.append(name);
            toFields.append('\t').append(name).append("=(").append(typeName).append(")args[").append(i).append("];\n");
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            ctClass.addMethod(CtNewMethod.getter((boolean.class == type ? "is" : "get") + name, ctField));
            ctClass.addMethod(CtNewMethod.setter("set" + name, ctField));
            i++;
        }
        toArgs.append("};\n};");
        toFields.append("};");
        ctClass.addMethod(CtMethod.make(toArgs.toString(), ctClass));
        ctClass.addMethod(CtMethod.make(toFields.toString(), ctClass));
        return ctClass.toClass();
    }

    @Override
    protected Class<?> buildResponseClass(final Class<?> clz, final Method method, final Naming naming) throws Exception {
        //ClassPool：CtClass对象的容器
        ClassPool pool = ClassPool.getDefault();
        //通过ClassPool生成一个public新类
        CtClass ctClass = pool.makeClass(naming.getFullName());
        ctClass.setInterfaces(new CtClass[]{pool.getCtClass(Serializable.class.getName()), pool.getCtClass(MethodArgs.class.getName())});
        Type type = method.getGenericReturnType();
        String typeName = type.getTypeName();
        String field = GrpcType.F_RESULT;
        String upperField = field.substring(0, 1).toUpperCase() + field.substring(1);
        CtField ctField = new CtField(pool.getCtClass(typeName), field, ctClass);
        ctField.setModifiers(Modifier.PRIVATE);
        ctClass.addField(ctField);
        ctClass.addMethod(CtNewMethod.getter((boolean.class == type ? "is" : "get") + upperField, ctField));
        ctClass.addMethod(CtNewMethod.setter("set" + upperField, ctField));
        ctClass.addMethod(CtMethod.make("public Object[] toArgs(){\n\treturn new Object[]{" + field + "};\n};", ctClass));
        ctClass.addMethod(CtMethod.make("public void toFields(Object[] args){\n\t" + field + "=(" + typeName + ")args[0];\n};", ctClass));
        return ctClass.toClass();
    }

}
