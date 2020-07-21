package io.joyrpc.codec.serialization.jackson;

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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.joyrpc.exception.SerializerException;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * 序列化
 */
public abstract class AbstractDeserializer<T> extends JsonDeserializer<T> {

    /**
     * 读取字符串
     *
     * @param parser   解析器
     * @param field    字段
     * @param nullable 是否可以null
     */
    protected String readString(final JsonParser parser, String field, boolean nullable) throws IOException {
        switch (parser.nextToken()) {
            case VALUE_STRING:
                return parser.getText();
            case VALUE_NULL:
                if (!nullable) {
                    throw new SerializerException("syntax error:" + field + " can not be null");
                }
                return null;
            default:
                throw new SerializerException("syntax error:" + field + " can not be null");
        }
    }

    /**
     * 读取字符串
     *
     * @param parser   解析器
     * @param field    字段
     * @param nullable 是否可以null
     * @param consumer 值消费者
     */
    protected void readString(final JsonParser parser, String field, boolean nullable, Consumer<String> consumer) throws IOException {
        consumer.accept(readString(parser, field, nullable));
    }
}
