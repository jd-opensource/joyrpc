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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 回调方法
 */
public class CallbackMethod {

    /**
     * 所属方法
     */
    protected Method method;
    /**
     * 索引
     */
    protected int index;
    /**
     * 参数索引
     */
    protected Parameter parameter;
    /**
     * 参数真实类型
     */
    protected Class<?> parameterType;
    /**
     * 返回值真实类型
     */
    protected Class<?> returnType;

    /**
     * 回调方法
     *
     * @param method    方法
     * @param index     参数索引
     * @param parameter 参数
     */
    public CallbackMethod(Method method, int index, Parameter parameter) {
        this.method = method;
        this.index = index;
        this.parameter = parameter;
        Type type = parameter.getParameterizedType();
        if (type instanceof ParameterizedType) {
            Type[] actualTypes = ((ParameterizedType) type).getActualTypeArguments();
            if (actualTypes.length == 2) {
                parameterType = getRealClass(actualTypes[0]);
                returnType = getRealClass(actualTypes[1]);
            }
        }
    }

    /**
     * 获取真实类型
     *
     * @param actualType 类型
     * @return 类
     */
    protected Class<?> getRealClass(final Type actualType) {
        if (actualType instanceof ParameterizedType) {
            // 例如 Callback<List<String>>
            Type rawType = ((ParameterizedType) actualType).getRawType();
            if (rawType instanceof Class) {
                return (Class<?>) rawType;
            }
        } else if (actualType instanceof Class) {
            // 普通的 Callback<String>
            return (Class<?>) actualType;
        }
        return null;
    }

    public int getIndex() {
        return index;
    }

    public Parameter getParameter() {
        return parameter;
    }

    public Class<?> getParameterType() {
        return parameterType;
    }

    public Class<?> getReturnType() {
        return returnType;
    }
}
