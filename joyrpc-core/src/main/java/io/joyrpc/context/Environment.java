package io.joyrpc.context;

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

import io.joyrpc.constants.Constants;
import io.joyrpc.extension.Extensible;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * 环境
 */
@Extensible("environment")
public interface Environment {

    /**
     * 添加环境变量
     *
     * @param key
     * @param value
     */
    void put(String key, Object value);

    /**
     * 获取环境项
     *
     * @param key
     * @return
     */
    Property getProperty(String key);

    /**
     * 迭代变量
     *
     * @return
     */
    Collection<Property> properties();

    /**
     * 获取值
     *
     * @param key 键
     * @return
     */
    default <T> T getValue(final String key) {
        Property property = getProperty(key);
        return property == null ? null : property.getValue();
    }

    /**
     * 获取字符串参数值
     *
     * @param key 键
     * @return
     */
    default String getString(final String key) {
        Property property = getProperty(key);
        return property == null ? null : property.getString();
    }

    /**
     * 获取字符串参数值
     *
     * @param key 键
     * @param def 默认值
     * @return 参数值
     */
    default String getString(final String key, final String def) {
        Property property = getProperty(key);
        return property == null ? def : property.getString(def);
    }

    /**
     * 获取日期参数值，日期是从EPOCH的毫秒数
     *
     * @param key 键
     * @param def 默认值
     * @return 参数值
     */
    default Date getDate(final String key, final Date def) {
        Property property = getProperty(key);
        return property == null ? def : property.getDate(def);
    }

    /**
     * 获取日期参数值，日期格式为字符串
     * <p>
     *
     * @param key    键
     * @param format 日期格式
     * @return 参数值
     */
    default Date getDate(final String key, final SimpleDateFormat format) {
        Property property = getProperty(key);
        return property == null ? null : property.getDate(format);
    }

    /**
     * 获取日期参数值，日期格式为字符串
     * <p>
     *
     * @param key    键
     * @param format 日期格式
     * @param def    默认值
     * @return 参数值
     */
    default Date getDate(final String key, final SimpleDateFormat format, final Date def) {
        Property property = getProperty(key);
        return property == null ? def : property.getDate(format, def);
    }

    /**
     * 获取单精度浮点数参数值
     *
     * @param key 键
     * @return 参数值
     */
    default Float getFloat(final String key) {
        Property property = getProperty(key);
        return property == null ? null : property.getFloat();
    }

    /**
     * 获取单精度浮点数参数值
     *
     * @param key 键
     * @param def 默认值
     * @return 参数值
     */
    default Float getFloat(final String key, final Float def) {
        Property property = getProperty(key);
        return property == null ? def : property.getFloat(def);
    }

    /**
     * 获取双精度浮点数参数值
     *
     * @param key 键
     * @return 参数值
     */
    default Double getDouble(final String key) {
        Property property = getProperty(key);
        return property == null ? null : property.getDouble();
    }

    /**
     * 获取双精度浮点数参数值
     *
     * @param key 键
     * @param def 默认值
     * @return 参数值
     */
    default Double getDouble(final String key, final Double def) {
        Property property = getProperty(key);
        return property == null ? def : property.getDouble(def);
    }

    /**
     * 获取长整形参数值
     *
     * @param key 键
     * @return 参数值
     */
    default Long getLong(final String key) {
        Property property = getProperty(key);
        return property == null ? null : property.getLong();
    }

    /**
     * 获取长整形参数值
     *
     * @param key 键
     * @param def 默认值
     * @return 参数值
     */
    default Long getLong(final String key, final Long def) {
        Property property = getProperty(key);
        return property == null ? def : property.getLong(def);
    }

    /**
     * 获取整形参数值
     *
     * @param key 键
     * @return 参数值
     */
    default Integer getInteger(final String key) {
        Property property = getProperty(key);
        return property == null ? null : property.getInteger();
    }

    /**
     * 获取整形参数值
     *
     * @param key 键
     * @param def 默认值
     * @return 参数值
     */
    default Integer getInteger(final String key, final Integer def) {
        Property property = getProperty(key);
        return property == null ? def : property.getInteger(def);
    }

    /**
     * 获取短整形参数值
     *
     * @param key 键
     * @return 参数值
     */
    default Short getShort(final String key) {
        Property property = getProperty(key);
        return property == null ? null : property.getShort();
    }

    /**
     * 获取短整形参数值
     *
     * @param key 键
     * @param def 默认值
     * @return 参数值
     */
    default Short getShort(final String key, final Short def) {
        Property property = getProperty(key);
        return property == null ? def : property.getShort(def);
    }

    /**
     * 获取字节参数值
     *
     * @param key 键
     * @return 参数值
     */
    default Byte getByte(final String key) {
        Property property = getProperty(key);
        return property == null ? null : property.getByte();
    }


    /**
     * 获取字节参数值
     *
     * @param key 键
     * @param def 默认值
     * @return 参数值
     */
    default Byte getByte(final String key, final Byte def) {
        Property property = getProperty(key);
        return property == null ? def : property.getByte(def);
    }

    /**
     * 获取布尔参数值
     *
     * @return 参数值
     */
    default Boolean getBoolean(final String key) {
        Property property = getProperty(key);
        return property == null ? null : property.getBoolean();
    }

    /**
     * 获取布尔参数值
     *
     * @param key 键
     * @param def 默认值
     * @return 参数值
     */
    default Boolean getBoolean(final String key, final Boolean def) {
        Property property = getProperty(key);
        return property == null ? def : property.getBoolean(def);
    }

    /**
     * 获取长整形自然数参数值
     *
     * @param key 键
     * @param def 默认值
     * @return 参数值
     */
    default Long getNatural(final String key, final Long def) {
        Property property = getProperty(key);
        return property == null ? def : property.getNatural(def);
    }

    /**
     * 获取整形自然数参数值
     *
     * @param key 键
     * @param def 默认值
     * @return 参数值
     */
    default Integer getNatural(final String key, final Integer def) {
        Property property = getProperty(key);
        return property == null ? def : property.getNatural(def);
    }

    /**
     * 获取短整形自然数参数值
     *
     * @param key 键
     * @param def 默认值
     * @return 参数值
     */
    default Short getNatural(final String key, final Short def) {
        Property property = getProperty(key);
        return property == null ? def : property.getNatural(def);
    }

    /**
     * 获取字节自然数参数值
     *
     * @param key 键
     * @param def 默认值
     * @return 参数值
     */
    default Byte getNatural(final String key, final Byte def) {
        Property property = getProperty(key);
        return property == null ? def : property.getNatural(def);
    }

    /**
     * 获取长整形正整数参数值
     *
     * @param key 键
     * @param def 默认值
     * @return 参数值
     */
    default Long getPositive(final String key, final Long def) {
        Property property = getProperty(key);
        return property == null ? def : property.getPositive(def);
    }

    /**
     * 获取整形正整数参数值
     *
     * @param key 键
     * @param def 默认值
     * @return 参数值
     */
    default Integer getPositive(final String key, final Integer def) {
        Property property = getProperty(key);
        return property == null ? def : property.getPositive(def);
    }

    /**
     * 获取短整形正整数参数值
     *
     * @param key 键
     * @param def 默认值
     * @return 参数值
     */
    default Short getPositive(final String key, final Short def) {
        Property property = getProperty(key);
        return property == null ? def : property.getPositive(def);
    }

    /**
     * 获取字节正整数参数值
     *
     * @param key 键
     * @param def 默认值
     * @return 参数值
     */
    default Byte getPositive(final String key, final Byte def) {
        Property property = getProperty(key);
        return property == null ? def : property.getPositive(def);
    }

    /**
     * 获取操作系统类型
     *
     * @return
     */
    default OsType osType() {
        Property property = getProperty(Constants.KEY_OS_TYPE);
        return property == null ? OsType.OTHER : OsType.valueOf(property.getString());
    }

    /**
     * 获取CPU核数
     *
     * @return
     */
    default int cpuCores() {
        Property property = getProperty(Constants.KEY_CPU_CORES);
        return property == null ? Runtime.getRuntime().availableProcessors() : property.getInteger();
    }


    /**
     * 返回所有环境变量
     *
     * @return
     */
    Map<String, String> env();

    /**
     * 打印环境到控制台
     */
    void print();

}
