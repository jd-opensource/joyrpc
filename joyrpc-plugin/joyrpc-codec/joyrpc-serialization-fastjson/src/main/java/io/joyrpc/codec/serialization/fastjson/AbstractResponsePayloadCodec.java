package io.joyrpc.codec.serialization.fastjson;

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

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.JSONLexer;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.AutowiredObjectDeserializer;
import com.alibaba.fastjson.serializer.AutowiredObjectSerializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.util.ClassUtils;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import static io.joyrpc.protocol.message.ResponsePayload.*;
import static io.joyrpc.util.ClassUtils.getCanonicalName;
import static io.joyrpc.util.GenericMethod.getReturnGenericType;

/**
 * 应答序列化
 */
public abstract class AbstractResponsePayloadCodec extends AbstractSerializer implements AutowiredObjectSerializer, AutowiredObjectDeserializer {

    @Override
    public int getFastMatchToken() {
        return JSONToken.LBRACE;
    }

    @Override
    public Set<Type> getAutowiredFor() {
        Set<Type> result = new HashSet<>(1);
        fillAutowiredFor(result);
        return result;
    }

    /**
     * 设置类型
     *
     * @param types 类型
     */
    protected void fillAutowiredFor(final Set<Type> types) {
        types.add(ResponsePayload.class);
    }

    @Override
    public <T> T deserialze(final DefaultJSONParser parser, final Type type, final Object fieldName) {
        JSONLexer lexer = parser.getLexer();
        switch (lexer.token()) {
            case JSONToken.NULL:
                lexer.nextToken();
                return null;
            case JSONToken.LBRACE:
                return (T) parse(parser, lexer);
            default:
                return null;
        }
    }

    /**
     * 解析应答
     *
     * @param parser 解析器
     * @param lexer  文法
     * @return 应答
     */
    protected ResponsePayload parse(final DefaultJSONParser parser, final JSONLexer lexer) {
        ResponsePayload payload = new ResponsePayload();
        String key;
        int token;
        try {
            String typeName = null;
            for (; ; ) {
                // lexer.scanSymbol
                key = lexer.scanSymbol(parser.getSymbolTable());
                if (key == null) {
                    token = lexer.token();
                    if (token == JSONToken.RBRACE) {
                        lexer.nextToken(JSONToken.COMMA);
                        break;
                    } else if (token == JSONToken.COMMA) {
                        if (lexer.isEnabled(Feature.AllowArbitraryCommas)) {
                            continue;
                        }
                    }
                }
                lexer.nextTokenWithColon(JSONToken.LITERAL_STRING);
                if (RES_CLASS.equals(key)) {
                    typeName = parseString(lexer, RES_CLASS, false);
                } else if (RESPONSE.equals(key)) {
                    payload.setResponse(parseObject(parser, lexer, getType(typeName)));
                } else if (EXCEPTION.equals(key)) {
                    payload.setException((Throwable) parseObject(parser, lexer, getThrowableType(typeName)));
                }
                if (lexer.token() == JSONToken.RBRACE) {
                    lexer.nextToken(JSONToken.COMMA);
                    break;
                }
            }
            return payload;
        } catch (ClassNotFoundException e) {
            throw new SerializerException(e.getMessage());
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
        if (Throwable.class.isAssignableFrom(clazz)) {
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

    @Override
    public void write(final JSONSerializer serializer, final Object object, final Object fieldName,
                      final Type fieldType, final int features) {
        if (object == null) {
            serializer.writeNull();
        } else {
            writeObjectBegin(serializer);
            ResponsePayload payload = (object instanceof ResponseMessage ? ((ResponseMessage<ResponsePayload>) object).getPayLoad() : (ResponsePayload) object);
            if (payload != null) {
                Throwable exception = payload.getException();
                Object response = payload.getResponse();
                if (response != null) {
                    write(serializer, RES_CLASS, getTypeName(response, payload.getType()), AFTER);
                    write(serializer, RESPONSE, response, NONE);
                } else if (exception != null) {
                    write(serializer, RES_CLASS, getCanonicalName(exception.getClass()), AFTER);
                    write(serializer, EXCEPTION, exception, NONE);
                }
            }
            writeObjectEnd(serializer);
        }
    }

    /**
     * 获取应答的类型名称
     *
     * @param response 应答
     * @param type     类型
     * @return 类型名称
     */
    protected String getTypeName(final Object response, final Type type) {
        return type == null ? getCanonicalName(response.getClass()) : type.getTypeName();
    }
}
