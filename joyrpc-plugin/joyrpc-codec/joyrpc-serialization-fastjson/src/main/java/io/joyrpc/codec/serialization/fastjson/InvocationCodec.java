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

import com.alibaba.fastjson.JSONReader;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.JSONLexer;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.AutowiredObjectDeserializer;
import com.alibaba.fastjson.serializer.AutowiredObjectSerializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import io.joyrpc.Callback;
import io.joyrpc.exception.CodecException;
import io.joyrpc.exception.MethodOverloadException;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.util.ClassUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.joyrpc.protocol.message.Invocation.*;
import static io.joyrpc.util.ClassUtils.getPublicMethod;

/**
 * Title: Invocation fastjson 序列化<br>
 * <p/>
 * Description: <br>
 * 保证序列化字段按如下顺序：<br>
 * 1、class name 即接口名称<br>
 * 2、alias<br>
 * 3、method name<br>
 * 4、argsType callback 调用才会写
 * 5、args 参数value<br>
 * 6、attachments (值不为空则序列化)<br>
 * <p/>
 */
public class InvocationCodec implements AutowiredObjectSerializer, AutowiredObjectDeserializer {

    public static final InvocationCodec INSTANCE = new InvocationCodec();

    @Override
    public Set<Type> getAutowiredFor() {
        Set<Type> result = new HashSet<>(1);
        result.add(Invocation.class);
        return result;
    }

    @Override
    public void write(final JSONSerializer serializer, final Object object, final Object fieldName, final Type fieldType, final int features) throws IOException {
        if (object == null) {
            serializer.writeNull();
        } else {
            Invocation invocation = (Invocation) object;
            SerializeWriter out = serializer.getWriter();
            out.append('{');
            //1、class name
            out.writeFieldName(CLASS_NAME);
            out.writeString(invocation.getClassName());
            out.append(",");
            //2、alias
            out.writeFieldName(ALIAS);
            out.writeString(invocation.getAlias());
            out.append(",");
            //3、method name
            out.writeFieldName(METHOD_NAME);
            out.writeString(invocation.getMethodName());
            out.append(",");
            //4.argsType
            if (Callback.class.isAssignableFrom(invocation.getClazz())) {
                //回调需要写上实际的参数类型
                ObjectSerializer argsTypeSerializer = serializer.getObjectWriter(String[].class);
                out.writeFieldName(ARGS_TYPE);
                argsTypeSerializer.write(serializer, invocation.computeArgsType(), ARGS_TYPE, null, features);
                out.append(",");
            }
            //5、args
            out.writeFieldName(ARGS);
            ObjectSerializer argsSerializer = serializer.getObjectWriter(Object[].class);
            if (invocation.getArgs() == null || invocation.getArgsType() != null && invocation.getArgsType().length == 0) {
                out.writeNull();
            } else {
                argsSerializer.write(serializer, invocation.getArgs(), ARGS, null, features);
            }
            //7、attachments
            Map<String, Object> attachments = invocation.getAttachments();
            if (attachments != null && !attachments.isEmpty()) {
                out.append(",");
                out.writeFieldName(ATTACHMENTS);
                ObjectSerializer mapSerializer = serializer.getObjectWriter(Map.class);
                mapSerializer.write(serializer, attachments, ATTACHMENTS, null, features);
            }
            out.append('}');
        }
    }

    @Override
    public int getFastMatchToken() {
        return JSONToken.LBRACE;
    }

    @Override
    public <T> T deserialze(final DefaultJSONParser parser, final Type type, final Object fieldName) {
        JSONLexer lexer = parser.getLexer();
        switch (lexer.token()) {
            case JSONToken.NULL:
                lexer.nextToken();
                return null;
            case JSONToken.LBRACE:
                return (T) parse(parser, lexer, new Invocation());
            default:
                return null;
        }
    }

    /**
     * 解析Invocation
     *
     * @param parser
     * @param lexer
     * @param invocation
     * @return
     */
    protected Invocation parse(final DefaultJSONParser parser, final JSONLexer lexer, final Invocation invocation) {
        String key;
        int token;
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
            token = lexer.token();

            if (CLASS_NAME.equals(key)) {
                if (token == JSONToken.LITERAL_STRING) {
                    invocation.setClassName(lexer.stringVal());
                    lexer.nextToken();
                }
            } else if (ALIAS.equals(key)) {
                if (token == JSONToken.LITERAL_STRING) {
                    invocation.setAlias(lexer.stringVal());
                } else {
                    if (!Callback.class.isAssignableFrom(invocation.getClazz())) {
                        throw new CodecException("syntax error: alias is empty");
                    }
                }
                lexer.nextToken();
            } else if (METHOD_NAME.equals(key)) {
                if (token == JSONToken.LITERAL_STRING) {
                    invocation.setMethodName(lexer.stringVal());
                    lexer.nextToken();
                } else {
                    throw new CodecException("syntax error:method is empty");
                }
            } else if (ARGS_TYPE.equals(key)) {
                invocation.setArgsType(parser.parseObject(String[].class));
            } else if (ARGS.equals(key)) {
                Class[] argsClass;
                try {
                    if (invocation.getArgsType() != null) {
                        argsClass = ClassUtils.getClasses(invocation.getArgsType());
                    } else {
                        Method method = getPublicMethod(invocation.getClassName(), invocation.getMethodName());
                        //如果调用此方法返回null说明在接口中没有找到对应的方法，接口发生变化
                        argsClass = method.getParameterTypes();
                    }
                } catch (ClassNotFoundException e) {
                    throw new CodecException(e.getMessage());
                } catch (NoSuchMethodException e) {
                    throw new CodecException(e.getMessage());
                } catch (MethodOverloadException e) {
                    throw new CodecException(e.getMessage());
                }
                invocation.setArgsType(argsClass);
                parseArgs(parser, argsClass, invocation);
            } else if (ATTACHMENTS.equals(key)) {
                invocation.addAttachments(parser.parseObject());
            }
            token = lexer.token();
            if (token == JSONToken.RBRACE) {
                lexer.nextToken(JSONToken.COMMA);
                break;
            }
        }
        return invocation;
    }

    /**
     * 解析参数
     *
     * @param parser
     * @param argsClass
     * @param invocation
     */
    protected void parseArgs(final DefaultJSONParser parser, final Class[] argsClass, final Invocation invocation) {
        JSONReader reader = new JSONReader(parser);
        reader.startArray();
        int i = 0;
        Object[] objects = new Object[argsClass.length];
        while (reader.hasNext()) {
            if (i >= objects.length) {
                throw new CodecException(String.format("the method %s of %s argument length is larger than %d",
                        invocation.getMethodName(), invocation.getClassName(), objects.length));
            }
            objects[i] = reader.readObject(argsClass[i]);
            i++;
        }
        reader.endArray();
        invocation.setArgs(objects);

    }
}
