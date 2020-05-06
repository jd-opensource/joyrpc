package io.joyrpc.proxy;

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

import io.joyrpc.exception.ProxyException;
import io.joyrpc.util.GrpcType;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * Grpc工厂类，负责生成参数和返回值的类型
 */
@FunctionalInterface
public interface GrpcFactory {

    int ORDER_JDK = 100;

    int ORDER_JAVASSIST = 200;

    int ORDER_BYTE_BUDDY = 300;

    /**
     * 动态生成参数和返回值的包装类，便于支持grpc调用
     *
     * @param clz    类型
     * @param method 方法
     * @return
     * @throws ProxyException
     */
    default GrpcType generate(Class<?> clz, Method method) throws ProxyException {
        return generate(clz, method, null);
    }

    /**
     * 动态生成参数和返回值的包装类，便于支持grpc调用
     *
     * @param clz    类型
     * @param method 方法
     * @param suffix 包装类后缀提供者
     * @return
     * @throws ProxyException
     */
    GrpcType generate(Class<?> clz, Method method, Supplier<String> suffix) throws ProxyException;
}
