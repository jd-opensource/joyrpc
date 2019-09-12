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
import io.joyrpc.extension.Extensible;

import java.io.*;
import java.lang.reflect.Type;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * JSON API
 */
@Extensible("json")
public interface Json {

    /**
     * 序列化
     *
     * @param os     输出流
     * @param object 对象
     * @return json字符串
     */
    void writeJSONString(OutputStream os, Object object) throws SerializerException;

    /**
     * 对象转为json字符串
     *
     * @param object 对象
     * @return json字符串
     */
    String toJSONString(Object object) throws SerializerException;

    /**
     * 对象转为json字节数组
     *
     * @param object 对象
     * @return json字符串
     */
    byte[] toJSONBytes(Object object) throws SerializerException;

    /**
     * 解析为指定对象
     *
     * @param text json字符串
     * @param type 指定类
     * @param <T>  指定对象
     * @return 指定对象
     */
    <T> T parseObject(String text, Type type) throws SerializerException;

    /**
     * 解析为指定对象
     *
     * @param text      json字符串
     * @param reference 指定类
     * @param <T>       指定对象
     * @return 指定对象
     */
    <T> T parseObject(String text, TypeReference<T> reference) throws SerializerException;

    /**
     * 解析为指定对象
     *
     * @param inputStream 输入流
     * @param type
     * @param <T>         指定对象
     * @return 指定对象
     */
    <T> T parseObject(InputStream inputStream, Type type) throws SerializerException;

    /**
     * 解析为指定对象
     *
     * @param inputStream 输入流
     * @param reference
     * @param <T>         指定对象
     * @return 指定对象
     */
    <T> T parseObject(InputStream inputStream, TypeReference<T> reference) throws SerializerException;


    /**
     * 流式解析数组
     *
     * @param text
     * @param function
     */
    default void parseArray(final String text, final Function<Function<Type, Object>, Boolean> function) throws SerializerException {
        if (text == null || function == null) {
            return;
        }
        parseArray(new StringReader(text), function);
    }

    /**
     * 流式解析数组
     *
     * @param is
     * @param function
     */
    default void parseArray(final InputStream is, final Function<Function<Type, Object>, Boolean> function) throws SerializerException {
        if (is == null || function == null) {
            return;
        }
        parseArray(new InputStreamReader(is), function);
    }

    /**
     * 流式解析数组
     *
     * @param reader
     * @param function
     */
    void parseArray(Reader reader, Function<Function<Type, Object>, Boolean> function) throws SerializerException;

    /**
     * 流式解析对象
     *
     * @param text
     * @param function
     */
    default void parseObject(final String text, final BiFunction<String, Function<Type, Object>, Boolean> function) throws SerializerException {
        if (text == null || function == null) {
            return;
        }
        parseObject(new StringReader(text), function);
    }

    /**
     * 流式解析对象
     *
     * @param is
     * @param function
     */
    default void parseObject(final InputStream is, final BiFunction<String, Function<Type, Object>, Boolean> function) throws SerializerException {
        if (is == null || function == null) {
            return;
        }
        parseObject(new InputStreamReader(is), function);
    }

    /**
     * 流式解析对象
     *
     * @param reader
     * @param function
     */
    void parseObject(Reader reader, BiFunction<String, Function<Type, Object>, Boolean> function) throws SerializerException;

}
