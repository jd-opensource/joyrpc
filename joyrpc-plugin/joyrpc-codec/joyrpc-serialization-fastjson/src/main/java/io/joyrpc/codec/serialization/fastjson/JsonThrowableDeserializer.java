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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.parser.*;
import com.alibaba.fastjson.parser.deserializer.FieldDeserializer;
import com.alibaba.fastjson.parser.deserializer.JavaBeanDeserializer;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import io.joyrpc.constants.Constants;
import io.joyrpc.exception.CreationException;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.util.ClassUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static io.joyrpc.util.ClassUtils.createException;

public class JsonThrowableDeserializer extends JavaBeanDeserializer {

    public JsonThrowableDeserializer(ParserConfig mapping, Class<?> clazz) {
        super(mapping, clazz, clazz);
    }

    @SuppressWarnings("unchecked")
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        JSONLexer lexer = parser.lexer;

        if (lexer.token() == JSONToken.NULL) {
            lexer.nextToken();
            return null;
        }

        if (parser.getResolveStatus() == DefaultJSONParser.TypeNameRedirect) {
            parser.setResolveStatus(DefaultJSONParser.NONE);
        } else {
            if (lexer.token() != JSONToken.LBRACE) {
                throw new JSONException("syntax error");
            }
        }

        Throwable cause = null;
        Class<?> exClass = null;

        if (type != null && type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (Throwable.class.isAssignableFrom(clazz)) {
                exClass = clazz;
            }
        }

        String message = null;
        StackTraceElement[] stackTrace = null;
        Map<String, Object> otherValues = null;


        for (; ; ) {
            // lexer.scanSymbol
            String key = lexer.scanSymbol(parser.getSymbolTable());

            if (key == null) {
                if (lexer.token() == JSONToken.RBRACE) {
                    lexer.nextToken(JSONToken.COMMA);
                    break;
                }
                if (lexer.token() == JSONToken.COMMA) {
                    if (lexer.isEnabled(Feature.AllowArbitraryCommas)) {
                        continue;
                    }
                }
            }

            lexer.nextTokenWithColon(JSONToken.LITERAL_STRING);

            if (JSON.DEFAULT_TYPE_KEY.equals(key)) {
                exClass = parseThrowableClass(lexer);
            } else if (Constants.FIELD_MESSAGE.equals(key)) {
                message = parseMessage(lexer);
            } else if (Constants.FIELD_CAUSE.equals(key)) {
                cause = deserialze(parser, null, Constants.FIELD_CAUSE);
            } else if (Constants.FIELD_STACKTRACE.equals(key)) {
                stackTrace = parser.parseObject(StackTraceElement[].class);
            } else {
                if (otherValues == null) {
                    otherValues = new HashMap();
                }
                otherValues.put(key, parser.parse());
            }
            if (lexer.token() == JSONToken.RBRACE) {
                lexer.nextToken(JSONToken.COMMA);
                break;
            }
        }

        Throwable ex = null;
        try {
            ex = createException(exClass, message, cause, stackTrace);
        } catch (CreationException e) {
            throw new SerializerException(e.getMessage(), e.getCause());
        }

        if (otherValues != null) {
            JavaBeanDeserializer exBeanDeser = null;

            if (exClass != null) {
                if (exClass == clazz) {
                    exBeanDeser = this;
                } else {
                    ObjectDeserializer exDeser = parser.getConfig().getDeserializer(exClass);
                    if (exDeser instanceof JavaBeanDeserializer) {
                        exBeanDeser = (JavaBeanDeserializer) exDeser;
                    }
                }
            }

            if (exBeanDeser != null) {
                for (Map.Entry<String, Object> entry : otherValues.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    FieldDeserializer fieldDeserializer = exBeanDeser.getFieldDeserializer(key);
                    if (fieldDeserializer != null) {
                        fieldDeserializer.setValue(ex, value);
                    }
                }
            }
        }

        return (T) ex;
    }

    protected String parseMessage(JSONLexer lexer) {
        String message;
        if (lexer.token() == JSONToken.NULL) {
            message = null;
        } else if (lexer.token() == JSONToken.LITERAL_STRING) {
            message = lexer.stringVal();
        } else {
            throw new JSONException("syntax error");
        }
        lexer.nextToken();
        return message;
    }

    protected Class<?> parseThrowableClass(JSONLexer lexer) {
        Class<?> exClass;
        if (lexer.token() == JSONToken.LITERAL_STRING) {
            String exClassName = lexer.stringVal();
            try {
                exClass = ClassUtils.forName(exClassName);
                if (!Throwable.class.isAssignableFrom(exClass)) {
                    throw new SerializerException(("invalid throwable class " + exClassName));
                }
            } catch (ClassNotFoundException e) {
                throw new SerializerException(("invalid throwable class " + exClassName));
            }
        } else {
            throw new JSONException("syntax error");
        }
        lexer.nextToken(JSONToken.COMMA);
        return exClass;
    }

    public int getFastMatchToken() {
        return JSONToken.LBRACE;
    }


}
