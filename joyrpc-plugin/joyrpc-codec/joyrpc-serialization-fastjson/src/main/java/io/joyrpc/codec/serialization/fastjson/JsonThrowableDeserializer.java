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
import io.joyrpc.exception.CreationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class JsonThrowableDeserializer extends JavaBeanDeserializer {

    protected static Map<Class, Optional<Instantiation>> instantiations = new ConcurrentHashMap<>();

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
                if (lexer.token() == JSONToken.LITERAL_STRING) {
                    String exClassName = lexer.stringVal();
                    exClass = parser.getConfig().checkAutoType(exClassName, Throwable.class, lexer.getFeatures());
                } else {
                    throw new JSONException("syntax error");
                }
                lexer.nextToken(JSONToken.COMMA);
            } else if ("message".equals(key)) {
                if (lexer.token() == JSONToken.NULL) {
                    message = null;
                } else if (lexer.token() == JSONToken.LITERAL_STRING) {
                    message = lexer.stringVal();
                } else {
                    throw new JSONException("syntax error");
                }
                lexer.nextToken();
            } else if ("cause".equals(key)) {
                cause = deserialze(parser, null, "cause");
            } else if ("stackTrace".equals(key)) {
                stackTrace = parser.parseObject(StackTraceElement[].class);
            } else {
                if (otherValues == null) {
                    otherValues = new HashMap<String, Object>();
                }
                otherValues.put(key, parser.parse());
            }

            if (lexer.token() == JSONToken.RBRACE) {
                lexer.nextToken(JSONToken.COMMA);
                break;
            }
        }

        Throwable ex = null;
        if (exClass == null) {
            ex = new Exception(message, cause);
        } else {
            if (!Throwable.class.isAssignableFrom(exClass)) {
                throw new JSONException("type not match, not Throwable. " + exClass.getName());
            }

            try {
                ex = createException(message, cause, exClass);
                if (ex == null) {
                    ex = new Exception(message, cause);
                }
            } catch (Exception e) {
                throw new JSONException("create instance error", e);
            }
        }

        if (stackTrace != null) {
            ex.setStackTrace(stackTrace);
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

    protected Throwable createException(final String message, final Throwable cause, final Class<?> exClass) throws Exception {

        Optional<Instantiation> optional = instantiations.computeIfAbsent(exClass, o -> {
            Constructor<?> constructor0 = null;
            Constructor<?> constructor1 = null;
            Constructor<?> constructor2 = null;
            Constructor<?> constructorNull = null;
            Class<?>[] types;
            boolean nullable;
            //遍历构造函数
            for (Constructor<?> constructor : o.getConstructors()) {
                types = constructor.getParameterTypes();
                if (types.length == 0) {
                    //无参构造函数
                    constructor0 = constructor;
                } else if (types.length == 1 && types[0] == String.class) {
                    //只带消息参数的构造函数
                    constructor1 = constructor;
                } else if (types.length == 2 && types[0] == String.class && types[1] == Throwable.class) {
                    //带消息和异常参数的构造函数
                    constructor2 = constructor;
                    break;
                } else if (constructorNull == null) {
                    //参数都可以为空的构造函数
                    nullable = true;
                    for (Class<?> type : types) {
                        if (type.isPrimitive()) {
                            nullable = false;
                            break;
                        }
                    }
                    if (nullable) {
                        constructorNull = constructor;
                    }
                }
            }

            if (constructor2 != null) {
                return Optional.of(new Constructor2(constructor2));
            } else if (constructor1 != null) {
                return Optional.of(new Constructor1(constructor1));
            } else if (constructor0 != null) {
                return Optional.of(new Constructor0(constructor0));
            } else if (constructorNull != null) {
                return Optional.of(new ConstructorNull(constructorNull));
            }

            return Optional.empty();
        });
        Instantiation instantiation = optional.get();
        return instantiation == null ? null : instantiation.newInstance(message, cause);

    }

    public int getFastMatchToken() {
        return JSONToken.LBRACE;
    }


    /**
     * 实例化接口
     */
    @FunctionalInterface
    protected interface Instantiation {

        /**
         * 构造
         *
         * @return
         * @throws CreationException
         */
        Throwable newInstance(String message, Throwable cause) throws InstantiationException, IllegalAccessException,
                IllegalArgumentException, InvocationTargetException;
    }

    /**
     * 无参数构造函数实
     */
    protected static class Constructor0 implements Instantiation {

        protected Constructor constructor;

        public Constructor0(final Constructor constructor) {
            this.constructor = constructor;
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
        }

        public Throwable newInstance(final String message, final Throwable cause) throws InstantiationException, IllegalAccessException,
                IllegalArgumentException, InvocationTargetException {
            return (Throwable) constructor.newInstance();
        }
    }

    /**
     * 参数为消息的构造函数
     */
    protected static class Constructor1 extends Constructor0 {

        public Constructor1(Constructor constructor) {
            super(constructor);
        }

        @Override
        public Throwable newInstance(final String message, final Throwable cause) throws InstantiationException, IllegalAccessException,
                IllegalArgumentException, InvocationTargetException {
            return (Throwable) constructor.newInstance(message);
        }
    }

    /**
     * 参数为消息和异常的构造函数
     */
    protected static class Constructor2 extends Constructor0 {

        public Constructor2(Constructor constructor) {
            super(constructor);
        }

        @Override
        public Throwable newInstance(final String message, final Throwable cause) throws InstantiationException, IllegalAccessException,
                IllegalArgumentException, InvocationTargetException {
            return (Throwable) constructor.newInstance(message, cause);
        }
    }

    /**
     * 参数全部可为空的构造函数
     */
    protected static class ConstructorNull extends Constructor0 {

        protected Object[] params;

        public ConstructorNull(Constructor constructor) {
            super(constructor);
            params = new Object[constructor.getParameterCount()];
        }

        @Override
        public Throwable newInstance(final String message, final Throwable cause) throws InstantiationException, IllegalAccessException,
                IllegalArgumentException, InvocationTargetException {
            return (Throwable) constructor.newInstance(params);
        }
    }

}
