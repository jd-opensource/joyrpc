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
import io.joyrpc.exception.CodecException;
import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.message.Invocation;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON序列化
 */
@Extension(GenericSerializer.JSON)
public class JsonGenericSerializer implements GenericSerializer {

    /**
     * JSON插件
     */
    protected final Json json = Plugin.JSON.get();

    @Override
    public Object serialize(final Object object) throws CodecException {
        return object == null || object instanceof Void ? null : json.toJSONBytes(object);
    }

    @Override
    public Object[] deserialize(final Invocation invocation) throws CodecException {
        Parameter[] parameters = invocation.getMethod().getParameters();
        Object[] genericArgs = invocation.getArgs();
        if (parameters.length == 0) {
            return new Object[0];
        } else {
            Object[] paramArgs = genericArgs == null || genericArgs.length < 3 ? null : (Object[]) genericArgs[2];
            byte[] json = paramArgs == null || paramArgs.length == 0 ? null : (byte[]) paramArgs[0];
            if (json == null || json.length == 0) {
                throw new CodecException("The number of parameter is wrong.");
            } else {
                switch (json[0]) {
                    case '[':
                        return parseArray(parameters, json);
                    case '{':
                        return parseObject(parameters, json);
                    default:
                        throw new CodecException("The content is not json format.");
                }
            }
        }
    }

    /**
     * 解析数组
     *
     * @param parameters
     * @param text
     * @return
     */
    protected Object[] parseArray(final Parameter[] parameters, final byte[] text) {
        final int[] index = new int[]{0};
        final Object[] result = new Object[parameters.length];
        json.parseArray(new ByteArrayInputStream(text), o -> {
            if (index[0] < parameters.length) {
                result[index[0]] = o.apply(parameters[index[0]].getParameterizedType());
            } else {
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
     * @param parameters
     * @param text
     * @return
     */
    protected Object[] parseObject(final Parameter[] parameters, final byte[] text) {
        if (parameters.length == 1) {
            return new Object[]{json.parseObject(new ByteArrayInputStream(text), parameters[0].getParameterizedType())};
        }
        final int[] index = new int[]{0};
        final Object[] result = new Object[parameters.length];
        //TODO 参数名称
        Map<String, Integer> names = new HashMap<>(parameters.length);
        for (int i = 0; i < parameters.length; i++) {
            names.put(parameters[i].getName(), i);
            names.put("arg" + i, i);
        }
        json.parseObject(new ByteArrayInputStream(text), (k, o) -> {
            //根据名称获取参数位置
            Integer pos = names.get(k);
            if (pos != null) {
                result[pos] = o.apply(parameters[pos].getParameterizedType());
            } else {
                //按照空位顺序占位
                for (int i = index[0]; i < parameters.length; i++) {
                    if (result[i] == null) {
                        result[i] = o.apply(parameters[i].getParameterizedType());
                        index[0] = i;
                        break;
                    }
                }
            }
            return true;
        });
        //判断参数是否足够
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] == null) {
                throw new CodecException("The number of parameter is wrong.");
            }
        }
        return result;
    }

}
