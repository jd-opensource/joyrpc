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
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.util.ClassUtils;

import java.io.IOException;
import java.lang.reflect.Type;

import static io.joyrpc.protocol.message.Invocation.CLASS_NAME;
import static io.joyrpc.protocol.message.ResponsePayload.EXCEPTION;
import static io.joyrpc.protocol.message.ResponsePayload.RESPONSE;
import static io.joyrpc.util.GenericMethod.getReturnGenericType;

/**
 * ResponsePayload反序列化
 */
public class ResponsePayloadDeserializer extends AbstractDeserializer<ResponsePayload> {

    public static final JsonDeserializer INSTANCE = new ResponsePayloadDeserializer();

    @Override
    public ResponsePayload deserialize(final JsonParser parser, final DeserializationContext ctx) throws IOException {
        switch (parser.currentToken()) {
            case VALUE_NULL:
                return null;
            case START_OBJECT:
                return parse(parser);
            default:
                throw new SerializerException("Error occurs while parsing responsePayload");
        }

    }

    /**
     * 解析
     *
     * @param parser 解析器
     * @return 应答
     * @throws IOException
     */
    protected ResponsePayload parse(final JsonParser parser) throws IOException {
        ResponsePayload payload = new ResponsePayload();
        String key;
        String typeName = null;
        try {
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                key = parser.currentName();
                if (CLASS_NAME.equals(key)) {
                    typeName = readString(parser, CLASS_NAME, false);
                } else if (RESPONSE.equals(key)) {
                    payload.setResponse(readResponse(parser, typeName));
                } else if (EXCEPTION.equals(key)) {
                    payload.setException(readException(parser, typeName));
                }
            }
        } catch (ClassNotFoundException e) {
            throw new SerializerException(e.getMessage(), e);
        }
        return payload;
    }

    /**
     * 读取应答
     *
     * @param parser   解析器
     * @param typeName 类型
     * @throws IOException
     * @throws ClassNotFoundException
     */
    protected Object readResponse(final JsonParser parser, final String typeName) throws IOException, ClassNotFoundException {
        if (typeName == null || typeName.isEmpty()) {
            return null;
        }
        switch (parser.nextToken()) {
            case START_OBJECT:
                return parser.readValueAs(new SimpleTypeReference(getType(typeName)));
            case VALUE_NULL:
                return null;
            default:
                throw new SerializerException("Error occurs while parsing responsePayload");
        }
    }

    /**
     * 读取扩展属性
     *
     * @param parser   解析器
     * @param typeName 类型
     * @throws IOException
     */
    protected Throwable readException(final JsonParser parser, final String typeName) throws IOException, ClassNotFoundException {
        if (typeName == null || typeName.isEmpty()) {
            return null;
        }
        switch (parser.nextToken()) {
            case START_OBJECT:
                return (Throwable) parser.readValueAs(getThrowableType(typeName));
            case VALUE_NULL:
                return null;
            default:
                throw new SerializerException("Error occurs while parsing responsePayload");
        }
    }

    /**
     * 获取异常类型
     *
     * @param typeName 类型名称
     * @return 异常类型
     * @throws ClassNotFoundException
     */
    protected Class<?> getThrowableType(final String typeName) throws ClassNotFoundException {
        Class<?> clazz = ClassUtils.getClass(typeName);
        if (clazz == null) {
            return Throwable.class;
        } else if (Throwable.class.isAssignableFrom(clazz)) {
            return clazz;
        } else {
            throw new SerializerException("syntax error: invalid throwable class " + typeName);
        }
    }

    /**
     * 根据类型名称获取类型
     *
     * @param typeName 类型名称
     * @return 类型
     * @throws ClassNotFoundException
     */
    protected Type getType(final String typeName) throws ClassNotFoundException {
        Type type = getReturnGenericType(typeName);
        type = type == null ? ClassUtils.getClass(typeName) : type;
        return type;
    }

}
