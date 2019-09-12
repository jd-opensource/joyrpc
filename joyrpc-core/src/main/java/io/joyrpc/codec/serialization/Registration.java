package io.joyrpc.codec.serialization;

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

import io.joyrpc.exception.SerializerException;

import java.util.Collection;

/**
 * 类型感知
 */
public interface Registration {

    /**
     * 注册序列化类
     *
     * @param clazz
     * @throws Serialization
     */
    void register(Class clazz) throws SerializerException;

    /**
     * 注册序列化类
     *
     * @param clazzs
     * @throws SerializerException
     */
    default void register(final Collection<Class<?>> clazzs) throws SerializerException {
        if (clazzs != null) {
            for (Class clazz : clazzs) {
                register(clazz);
            }
        }
    }
}
