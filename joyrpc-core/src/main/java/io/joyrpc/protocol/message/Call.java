package io.joyrpc.protocol.message;

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

import io.joyrpc.exception.MethodOverloadException;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * 调用接口
 */
public interface Call extends Serializable {
    /**
     * 获取参数类名，返回的数组不能修改
     *
     * @return 参数类型
     */
    String[] getArgsType();

    /**
     * 设置参数类型
     *
     * @param argsType 参数类型
     */
    void setArgsType(String[] argsType);

    /**
     * 获取参数
     *
     * @return 参数
     */
    Object[] getArgs();

    /**
     * 设置参数
     *
     * @param args 参数
     */
    void setArgs(Object[] args);

    /**
     * 获取参数类型，返回的数组不能修改
     *
     * @return 参数类型
     */
    Class[] getArgClasses();

    /**
     * 获取接口类名
     *
     * @return 接口类名
     */
    String getClassName();

    /**
     * 设置参数类型
     *
     * @param className
     */
    void setClassName(String className);

    /**
     * 获取方法名称
     *
     * @return 方法名称
     */
    String getMethodName();

    /**
     * 设置方法名称
     *
     * @param methodName 方法名称
     */
    void setMethodName(String methodName);

    /**
     * 获取分组
     *
     * @return 分组
     */
    String getAlias();

    /**
     * 设置分组
     *
     * @param alias 分组
     */
    void setAlias(String alias);

    /**
     * 获取方法
     *
     * @return 方法
     */
    Method getMethod();

    /**
     * 获取接口类
     *
     * @return 接口类
     */
    Class getClazz();

    /**
     * 获取扩展对象
     *
     * @return 扩展对象
     */
    Object getObject();

    /**
     * 获取扩展属性
     *
     * @return
     */
    Map<String, Object> getAttachments();

    /**
     * 添加扩展属性
     *
     * @param map 参数
     */
    void addAttachments(final Map<String, ?> map);

    /**
     * 判断是否是泛型
     *
     * @return 泛型标识
     */
    boolean isGeneric();

    /**
     * 计算真实的参数类型
     *
     * @return 参数泛型数组
     */
    Type[] computeTypes() throws NoSuchMethodException, MethodOverloadException, ClassNotFoundException;

    /**
     * 计算参数类型名称
     *
     * @return 参数类型名称数组
     */
    String[] computeArgsType();

    /**
     * 是否是回调
     *
     * @return 回调标识
     */
    boolean isCallback();

}
