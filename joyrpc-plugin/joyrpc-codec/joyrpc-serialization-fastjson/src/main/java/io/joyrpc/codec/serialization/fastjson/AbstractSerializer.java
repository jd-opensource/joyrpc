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
import com.alibaba.fastjson.parser.JSONLexer;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import io.joyrpc.exception.SerializerException;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * 序列化基础类
 */
public class AbstractSerializer {

    protected static final int NONE = 0;
    protected static final int BEFORE = 0;
    protected static final int AFTER = 0;

    /**
     * 写值
     *
     * @param out   输出
     * @param field 字段
     * @param value 值
     */
    protected void writeString(final SerializeWriter out, final String field, final String value) {
        writeString(out, field, value, true, AFTER);
    }

    /**
     * 写值
     *
     * @param out        输出
     * @param field      字段
     * @param value      值
     * @param ignoreNull 是否忽略null值
     * @param separator  分隔符位置
     */
    protected void writeString(final SerializeWriter out, final String field, final String value,
                               final boolean ignoreNull, final int separator) {
        if (value != null || !ignoreNull) {
            if (separator == BEFORE) {
                out.write(',');
            }
            out.writeFieldName(field);
            if (value == null) {
                out.writeNull();
            } else {
                out.writeString(value);
            }
            if (separator == AFTER) {
                out.write(',');
            }
        }
    }

    /**
     * 写值
     *
     * @param serializer 输出
     * @param field      字段
     * @param value      值
     * @param separator  分隔符位置
     */
    protected void write(final JSONSerializer serializer, final String field, final Object value, final int separator) {
        write(serializer, field, value, false, separator);
    }

    /**
     * 写值
     *
     * @param serializer 输出
     * @param field      字段
     * @param value      值
     * @param ignoreNull 是否忽略null值
     * @param separator  分隔符位置
     */
    protected void write(final JSONSerializer serializer, final String field, final Object value,
                         final boolean ignoreNull, final int separator) {
        if (value != null || !ignoreNull) {
            SerializeWriter out = serializer.getWriter();
            if (separator == BEFORE) {
                out.write(',');
            }
            out.writeFieldName(field);
            serializer.write(value);
            if (separator == AFTER) {
                out.write(',');
            }
        }
    }

    /**
     * 读取字符串
     *
     * @param lexer    文法
     * @param field    字段
     * @param nullable 是否可以null
     */
    protected String parseString(final JSONLexer lexer, final String field, final boolean nullable) {
        String result = null;
        switch (lexer.token()) {
            case JSONToken.LITERAL_STRING:
                result = lexer.stringVal();
                lexer.nextToken();
                break;
            case JSONToken.NULL:
                if (!nullable) {
                    throw new SerializerException("syntax error: invalid " + field);
                }
                lexer.nextToken();
                break;
            default:
                throw new SerializerException("syntax error: invalid " + field);
        }
        return result;
    }

    /**
     * 读取MAP
     *
     * @param parser 解析器
     * @param lexer  文法
     * @param field  字段
     */
    protected Map<String, Object> parseMap(final DefaultJSONParser parser, final JSONLexer lexer, final String field) {
        Map<String, Object> result = null;
        switch (lexer.token()) {
            case JSONToken.LBRACE:
                result = parser.parseObject();
                break;
            case JSONToken.NULL:
                lexer.nextToken();
                break;
            default:
                throw new SerializerException("syntax error: invalid " + field);
        }
        return result;
    }

    /**
     * 读取字符串数组
     *
     * @param parser 解析器
     * @param lexer  文法
     * @param field  字段
     */
    protected String[] parseStrings(final DefaultJSONParser parser, final JSONLexer lexer, final String field) {
        String result[] = null;
        switch (lexer.token()) {
            case JSONToken.LBRACKET:
                result = parser.parseObject(String[].class);
                break;
            case JSONToken.NULL:
                lexer.nextToken();
                break;
            default:
                throw new SerializerException("syntax error: invalid " + field);
        }
        return result;
    }

    /**
     * 解析对象
     *
     * @param parser 解析器
     * @param lexer  语法
     * @param type   类型
     * @return
     */
    protected Object parseObject(final DefaultJSONParser parser, final JSONLexer lexer, final Type type) {
        Object result = null;
        if (lexer.token() != JSONToken.NULL) {
            result = parser.parseObject(type);
        }
        return result;
    }

    /**
     * 解析对象数组
     *
     * @param parser 解析器
     * @param lexer  语法
     * @param types  类型
     * @param field  字段
     */
    protected Object[] parseObjects(final DefaultJSONParser parser, final JSONLexer lexer, final Type[] types, final String field) {
        Object[] result = null;
        //空数组
        if (lexer.token() == JSONToken.NULL) {
            if (types.length == 0) {
                lexer.nextToken();
            } else {
                throw new SerializerException("syntax error: invalid " + field);
            }
        } else {
            //解析参数
            JSONReader reader = new JSONReader(parser);
            reader.startArray();
            int i = 0;
            result = new Object[types.length];
            while (reader.hasNext()) {
                if (i >= result.length) {
                    throw new SerializerException("syntax error: invalid " + field);
                }
                result[i] = reader.readObject(types[i]);
                i++;
            }
            reader.endArray();
        }
        return result;
    }
}
