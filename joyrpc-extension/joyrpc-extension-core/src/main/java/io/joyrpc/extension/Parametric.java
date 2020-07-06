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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.BiConsumer;

/**
 * 参数化接口
 * 用于获取参数配置信息
 *
 * @date 2019-04-24 18:14
 */
public interface Parametric {
    /**
     * 获取参数，该方法便于绑定
     *
     * @param key
     * @return
     */
    <T> T getObject(String key);

    /**
     * 获取字符串参数值
     *
     * @param key 参数名称
     * @return 参数值
     */
    String getString(String key);

    /**
     * 获取字符串参数值
     *
     * @param key 参数名称
     * @param def 默认值
     * @return 参数值
     */
    String getString(String key, String def);

    /**
     * 获取字符串参数值
     *
     * @param key       参数名称
     * @param candidate 候选参数
     * @param def       默认值
     * @return 参数值
     */
    String getString(String key, String candidate, String def);

    /**
     * 获取值
     *
     * @param option
     * @return
     */
    String getString(URLOption<String> option);

    /**
     * 获取值
     *
     * @param option
     * @return
     */
    String getString(URLBiOption<String> option);

    /**
     * 获取日期参数值，日期是从EPOCH的毫秒数
     *
     * @param key 参数名称
     * @param def 默认值
     * @return 参数值
     */
    Date getDate(String key, Date def);

    /**
     * 获取日期参数值，日期格式为字符串
     *
     * @param key    参数名称
     * @param format 日期格式
     * @return 参数值
     */
    Date getDate(String key, SimpleDateFormat format);

    /**
     * 获取日期参数值，日期格式为字符串
     *
     * @param key    参数名称
     * @param format 日期格式
     * @param def    默认值
     * @return 参数值
     */
    Date getDate(String key, SimpleDateFormat format, Date def);

    /**
     * 获取日期参数值，日期格式为字符串
     *
     * @param key    参数名称
     * @param parser 日期转换
     * @param def    默认值
     * @return 参数值
     */
    Date getDate(final String key, final DateParser parser, final Date def);

    /**
     * 获取单精度浮点数参数值
     *
     * @param key 参数名称
     * @return 参数值
     */
    Float getFloat(String key);

    /**
     * 获取单精度浮点数参数值
     *
     * @param key 参数名称
     * @param def 默认值
     * @return 参数值
     */
    Float getFloat(String key, Float def);

    /**
     * 获取单精度浮点数参数值
     *
     * @param key       参数名称
     * @param candidate 候选参数
     * @param def       默认值
     * @return 参数值
     */
    Float getFloat(String key, String candidate, Float def);

    /**
     * 获取浮点数
     *
     * @param option 选项
     * @return
     */
    Float getFloat(URLOption<Float> option);

    /**
     * 获取浮点数
     *
     * @param option 选项
     * @return
     */
    Float getFloat(URLBiOption<Float> option);

    /**
     * 获取双精度浮点数参数值
     *
     * @param key 参数名称
     * @return 参数值
     */
    Double getDouble(String key);

    /**
     * 获取双精度浮点数参数值
     *
     * @param key       参数名称
     * @param candidate 候选参数
     * @param def       默认值
     * @return 参数值
     */
    Double getDouble(String key, String candidate, Double def);

    /**
     * 获取双精度浮点数参数值
     *
     * @param key 参数名称
     * @param def 默认值
     * @return 参数值
     */
    Double getDouble(String key, Double def);

    /**
     * 获取双精度浮点数
     *
     * @param option 选项
     * @return
     */
    Double getDouble(URLOption<Double> option);

    /**
     * 获取双精度浮点数
     *
     * @param option 选项
     * @return
     */
    Double getDouble(URLBiOption<Double> option);

    /**
     * 获取长整型参数值
     *
     * @param key 参数名称
     * @return 参数值
     */
    Long getLong(String key);

    /**
     * 获取长整型参数值
     *
     * @param key       参数名称
     * @param candidate 候选参数
     * @param def       默认值
     * @return 参数值
     */
    Long getLong(String key, String candidate, Long def);

    /**
     * 获取长整型参数值
     *
     * @param key 参数名称
     * @param def 默认值
     * @return 参数值
     */
    Long getLong(String key, Long def);

    /**
     * 获取长整型参数值
     *
     * @param option 选项
     * @return
     */
    Long getLong(URLOption<Long> option);

    /**
     * 获取长整型参数值
     *
     * @param option 选项
     * @return
     */
    Long getLong(URLBiOption<Long> option);

    /**
     * 获取整型参数值
     *
     * @param key 参数名称
     * @return 参数值
     */
    Integer getInteger(String key);

    /**
     * 获取整型参数值
     *
     * @param key 参数名称
     * @param def 默认值
     * @return 参数值
     */
    Integer getInteger(String key, Integer def);

    /**
     * 获取整型参数值
     *
     * @param key       参数名称
     * @param candidate 候选参数
     * @param def       默认值
     * @return 参数值
     */
    Integer getInteger(String key, String candidate, Integer def);

    /**
     * 获取整型参数值
     *
     * @param option 选项
     * @return
     */
    Integer getInteger(URLOption<Integer> option);

    /**
     * 获取整型参数值
     *
     * @param option 选项
     * @return
     */
    Integer getInteger(URLBiOption<Integer> option);

    /**
     * 获取短整型参数值
     *
     * @param key 参数名称
     * @return 参数值
     */
    Short getShort(String key);

    /**
     * 获取短整型参数值
     *
     * @param key 参数名称
     * @param def 默认值
     * @return 参数值
     */
    Short getShort(String key, Short def);

    /**
     * 获取短整型参数值
     *
     * @param key       参数名称
     * @param candidate 候选参数
     * @param def       默认值
     * @return 参数值
     */
    Short getShort(String key, String candidate, Short def);

    /**
     * 获取短整型参数值
     *
     * @param option 选项
     * @return
     */
    Short getShort(URLOption<Short> option);

    /**
     * 获取短整型参数值
     *
     * @param option 选项
     * @return
     */
    Short getShort(URLBiOption<Short> option);

    /**
     * 获取字节型参数值
     *
     * @param key 参数名称
     * @return 参数值
     */
    Byte getByte(String key);

    /**
     * 获取字节型参数值
     *
     * @param key 参数名称
     * @param def 默认值
     * @return 参数值
     */
    Byte getByte(String key, Byte def);

    /**
     * 获取字节型参数值
     *
     * @param key       参数名称
     * @param candidate 候选参数
     * @param def       默认值
     * @return 参数值
     */
    Byte getByte(String key, String candidate, Byte def);

    /**
     * 获取字节型参数值
     *
     * @param option 选项
     * @return
     */
    Byte getByte(URLOption<Byte> option);

    /**
     * 获取字节型参数值
     *
     * @param option 选项
     * @return
     */
    Byte getByte(URLBiOption<Byte> option);

    /**
     * 获取布尔型参数值
     *
     * @param key 参数名称
     * @return 参数值
     */
    Boolean getBoolean(String key);

    /**
     * 获取布尔型参数值
     *
     * @param key 参数名称
     * @param def 默认值
     * @return 参数值
     */
    Boolean getBoolean(String key, Boolean def);

    /**
     * 获取布尔型参数值
     *
     * @param key       参数名称
     * @param candidate 候选参数
     * @param def       默认值
     * @return 参数值
     */
    Boolean getBoolean(String key, String candidate, Boolean def);

    /**
     * 获取布尔型参数值
     *
     * @param option 选项
     * @return
     */
    Boolean getBoolean(URLOption<Boolean> option);

    /**
     * 获取布尔型参数值
     *
     * @param option 选项
     * @return
     */
    Boolean getBoolean(URLBiOption<Boolean> option);

    /**
     * 获取长整型自然数参数值
     *
     * @param key 参数名称
     * @param def 默认值
     * @return 参数值
     */
    Long getNatural(String key, Long def);

    /**
     * 获取长整型自然数参数值
     *
     * @param key       参数名称
     * @param candidate 候选参数
     * @param def       默认值
     * @return 参数值
     */
    Long getNatural(String key, String candidate, Long def);

    /**
     * 获取长整型自然数参数值
     *
     * @param option 选项
     * @return
     */
    Long getNaturalLong(URLOption<Long> option);

    /**
     * 获取长整型自然数参数值
     *
     * @param option 选项
     * @return
     */
    Long getNaturalLong(URLBiOption<Long> option);

    /**
     * 获取整型自然数参数值
     *
     * @param key 参数名称
     * @param def 默认值
     * @return 参数值
     */
    Integer getNatural(String key, Integer def);

    /**
     * 获取整型自然数参数值
     *
     * @param key       参数名称
     * @param candidate 候选参数
     * @param def       默认值
     * @return 参数值
     */
    Integer getNatural(String key, String candidate, Integer def);

    /**
     * 获取整型自然数参数值
     *
     * @param option 选项
     * @return
     */
    Integer getNaturalInt(URLOption<Integer> option);

    /**
     * 获取整型型自然数参数值
     *
     * @param option 选项
     * @return
     */
    Integer getNaturalInt(URLBiOption<Integer> option);

    /**
     * 获取短整型自然数参数值
     *
     * @param key 参数名称
     * @param def 默认值
     * @return 参数值
     */
    Short getNatural(String key, Short def);

    /**
     * 获取短整型自然数参数值
     *
     * @param key       参数名称
     * @param candidate 候选参数
     * @param def       默认值
     * @return 参数值
     */
    Short getNatural(String key, String candidate, Short def);

    /**
     * 获取短整型自然数参数值
     *
     * @param option 选项
     * @return
     */
    Short getNaturalShort(URLOption<Short> option);

    /**
     * 获取短整型型自然数参数值
     *
     * @param option 选项
     * @return
     */
    Short getNaturalShort(URLBiOption<Short> option);

    /**
     * 获取字节型自然数参数值
     *
     * @param key 参数名称
     * @param def 默认值
     * @return 参数值
     */
    Byte getNatural(String key, Byte def);

    /**
     * 获取字节型自然数参数值
     *
     * @param key       参数名称
     * @param candidate 候选参数
     * @param def       默认值
     * @return 参数值
     */
    Byte getNatural(String key, String candidate, Byte def);

    /**
     * 获取字节型自然数参数值
     *
     * @param option 选项
     * @return
     */
    Byte getNaturalByte(URLOption<Byte> option);

    /**
     * 获取字节型自然数参数值
     *
     * @param option 选项
     * @return
     */
    Byte getNaturalByte(URLBiOption<Byte> option);

    /**
     * 获取长整型正整数参数值
     *
     * @param key 参数名称
     * @param def 默认值
     * @return 参数值
     */
    Long getPositive(String key, Long def);

    /**
     * 获取长整型正整数参数值
     *
     * @param key       参数名称
     * @param candidate 候选参数
     * @param def       默认值
     * @return 参数值
     */
    Long getPositive(String key, String candidate, Long def);

    /**
     * 获取长整型正整数参数值
     *
     * @param option 选项
     * @return
     */
    Long getPositiveLong(URLOption<Long> option);

    /**
     * 获取长整型正整数参数值
     *
     * @param option 选项
     * @return
     */
    Long getPositiveLong(URLBiOption<Long> option);

    /**
     * 获取整型正整数参数值
     *
     * @param key 参数名称
     * @param def 默认值
     * @return 参数值
     */
    Integer getPositive(String key, Integer def);

    /**
     * 获取整型正整数参数值
     *
     * @param key       参数名称
     * @param candidate 候选参数
     * @param def       默认值
     * @return 参数值
     */
    Integer getPositive(String key, String candidate, Integer def);

    /**
     * 获取整型正整数参数值
     *
     * @param option 选项
     * @return
     */
    Integer getPositiveInt(URLOption<Integer> option);

    /**
     * 获取整型正整数参数值
     *
     * @param option 选项
     * @return
     */
    Integer getPositiveInt(URLBiOption<Integer> option);

    /**
     * 获取短整型正整数参数值
     *
     * @param key 参数名称
     * @param def 默认值
     * @return 参数值
     */
    Short getPositive(String key, Short def);

    /**
     * 获取短整型正整数参数值
     *
     * @param key       参数名称
     * @param candidate 候选参数
     * @param def       默认值
     * @return 参数值
     */
    Short getPositive(String key, String candidate, Short def);

    /**
     * 获取短整型正整数参数值
     *
     * @param option 选项
     * @return
     */
    Short getPositiveShort(URLOption<Short> option);

    /**
     * 获取短整型正整数参数值
     *
     * @param option 选项
     * @return
     */
    Short getPositiveShort(URLBiOption<Short> option);

    /**
     * 获取字节正整数参数值
     *
     * @param key 参数名称
     * @param def 默认值
     * @return 参数值
     */
    Byte getPositive(String key, Byte def);

    /**
     * 获取字节正整数参数值
     *
     * @param key       参数名称
     * @param candidate 候选参数
     * @param def       默认值
     * @return 参数值
     */
    Byte getPositive(String key, String candidate, Byte def);

    /**
     * 获取字节型正整数参数值
     *
     * @param option 选项
     * @return
     */
    Byte getPositiveByte(URLOption<Byte> option);

    /**
     * 获取字节型正整数参数值
     *
     * @param option 选项
     * @return
     */
    Byte getPositiveByte(URLBiOption<Byte> option);

    /**
     * 迭代消费
     *
     * @param consumer
     */
    void foreach(BiConsumer<String, Object> consumer);

}
