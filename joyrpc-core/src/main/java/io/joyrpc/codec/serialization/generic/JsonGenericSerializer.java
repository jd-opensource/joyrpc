package io.joyrpc.codec.serialization.generic;

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

import io.joyrpc.Plugin;
import io.joyrpc.codec.serialization.GenericSerializer;
import io.joyrpc.codec.serialization.Json;
import io.joyrpc.codec.serialization.UnsafeByteArrayInputStream;
import io.joyrpc.exception.CodecException;
import io.joyrpc.exception.MethodOverloadException;
import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.message.Call;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * JSON序列化
 */
@Extension(GenericSerializer.JSON)
public class JsonGenericSerializer implements GenericSerializer {

    protected static final Object NULL = new Object();

    protected static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();

    /**
     * JSON插件
     */
    protected final Json json = Plugin.JSON.get();

    @Override
    public Object serialize(final Object object) throws CodecException {
        return object == null || object instanceof Void ? null : json.toJSONBytes(object);
    }

    @Override
    public Object[] deserialize(final Call invocation) throws CodecException {
        try {
            Parameter[] parameters = invocation.getMethod().getParameters();
            if (parameters.length == 0) {
                return new Object[0];
            } else {
                //计算真实的类型，处理了泛型调用
                Type[] types = invocation.computeTypes();
                Object[] paramArgs = (Object[]) (invocation.getArgs()[2]);
                byte[] json = null;
                if (paramArgs != null && paramArgs.length > 0) {
                    json = paramArgs[0] instanceof byte[] ? (byte[]) paramArgs[0] :
                            ((String) paramArgs[0]).startsWith("[") || ((String) paramArgs[0]).startsWith("{") ?
                                    ((String) paramArgs[0]).getBytes(UTF_8) :
                                    BASE64_DECODER.decode((String) paramArgs[0]);
                }
                if (json == null || json.length == 0) {
                    throw new CodecException("The number of parameter is wrong.");
                } else {
                    switch (json[0]) {
                        case '[':
                            return parseArray(parameters, types, json);
                        case '{':
                            return parseObject(parameters, types, json);
                        default:
                            throw new CodecException("The content is not json format.");
                    }
                }
            }
        } catch (NoSuchMethodException | MethodOverloadException | ClassNotFoundException e) {
            throw new CodecException(e.getMessage());
        }
    }

    /**
     * 解析数组
     *
     * @param parameters   参数对象
     * @param genericTypes 参数泛化信息
     * @param text         文本内容
     * @return
     */
    protected Object[] parseArray(final Parameter[] parameters, final Type[] genericTypes, final byte[] text) {
        final int[] index = new int[]{0};
        final Object[] result = new Object[parameters.length];
        json.parseArray(new UnsafeByteArrayInputStream(text), o -> {
            if (index[0] < parameters.length) {
                result[index[0]] = o.apply(genericTypes[index[0]]);
            } else {
                //忽略掉多余的参数
                o.apply(Object.class);
            }
            ++index[0];
            return true;
        });
        if (index[0] != parameters.length) {
            throw new CodecException("The number of parameter is wrong.");
        }
        return result;
    }


    /**
     * 解析数组
     *
     * @param parameters   参数对象
     * @param genericTypes 参数泛化信息
     * @param text         文本内容
     * @return 参数数组
     */
    protected Object[] parseObject(final Parameter[] parameters, final Type[] genericTypes, final byte[] text) {
        if (parameters.length == 1) {
            return new Object[]{json.parseObject(new UnsafeByteArrayInputStream(text), genericTypes[0])};
        }
        final int[] index = new int[]{0};
        final Object[] result = new Object[parameters.length];
        Map<String, Integer> names = new HashMap<>(parameters.length);
        Parameter parameter;
        for (int i = 0; i < parameters.length; i++) {
            parameter = parameters[i];
            if (parameter.isNamePresent()) {
                names.put(parameter.getName(), i);
                names.put("arg" + i, i);
            } else {
                names.put(parameter.getName(), i);
            }
        }
        json.parseObject(new UnsafeByteArrayInputStream(text), (k, o) -> {
            //根据名称获取参数位置
            Integer pos = names.get(k);
            if (pos != null) {
                result[pos] = o.apply(genericTypes[pos]);
                //null设置为NULL对象
                if (result[pos] == null) {
                    result[pos] = NULL;
                }
            } else {
                //按照空位顺序占位
                for (int i = index[0]; i < parameters.length; i++) {
                    if (result[i] == null) {
                        result[i] = o.apply(genericTypes[i]);
                        //null设置为NULL对象
                        if (result[i] == null) {
                            result[i] = NULL;
                        }
                        //递增
                        index[0] = i + 1;
                        break;
                    }
                }
            }
            return true;
        });
        //判断参数是否足够
        for (int i = 0; i < result.length; i++) {
            if (result[i] == null) {
                throw new CodecException("The number of parameter is wrong.");
            } else if (result[i] == NULL) {
                result[i] = null;
            }
        }
        return result;
    }

}
