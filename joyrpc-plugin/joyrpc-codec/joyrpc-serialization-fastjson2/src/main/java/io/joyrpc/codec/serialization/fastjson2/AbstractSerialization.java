package io.joyrpc.codec.serialization.fastjson2;

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

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.writer.ObjectWriter;
import io.joyrpc.exception.SerializerException;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * 抽象序列化
 */
public abstract class AbstractSerialization<T> implements ObjectWriter<T>, ObjectReader<T> {

    @Override
    public long getFeatures() {
        return 0;
    }

    @Override
    public void write(final JSONWriter jsonWriter, final Object object, final Object fieldName, final Type fieldType, final long features) {
        if (object == null) {
            jsonWriter.writeNull();
        }
        jsonWriter.writeString(object.toString());
    }

    /**
     * 写对象，对象不为空
     *
     * @param jsonWriter 写入器
     * @param object     对象
     * @param fieldName  字段名称
     * @param fieldType  字段值
     * @param features   特性
     */
    protected abstract void doWrite(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features);

    @Override
    public T readObject(final JSONReader jsonReader, final Type fieldType, final Object fieldName, final long features) {
        return jsonReader.readIfNull() ? null : doRead(jsonReader, fieldType, fieldName, features);
    }

    /**
     * 读对象，对象不为空
     *
     * @param jsonReader 读取器
     * @param fieldType  字段类型
     * @param fieldName  字段名称
     * @param features   特性
     * @return 对象
     */
    protected abstract T doRead(JSONReader jsonReader, Type fieldType, Object fieldName, long features);

    /**
     * 开始对象输出
     *
     * @param jsonWriter 输出
     */
    protected void writeObjectBegin(final JSONWriter jsonWriter) {
        jsonWriter.startObject();
    }

    /**
     * j结束对象输出
     *
     * @param jsonWriter 输出
     */
    protected void writeObjectEnd(final JSONWriter jsonWriter) {
        jsonWriter.endObject();
    }

    /**
     * 写值
     *
     * @param jsonWriter 写入器
     * @param fieldName  字段
     * @param value      值
     * @param ignoreNull 是否忽略null值
     */
    protected void writeString(final JSONWriter jsonWriter, final String fieldName, final String value, final boolean ignoreNull) {
        if (value != null || !ignoreNull) {
            jsonWriter.writeName(fieldName);
            jsonWriter.writeColon();
            jsonWriter.writeString(value);
        }
    }

    /**
     * 写值
     *
     * @param jsonWriter 写入器
     * @param fieldName  字段
     * @param value      值
     * @param ignoreNull 是否忽略null值
     */
    protected void writeObject(final JSONWriter jsonWriter, final String fieldName, final Object value, final boolean ignoreNull) {
        if (value != null || !ignoreNull) {
            ObjectWriter objectWriter = jsonWriter.getObjectWriter(value.getClass());
            jsonWriter.writeName(fieldName);
            jsonWriter.writeColon();
            if (value == null) {
                jsonWriter.writeNull();
            } else {
                objectWriter.write(jsonWriter, value);
            }
        }
    }

    /**
     * 读取字符串
     *
     * @param jsonReader 读取器
     * @param fieldName  字段
     * @param nullable   是否可以null
     */
    protected String readString(final JSONReader jsonReader, final String fieldName, final boolean nullable) {
        String result = jsonReader.readString();
        if (result == null && !nullable) {
            throw new SerializerException("syntax error: invalid " + fieldName);
        }
        return result;
    }

    /**
     * 读取字符串数组
     *
     * @param jsonReader 读取器
     * @param fieldName  字段
     * @param nullable   是否可以null
     */
    protected String[] readStringArray(final JSONReader jsonReader, final String fieldName, final boolean nullable) {
        List<String> result = jsonReader.readArray(String.class);
        if (result == null) {
            if (!nullable) {
                throw new SerializerException("syntax error: invalid " + fieldName);
            }
            return null;
        }
        return result.toArray(new String[result.size()]);
    }

    /**
     * 读取对象数组
     *
     * @param jsonReader 读取器
     * @param fieldName  字段
     * @param types      类型
     * @param nullable   是否可以null
     */
    protected Object[] readObjectArray(final JSONReader jsonReader, final String fieldName, final Type[] types, final boolean nullable) {
        Object[] result = jsonReader.readArray(types);
        if (result == null && types.length != 0 && !nullable) {
            throw new SerializerException("syntax error: invalid " + fieldName);
        }
        return result;
    }

    /**
     * 读取Map
     *
     * @param jsonReader 读取器
     * @param fieldName  字段
     * @param nullable   是否可以null
     */
    protected Map<String, Object> readObject(final JSONReader jsonReader, final String fieldName, final boolean nullable) {
        Map<String, Object> result = jsonReader.readObject();
        if (result == null) {
            if (!nullable) {
                throw new SerializerException("syntax error: invalid " + fieldName);
            }
        }
        return result;
    }
}
