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

import java.io.Serializable;
import java.lang.reflect.Method;
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
     * 获取参数
     *
     * @return 参数
     */
    Object[] getArgs();

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
     * 获取方法名称
     *
     * @return 方法名称
     */
    String getMethodName();

    /**
     * 获取分组
     *
     * @return 分组
     */
    String getAlias();

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
     * 判断是否是泛型
     *
     * @return 泛型标识
     */
    boolean isGeneric();
}
