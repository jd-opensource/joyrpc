package io.joyrpc.extension;

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

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态配置集处理上下文
 */
public class Context implements Serializable, Cloneable {

    // 参数
    protected ConcurrentHashMap<String, Object> parameters = new ConcurrentHashMap<String, Object>();

    /**
     * 获取指定类型的参数
     *
     * @param name  参数名称
     * @param clazz 类型
     * @return 参数对象
     */
    public <T> T getObject(final String name, final Class<T> clazz) {
        return (T) getObject(name);
    }

    /**
     * 获取对象参数
     *
     * @param name 参数名称
     * @return 参数对象
     */
    public <T> T getObject(final String name) {
        return (T) parameters.get(name);
    }

    /**
     * 获取字符串参数
     *
     * @param name 名称
     * @return 字符串参数
     */
    public String getString(final String name) {
        return Converts.getString(getObject(name));
    }

    /**
     * 获取字符串参数，如果为空字符串则返回默认值
     *
     * @param name 名称
     * @param def  默认值
     * @return 字符串参数
     */
    public String getString(final String name, final String def) {
        return Converts.getString(getObject(name), def);
    }

    /**
     * 获取字节参数
     *
     * @param name 名称
     * @return 浮点数参数
     */
    public Byte getByte(final String name) {
        return Converts.getByte(getObject(name), null);
    }

    /**
     * 获取字节参数
     *
     * @param name 名称
     * @param def  默认值
     * @return 浮点数参数
     */
    public Byte getByte(final String name, final Byte def) {
        return Converts.getByte(getObject(name), def);
    }

    /**
     * 获取短整数参数
     *
     * @param name 名称
     * @return 浮点数参数
     */
    public Short getShort(final String name) {
        return Converts.getShort(getObject(name), null);
    }

    /**
     * 获取短整数参数
     *
     * @param name 名称
     * @param def  默认值
     * @return 浮点数参数
     */
    public Short getShort(final String name, final Short def) {
        return Converts.getShort(getObject(name), def);
    }

    /**
     * 获取整数参数
     *
     * @param name 名称
     * @return 整数
     */
    public Integer getInteger(final String name) {
        return Converts.getInteger(getObject(name), null);
    }

    /**
     * 获取整数参数
     *
     * @param name 名称
     * @param def  默认值
     * @return 整数
     */
    public Integer getInteger(final String name, final Integer def) {
        return Converts.getInteger(getObject(name), def);
    }

    /**
     * 获取长整形参数
     *
     * @param name 名称
     * @return 长整形参数
     */
    public Long getLong(final String name) {
        return Converts.getLong(getObject(name), null);
    }

    /**
     * 获取长整形参数
     *
     * @param name 名称
     * @param def  默认值
     * @return 长整形参数
     */
    public Long getLong(final String name, final Long def) {
        return Converts.getLong(getObject(name), def);
    }

    /**
     * 获取单精度浮点数参数
     *
     * @param name 名称
     * @return 浮点数参数
     */
    public Float getFloat(final String name) {
        return Converts.getFloat(getObject(name), null);
    }

    /**
     * 获取单精度浮点数参数
     *
     * @param name 名称
     * @param def  默认值
     * @return 浮点数参数
     */
    public Float getFloat(final String name, final Float def) {
        return Converts.getFloat(getObject(name), def);
    }

    /**
     * 获取双精度浮点数参数
     *
     * @param name 名称
     * @return 浮点数参数
     */
    public Double getDouble(final String name) {
        return Converts.getDouble(getObject(name), null);
    }

    /**
     * 获取双精度浮点数参数
     *
     * @param name 名称
     * @param def  默认值
     * @return 浮点数参数
     */
    public Double getDouble(final String name, final Double def) {
        return Converts.getDouble(getObject(name), def);
    }

    /**
     * 获取布尔值
     *
     * @param name 名称
     * @return 布尔值
     */
    public Boolean getBoolean(final String name) {
        return Converts.getBoolean(getObject(name), null);
    }

    /**
     * 获取布尔值
     *
     * @param name 名称
     * @param def  默认值
     * @return 布尔值
     */
    public Boolean getBoolean(final String name, final Boolean def) {
        return Converts.getBoolean(getObject(name), def);
    }

    /**
     * 获取日期参数值，日期是从EPOCH的毫秒数
     *
     * @param name 参数名称
     * @return 参数值
     */
    public Date getDate(final String name) {
        return Converts.getDate(getObject(name), (Date) null);
    }

    /**
     * 获取日期参数值，日期是从EPOCH的毫秒数
     *
     * @param name 参数名称
     * @param def  默认值
     * @return 参数值
     */
    public Date getDate(final String name, final Date def) {
        return Converts.getDate(getObject(name), def);
    }

    /**
     * 获取日期参数值，日期格式为字符串
     *
     * @param name   参数名称
     * @param format 日期格式
     * @return 参数值
     */
    public Date getDate(final String name, final SimpleDateFormat format) {
        return Converts.getDate(getObject(name), format);
    }

    /**
     * 获取日期参数值，日期格式为字符串
     *
     * @param name   参数名称
     * @param format 日期格式
     * @param def    默认值
     * @return 参数值
     */
    public Date getDate(final String name, final SimpleDateFormat format, final Date def) {
        return Converts.getDate(getObject(name), format, def);
    }

    /**
     * 获取日期参数值，日期格式为字符串
     *
     * @param name   参数名称
     * @param format 日期格式
     * @param def    默认值
     * @return 参数值
     */
    public Date getDate(final String name, final String format, final String def) {
        return Converts.getDate(getObject(name), format, def);
    }

    /**
     * 获取正整数参数
     *
     * @param name 名称
     * @param def  默认值
     * @return 正整数
     */
    public Byte getPositive(final String name, final Byte def) {
        return Converts.getPositive(getObject(name), def);
    }

    /**
     * 获取正整数参数
     *
     * @param name 名称
     * @param def  默认值
     * @return 正整数
     */
    public Short getPositive(final String name, final Short def) {
        return Converts.getPositive(getObject(name), def);
    }

    /**
     * 获取正整数参数
     *
     * @param name 名称
     * @param def  默认值
     * @return 正整数
     */
    public Integer getPositive(final String name, final Integer def) {
        return Converts.getPositive(getObject(name), def);
    }

    /**
     * 获取长正整数参数
     *
     * @param name 名称
     * @param def  默认值
     * @return 长正整数
     */
    public Long getPositive(final String name, final Long def) {
        return Converts.getPositive(getObject(name), def);
    }

    /**
     * 获取短整数自然数参数
     *
     * @param name 名称
     * @param def  默认值
     * @return 自然数
     */
    public Short getNatural(final String name, final Short def) {
        return Converts.getNatural(getObject(name), def);
    }

    /**
     * 获取短整数自然数参数
     *
     * @param name 名称
     * @param def  默认值
     * @return 自然数
     */
    public Byte getNatural(final String name, final Byte def) {
        return Converts.getNatural(getObject(name), def);
    }

    /**
     * 获取整数自然数参数
     *
     * @param name 名称
     * @param def  默认值
     * @return 自然数
     */
    public Integer getNatural(final String name, final Integer def) {
        return Converts.getNatural(getObject(name), def);
    }

    /**
     * 获取长整数自然数参数
     *
     * @param name 名称
     * @param def  默认值
     * @return 自然数
     */
    public Long getNatural(final String name, final Long def) {
        return Converts.getNatural(getObject(name), def);
    }

    /**
     * 存放键值对
     *
     * @param name  键
     * @param value 值
     * @return 先前的对象
     */
    public Object put(final String name, final Object value) {
        return name == null || value == null ? null : parameters.put(name, value);
    }

    /**
     * 存放键值对
     *
     * @param name  键
     * @param value 值
     * @return 先前的对象
     */
    public Object putIfNotNull(final String name, final Object value) {
        return name == null || value == null ? null : parameters.put(name, value);
    }

    /**
     * 存放键值对
     *
     * @param name  键
     * @param value 值
     * @return 先前的对象
     */
    public Object putIfAbsent(final String name, final Object value) {
        return name == null || value == null ? null : parameters.putIfAbsent(name, value);
    }

    /**
     * 存放键值
     *
     * @param map 键值对
     */
    public void put(final Map<String, ?> map) {
        if (map != null) {
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 存放键值
     *
     * @param map 键值对
     */
    public void putIfAbsent(final Map<String, ?> map) {
        if (map != null) {
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 存放键值
     *
     * @param context 上下文
     */
    public void put(final Context context) {
        if (context != null) {
            put(context.parameters);
        }
    }

    /**
     * 存放键值
     *
     * @param context 上下文
     */
    public void putIfAbsent(final Context context) {
        if (context != null) {
            putIfAbsent(context.parameters);
        }
    }

    /**
     * 删除参数
     *
     * @param name 参数名称
     * @return 参数值
     */
    public Object remove(final String name) {
        return name == null ? null : parameters.remove(name);
    }

    /**
     * 清理所有参数
     */
    public void remove() {
        parameters.clear();
    }

    /**
     * 转换成Map对象
     *
     * @return Map对象
     */
    public Map<String, Object> toMap() {
        return new HashMap<String, Object>(parameters);
    }

    /**
     * 获取迭代器
     *
     * @return 迭代器
     */
    public Iterator<Map.Entry<String, Object>> iterator() {
        return parameters.entrySet().iterator();
    }

    @Override
    public Context clone() {
        Context result = new Context();
        result.put(parameters);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Context context = (Context) o;

        return parameters != null ? parameters.equals(context.parameters) : context.parameters == null;

    }

    @Override
    public int hashCode() {
        return parameters != null ? parameters.hashCode() : 0;
    }

}
