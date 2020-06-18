package io.joyrpc.util;

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

import java.lang.reflect.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static io.joyrpc.util.ClassUtils.*;

/**
 * 泛型检测
 */
public class GenericChecker {

    /**
     * 非静态方法
     */
    public static final Predicate<Method> NONE_STATIC_METHOD = (method -> !Modifier.isStatic(method.getModifiers()));

    /**
     * 可序列化字段
     */
    public static final Predicate<Field> NONE_STATIC_FINAL_TRANSIENT_FIELD = (field -> {
        int modifiers = field.getModifiers();
        return !Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers) && !Modifier.isTransient(modifiers);
    });

    /**
     * 唯一
     */
    protected Set<Type> uniques = new HashSet<>();

    /**
     * 检测所有公共方法设计的类型
     *
     * @param clazz
     * @param predicate
     * @param consumer
     */
    public void checkMethods(final Class clazz, final Predicate<Method> predicate,
                             final BiConsumer<Class, Scope> consumer) {
        if (clazz == null || consumer == null) {
            return;
        }
        GenericClass genericClass = getGenericClass(clazz);
        List<Method> methods = getPublicMethod(clazz);
        GenericMethod genericMethod;
        for (Method method : methods) {
            //静态方法不验证
            if (predicate == null || predicate.test(method)) {
                genericMethod = genericClass.get(method);
                checkReturnType(genericMethod, consumer);
                checkParameterTypes(genericMethod, consumer);
                checkExceptionTypes(genericMethod, consumer);
            }
        }
    }

    /**
     * 检查字段
     *
     * @param genericClass
     * @param predicate
     * @param consumer
     */
    public void checkFields(final GenericClass genericClass, final Predicate<Field> predicate,
                            final BiConsumer<Class, Scope> consumer) {
        //检查字段
        List<Field> fields = getFields(genericClass.getClazz());
        for (Field field : fields) {
            if (predicate.test(field)) {
                checkType(genericClass.get(field), field.getGenericType(), Scope.FIELD, consumer);
            }
        }
    }

    /**
     * 检查返回值
     *
     * @param genericMethod
     * @param consumer
     */
    public void checkReturnType(final GenericMethod genericMethod, final BiConsumer<Class, Scope> consumer) {
        checkType(genericMethod.getReturnType(), genericMethod.getMethod().getGenericReturnType(), Scope.RETURN, consumer);
    }

    /**
     * 检查参数类型
     *
     * @param method
     * @param consumer
     */
    public void checkParameterTypes(final GenericMethod method, final BiConsumer<Class, Scope> consumer) {
        Type[] types = method.getMethod().getGenericParameterTypes();
        if (types != null) {
            GenericType[] genericTypes = method.getParameters();
            for (int i = 0; i < types.length; i++) {
                checkType(genericTypes[i], types[i], Scope.PARAMETER, consumer);
            }
        }
    }

    /**
     * 检查参数类型
     *
     * @param method
     * @param consumer
     */
    public void checkExceptionTypes(final GenericMethod method, final BiConsumer<Class, Scope> consumer) {
        Type[] types = method.getMethod().getGenericExceptionTypes();
        if (types != null) {
            GenericType[] genericTypes = method.getExceptions();
            for (int i = 0; i < types.length; i++) {
                checkType(genericTypes[i], types[i], Scope.EXCEPTION, consumer);
            }
        }
    }

    /**
     * 检查类型
     *
     * @param genericType
     * @param type
     * @param scope
     * @param consumer
     */
    public void checkType(final GenericType genericType, final Type type,
                          final Scope scope,
                          final BiConsumer<Class, Scope> consumer) {
        if (!uniques.add(type)) {
            //已经检查过
            return;
        } else if (type instanceof Class) {
            checkClass((Class) type, scope, consumer);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            //rawType
            Type rawType = parameterizedType.getRawType();
            checkType(genericType, rawType, scope, consumer);
            //actualTypeArguments
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments != null && actualTypeArguments.length > 0) {
                for (Type actualTypeArgument : actualTypeArguments) {
                    checkType(genericType, actualTypeArgument, scope, consumer);
                }
            }
        } else if (type instanceof TypeVariable) {
            //变量
            GenericType.Variable variable = genericType.getVariable(((TypeVariable) type).getName());
            checkType(genericType, variable.getGenericType(), scope, consumer);
        } else if (type instanceof GenericArrayType) {
            //泛型数组
            checkType(genericType, ((GenericArrayType) type).getGenericComponentType(), scope, consumer);
        } else if (type instanceof WildcardType) {

        }
    }

    /**
     * 检查类型
     *
     * @param clazz    类
     * @param scope    作用域
     * @param consumer 消费者
     */
    public void checkClass(final Class clazz, final Scope scope, final BiConsumer<Class, Scope> consumer) {
        //参数允许是Callback
        consumer.accept(clazz.isArray() ? clazz.getComponentType() : clazz, scope);
    }

    /**
     * 作用域
     */
    public enum Scope {
        /**
         * 参数
         */
        PARAMETER("parameter"),
        /**
         * 返回值
         */
        RETURN("return"),
        /**
         * 异常
         */
        EXCEPTION("exception"),
        /**
         * 字段
         */
        FIELD("field");

        String name;

        Scope(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
