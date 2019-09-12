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

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

/**
 * 对象序列化
 */
public interface Serializer {


    /**
     * 序列化
     *
     * @param os
     * @param object
     * @param <T>
     * @throws SerializerException
     */
    <T> void serialize(OutputStream os, T object) throws SerializerException;

    /**
     * 反序列化
     *
     * @param is
     * @param type
     * @param <T>
     * @return
     * @throws SerializerException
     */
    <T> T deserialize(InputStream is, Type type) throws SerializerException;
}
