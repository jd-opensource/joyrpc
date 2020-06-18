package io.joyrpc.invoker;

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

import io.joyrpc.util.ClassUtils;
import io.joyrpc.util.GenericClass;
import io.joyrpc.util.GenericMethod;
import io.joyrpc.util.GenericType;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 回调方法
 */
public class CallbackMethod {

    protected static final AtomicLong counter = new AtomicLong(0);

    /**
     * 所属接口
     */
    protected Class<?> interfaceClass;
    /**
     * 泛化信息
     */
    protected GenericClass genericClass;
    /**
     * 所属方法
     */
    protected Method method;
    /**
     * 参数索引
     */
    protected int index;
    /**
     * 参数对象
     */
    protected Parameter parameter;
    /**
     * 方法的参数类型
     */
    protected Map<Method, Class[]> parameterTypes;

    /**
     * 回调方法
     *
     * @param interfaceClass 接口类
     * @param method         方法
     * @param index          参数索引
     * @param parameter      参数
     * @param genericClass   泛化信息
     */
    public CallbackMethod(final Class<?> interfaceClass,
                          final Method method,
                          final int index,
                          final Parameter parameter,
                          final GenericClass genericClass) {
        this.interfaceClass = interfaceClass;
        this.genericClass = genericClass;
        this.method = method;
        this.index = index;
        this.parameter = parameter;
        //构造回调方法的时候确保了参数只能是接口，为了兼容Callback，处理泛型
        Class<?> parameterType = parameter.getType();
        TypeVariable<? extends Class<?>>[] variables = parameterType.getTypeParameters();
        //类有泛型变量
        if (variables.length > 0) {
            //获取方法参数的泛型信息
            GenericMethod genericMethod = genericClass.get(method);
            GenericType[] genericTypes = genericMethod.getParameters();
            Map<String, Class<?>> variableTypes = new HashMap<>();
            Type[] types = ((ParameterizedType) parameter.getParameterizedType()).getActualTypeArguments();
            for (int i = 0; i < types.length; i++) {
                //计算泛型参数的类型
                compute(variables[i].getName(), types[i], variableTypes, genericTypes[index]);
            }
            //回调参数类的泛型信息
            GenericClass gc = new GenericClass(parameterType, variableTypes);
            parameterTypes = new HashMap<>();
            Method[] methods = parameterType.getMethods();
            for (Method m : methods) {
                //计算每个方法的泛型信息
                parameterTypes.put(m, computeParameterType(gc.get(m)));
            }
        }
    }

    /**
     * 计算类的泛型变量
     *
     * @param variable 变量名称
     * @param type     类型
     * @param types    变量类型
     * @param gType    泛型信息
     */
    protected void compute(String variable, Type type, Map<String, Class<?>> types, GenericType gType) {
        if (type instanceof ParameterizedType) {
            // 例如 Callback<List<String>>
            Type rawType = ((ParameterizedType) type).getRawType();
            if (rawType instanceof Class) {
                types.put(variable, (Class<?>) rawType);
            } else {
                types.put(variable, null);
            }
        } else if (type instanceof Class) {
            // 普通的 Callback<String>
            types.put(variable, (Class<?>) type);
        } else if (type instanceof TypeVariable) {
            compute(variable, gType.getVariable(variable).getGenericType(), types, gType);
        }
    }

    /**
     * 计算方法参数类
     *
     * @param gMethod
     * @return
     */
    protected Class<?>[] computeParameterType(final GenericMethod gMethod) {
        GenericType[] types = gMethod.getParameters();
        Class<?>[] result = new Class<?>[types.length];
        for (int i = 0; i < types.length; i++) {
            result[i] = computeParameterType(types[i].getType(), types[i]);
        }
        return result;
    }

    /**
     * 获取真实类型
     *
     * @param type  类型
     * @param gType 泛型信息
     * @return 类
     */
    protected Class<?> computeParameterType(final Type type, final GenericType gType) {
        if (type instanceof ParameterizedType) {
            // 例如 Callback<List<String>>
            Type rawType = ((ParameterizedType) type).getRawType();
            if (rawType instanceof Class) {
                return (Class<?>) rawType;
            }
        } else if (type instanceof Class) {
            // 普通的 Callback<String>
            return (Class<?>) type;
        } else if (type instanceof TypeVariable) {
            // 类上的泛型变量
            GenericType.Variable variable = gType.getVariable(((TypeVariable) type).getName());
            return variable == null ? null : computeParameterType(variable.getGenericType(), gType);
        } else if (type instanceof GenericArrayType) {
            // 泛型数组
            Class<?> result = computeParameterType(((GenericArrayType) type).getGenericComponentType(), gType);
            try {
                return result == null ? null : ClassUtils.getClass(result.getCanonicalName() + "[]");
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    public int getIndex() {
        return index;
    }

    public Parameter getParameter() {
        return parameter;
    }

    /**
     * 获取方法的参数类型，进行泛化处理，便于兼容原有的Callback接口
     *
     * @param method 方法
     * @return 参数类型
     */
    public Class<?>[] getParameterTypes(final Method method) {
        //考虑到有泛型情况，需要处理
        return parameterTypes == null ? null : parameterTypes.get(method);
    }
}
