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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.joyrpc.exception.MethodOverloadException;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.protocol.message.Invocation;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Consumer;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static io.joyrpc.protocol.message.Invocation.*;

/**
 * Invocation反序列化
 */
public class InvocationDeserializer extends JsonDeserializer<Invocation> {

    public static final JsonDeserializer INSTANCE = new InvocationDeserializer();

    @Override
    public Invocation deserialize(final JsonParser parser, final DeserializationContext ctx) throws IOException {
        switch (parser.currentToken()) {
            case VALUE_NULL:
                return null;
            case START_OBJECT:
                return parse(parser);
            default:
                throw new SerializerException("Error occurs while parsing invocation");
        }

    }

    /**
     * 解析
     *
     * @param parser 解析器
     * @return 调用对象
     * @throws IOException
     */
    protected Invocation parse(final JsonParser parser) throws IOException {
        Invocation invocation = new Invocation();
        String key;
        try {
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                key = parser.currentName();
                if (CLASS_NAME.equals(key)) {
                    readString(parser, CLASS_NAME, false, v -> invocation.setClassName(v));
                } else if (ALIAS.equals(key)) {
                    readString(parser, ALIAS, true, v -> invocation.setAlias(v));
                } else if (METHOD_NAME.equals(key)) {
                    readString(parser, METHOD_NAME, true, v -> invocation.setMethodName(v));
                } else if (ARGS_TYPE.equals(key)) {
                    invocation.setArgsType(readArgTypes(parser));
                } else if (ARGS.equals(key)) {
                    invocation.setArgs(parseArgs(parser, invocation.computeTypes()));
                } else if (ATTACHMENTS.equals(key)) {
                    invocation.addAttachments(readAttachments(parser));
                }
            }
        } catch (ClassNotFoundException | NoSuchMethodException | MethodOverloadException e) {
            throw new SerializerException(e.getMessage(), e);
        }
        return invocation;
    }

    /**
     * 读取字符串数组
     *
     * @param parser 解析器
     */
    protected String[] readArgTypes(final JsonParser parser) throws IOException {
        switch (parser.nextToken()) {
            case START_ARRAY:
                return parser.readValueAs(String[].class);
            case VALUE_NULL:
                return null;
            default:
                throw new SerializerException("Error occurs while parsing invocation");
        }
    }

    /**
     * 读取扩展属性
     *
     * @param parser 解析器
     */
    protected Map<String, Object> readAttachments(final JsonParser parser) throws IOException {
        switch (parser.nextToken()) {
            case START_OBJECT:
                return parser.readValueAs(new TypeReference<Map<String, Object>>() {
                });
            case VALUE_NULL:
                return null;
            default:
                throw new SerializerException("Error occurs while parsing invocation");
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
        switch (parser.nextToken()) {
            case VALUE_STRING:
                consumer.accept(parser.getText());
                break;
            case VALUE_NULL:
                if (!nullable) {
                    throw new SerializerException("syntax error:" + field + " can not be null");
                }
                break;
            default:
                throw new SerializerException("syntax error:" + field + " can not be null");
        }
    }

    /**
     * 解析参数
     *
     * @param parser 解析器
     * @param types  类型
     */
    protected Object[] parseArgs(final JsonParser parser, final Type[] types) throws IOException {
        switch (parser.nextToken()) {
            case START_ARRAY:
                //解析参数
                Object[] objects = new Object[types.length];
                for (int i = 0; i < objects.length; i++) {
                    parser.nextToken();
                    objects[i] = parser.readValueAs(new SimpleTypeReference(types[i]));
                }
                if (parser.nextToken() != END_ARRAY) {
                    throw new SerializerException("The argument size must be " + types.length);
                }
                return objects;
            case VALUE_NULL:
                if (types.length == 0) {
                    return new Object[0];
                } else {
                    throw new SerializerException("syntax error: args can not be null");
                }
            default:
                throw new SerializerException("Error occurs while parsing invocation");
        }
    }
}
