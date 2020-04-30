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
import java.util.function.Supplier;

import static io.joyrpc.proxy.GrpcFactory.ORDER_JAVASSIST;

@Extension(value = "javassist", order = ORDER_JAVASSIST)
@ConditionalOnClass("javassist.ClassPool")
public class JavassistGrpcFactory extends AbstractGrpcFactory {

    @Override
    protected Class<?> buildRequestClass(final Class<?> clz, final Method method, final Supplier<String> suffix) throws Exception {
        //ClassPool：CtClass对象的容器
        ClassPool pool = ClassPool.getDefault();
        //通过ClassPool生成一个public新类
        CtClass ctClass = pool.makeClass(clz.getName() + "$" + suffix.get());
        ctClass.setInterfaces(new CtClass[]{pool.getCtClass(Serializable.class.getName()), pool.getCtClass(MethodArgs.class.getName())});
        //添加字段
        CtField ctField;
        String name;
        Type type;
        int i = 0;
        StringBuilder toArgs = new StringBuilder(50).append("return new Object[]{");
        for (Parameter parameter : method.getParameters()) {
            name = parameter.getName();
            type = parameter.getParameterizedType();
            ctField = new CtField(pool.getCtClass(type.getTypeName()), name, ctClass);
            ctField.setModifiers(Modifier.PRIVATE);
            ctClass.addField(ctField);
            if (i++ > 0) {
                toArgs.append(',');
            }
            toArgs.append(name);
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            ctClass.addMethod(CtNewMethod.getter((boolean.class == type ? "is" : "get") + name, ctField));
            ctClass.addMethod(CtNewMethod.setter("set" + name, ctField));
        }
        toArgs.append("};");
        ctClass.addMethod(CtNewMethod.make(pool.getCtClass("java.lang.Object[]"),
                "toArgs", new CtClass[0], new CtClass[0], toArgs.toString(), ctClass));
        return ctClass.toClass();
    }

    @Override
    protected Class<?> buildResponseClass(final Class<?> clz, final Method method, final Supplier<String> naming) throws Exception {
        //ClassPool：CtClass对象的容器
        ClassPool pool = ClassPool.getDefault();
        //通过ClassPool生成一个public新类
        CtClass ctClass = pool.makeClass(clz.getName() + "$" + naming.get());
        Type type = method.getGenericReturnType();
        String name = GrpcType.F_RESULT;
        CtField ctField = new CtField(pool.getCtClass(type.getTypeName()), name, ctClass);
        ctField.setModifiers(Modifier.PRIVATE);
        ctClass.addField(ctField);
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        ctClass.addMethod(CtNewMethod.getter((boolean.class == type ? "is" : "get") + name, ctField));
        ctClass.addMethod(CtNewMethod.setter("set" + name, ctField));
        return ctClass.toClass();
    }

}
