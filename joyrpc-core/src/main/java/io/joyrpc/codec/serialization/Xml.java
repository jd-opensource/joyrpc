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

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Xml
 */
public interface Xml {

    /**
     * 序列化
     *
     * @param target
     * @return
     * @throws Exception
     */
    default String marshall(final Object target) throws SerializerException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
        marshall(os, target);
        return new String(os.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * 序列化
     *
     * @param os
     * @param target
     * @throws Exception
     */
    void marshall(OutputStream os, Object target) throws SerializerException;

    /**
     * 序列化
     *
     * @param writer
     * @param target
     * @throws Exception
     */
    void marshall(Writer writer, Object target) throws SerializerException;

    /**
     * 反序列化
     *
     * @param reader
     * @param clazz
     * @return
     * @throws Exception
     */
    <T> T unmarshall(Reader reader, Class<T> clazz) throws SerializerException;

    /**
     * 反序列化
     *
     * @param is
     * @param clazz
     * @return
     * @throws Exception
     */
    <T> T unmarshall(InputStream is, Class<T> clazz) throws SerializerException;

    /**
     * 反序列化
     *
     * @param value
     * @param clazz
     * @return
     * @throws Exception
     */
    default <T> T unmarshall(final String value, final Class<T> clazz) throws SerializerException {
        return unmarshall(new StringReader(value), clazz);
    }

}
