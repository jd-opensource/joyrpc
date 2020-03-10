package io.joyrpc.protocol.http;

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

import io.joyrpc.extension.Extensible;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * URL参数绑定
 */
@Extensible("URLBinding")
@FunctionalInterface
public interface URLBinding {

    /**
     * 把字符串转换成参数类型
     *
     * @param clazz     类型
     * @param method    方法
     * @param parameter 参数类型
     * @param index     参数索引
     * @param value     字符串
     * @return 目标值
     */
    Object convert(Class<?> clazz, Method method, Parameter parameter, int index, String value);
}
