package io.joyrpc.config;

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

import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.exception.IllegalConfigureException;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.extension.ExtensionPoint;
import io.joyrpc.extension.URLBiOption;
import io.joyrpc.extension.URLOption;
import io.joyrpc.filter.Filter;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;

/**
 * 配置基类
 */
public abstract class AbstractConfig {

    /**
     * 默认IO通道高水位大小
     */
    public final static int WRITE_BUFFER_HIGH_WATER_MARK = 64 * 1024;
    /**
     * 默认IO通道低水位大小
     */
    public final static int WRITE_BUFFER_LOW_WATER_MARK = 32 * 1024;

    /**
     * 可用的字符串为：英文大小写，数字，横杆-，下划线_，点.
     * !@#$*,;:有特殊含义
     */
    public final static Pattern NORMAL = Pattern.compile("^[a-zA-Z0-9\\-\\_\\.]+$");

    /**
     * 可用的字符串为：英文大小写，数字，横杆-，下划线_，点. 逗号,
     * !@#$*;:有特殊含义
     */
    public final static Pattern NORMAL_COMMA = Pattern.compile("^[a-zA-Z0-9\\-\\_\\.,]+$");

    /**
     * 可用的字符串为：英文大小写，数字，横杆-，下划线_，点. 冒号:
     * !@#$*,;有特殊含义
     */
    public final static Pattern NORMAL_COLON = Pattern.compile("^[a-zA-Z0-9\\-\\_\\.:]+$");

    /**
     * 可用的字符串为：英文大小写，数字，横杆-，下划线_，点. 分号;
     * !@#$*,;有特殊含义
     */
    public final static Pattern NORMAL_SEMICOLON = Pattern.compile("^[a-zA-Z0-9\\-\\_\\.;]+$");

    /**
     * 可用的字符串为：英文大小写，数字，横杆-，下划线_，点. 逗号, 冒号:
     * !@#$*,;有特殊含义
     */
    public final static Pattern NORMAL_COMMA_COLON = Pattern.compile("^[a-zA-Z0-9\\-\\_\\.,:]+$");

    /**
     * 可用的字符串为：英文大小写，数字，横杆-，下划线_，点. 分号; 冒号:
     * !@#$*,;有特殊含义
     */
    public final static Pattern NORMAL_SEMICOLON_COLON = Pattern.compile("^[a-zA-Z0-9\\-\\_\\.;:]+$");

    public AbstractConfig() {
    }

    public AbstractConfig(AbstractConfig config) {

    }

    /**
     * 匹配正常字符串
     *
     * @param configValue 配置项
     * @return 是否匹配，否表示有其他字符
     */
    protected boolean match(Pattern pattern, String configValue) {
        return pattern.matcher(configValue).find();
    }

    /**
     * 检查字符串是否是正常值，不是则抛出异常
     *
     * @param configKey   配置项
     * @param configValue 配置值
     * @throws IllegalConfigureException 非法异常
     */
    protected void checkNormal(String configKey, String configValue) throws IllegalConfigureException {
        checkPattern(configKey, configValue, NORMAL, "only allow a-zA-Z0-9 '-' '_' '.'");
    }

    /**
     * 检查字符串是否是正常值（含逗号），不是则抛出异常
     *
     * @param configKey   配置项
     * @param configValue 配置值
     * @throws IllegalConfigureException 非法异常
     */
    protected void checkNormalWithComma(String configKey, String configValue) throws IllegalConfigureException {
        checkPattern(configKey, configValue, NORMAL_COMMA, "only allow a-zA-Z0-9 '-' '_' '.' ','");
    }

    /**
     * 检查字符串是否是正常值（含冒号），不是则抛出异常
     *
     * @param configKey   配置项
     * @param configValue 配置值
     * @throws IllegalConfigureException 非法异常
     */
    protected void checkNormalWithColon(String configKey, String configValue) throws IllegalConfigureException {
        checkPattern(configKey, configValue, NORMAL_COLON, "only allow a-zA-Z0-9 '-' '_' '.' ':'");
    }

    /**
     * 检查字符串是否是正常值（含冒号），不是则抛出异常
     *
     * @param configKey   配置项
     * @param configValue 配置值
     * @throws IllegalConfigureException 非法异常
     */
    protected void checkNormalWithCommaColon(String configKey, String configValue) throws IllegalConfigureException {
        checkPattern(configKey, configValue, NORMAL_COMMA_COLON, "only allow a-zA-Z0-9 '-' '_' '.' ':' ','");
    }

    /**
     * 根据正则表达式检查字符串是否是正常值（含冒号），不是则抛出异常
     *
     * @param configKey   配置项
     * @param configValue 配置值
     * @param pattern     正则表达式
     * @param message     消息
     * @throws IllegalConfigureException
     */
    protected void checkPattern(String configKey, String configValue, Pattern pattern, String message)
            throws IllegalConfigureException {
        if (configValue != null && !match(pattern, configValue)) {
            throw new IllegalConfigureException(configKey, configValue, message, ExceptionCode.COMMON_VALUE_ILLEGAL);
        }
    }

    /**
     * 检查数字是否为正整数（>0)
     *
     * @param configKey   配置项
     * @param configValue 配置值
     * @throws IllegalConfigureException 非法异常
     */
    protected void checkPositiveInteger(String configKey, Integer configValue) throws IllegalConfigureException {
        if (configValue != null && configValue <= 0) {
            throw new IllegalConfigureException(configKey, configValue + "", "must > 0", ExceptionCode.COMMON_VALUE_ILLEGAL);
        }
    }

    /**
     * 验证水位
     *
     * @param bufferHighWaterMark
     * @param bufferLowWaterMark
     */
    protected void checkBufferMarkValid(Integer bufferHighWaterMark, Integer bufferLowWaterMark) {

        bufferHighWaterMark = bufferHighWaterMark == null ? WRITE_BUFFER_HIGH_WATER_MARK : bufferHighWaterMark;
        bufferLowWaterMark = bufferLowWaterMark == null ? WRITE_BUFFER_LOW_WATER_MARK : bufferLowWaterMark;
        // 低水位设置不能小于默认的高水位值,且水位值必须在合法范围内
        int lowestWaterMark = WRITE_BUFFER_LOW_WATER_MARK >> 2;
        if (bufferLowWaterMark > bufferHighWaterMark
                || bufferHighWaterMark < WRITE_BUFFER_LOW_WATER_MARK
                || bufferLowWaterMark < lowestWaterMark) {
            throw new IllegalConfigureException(String.format("highWaterMark cannot be less than lowWaterMark. highWaterMark must be greater than %d which is default lowWaterMark," +
                            " and lowWaterMark must be greater than %d. current highWaterMark:%d and current lowWaterMark:%d",
                    WRITE_BUFFER_LOW_WATER_MARK, lowestWaterMark, bufferHighWaterMark, bufferLowWaterMark), ExceptionCode.COMMON_VALUE_ILLEGAL);
        }
    }

    /**
     * 检查数字是否为非负数（>=0)
     *
     * @param configKey   配置项checkBufferMarkValid
     * @param configValue 配置值
     * @throws IllegalConfigureException 非法异常
     */
    protected void checkNatureInteger(String configKey, Integer configValue) throws IllegalConfigureException {
        if (configValue != null && configValue < 0) {
            throw new IllegalConfigureException(configKey, configValue + "", "must >= 0", ExceptionCode.COMMON_VALUE_ILLEGAL);
        }
    }

    /**
     * 检查插件是否存着
     *
     * @param extensionPoint
     * @param type
     * @param property
     * @param value
     * @param <T>
     * @return
     */
    protected static <T> T checkExtension(final ExtensionPoint<T, String> extensionPoint, final Class<T> type, final String property, final String value) {
        if (value != null && !value.isEmpty()) {
            T v = extensionPoint.get(value);
            if (v == null) {
                throw new IllegalConfigureException("No such extension " + value + " for " + property + "@" + type.getName(), ExceptionCode.COMMON_PLUGIN_ILLEGAL);
            }
            return v;
        }
        return null;
    }

    /**
     * 检查过滤器配置
     *
     * @param property
     * @param value
     * @param extension
     */
    protected void checkFilterConfig(String property, String value, ExtensionPoint<? extends Filter, String> extension) {
        if (value == null || value.isEmpty()) {
            return;
        }
        String[] values = split(value, SEMICOLON_COMMA_WHITESPACE);
        Filter filter;
        for (String v : values) {
            if (v.equals("-*") || v.equals("-default")) {
                continue;
            } else if (v.startsWith("-")) {
                v = v.substring(1);
            }
            filter = extension.get(v);
            if (null == filter) {
                throw new InitializationException("Value of \"filter\" " + v + " value is not found. config with key " + property + " !", ExceptionCode.FILTER_PLUGIN_NO_EXISTS);
            }
        }
    }

    protected void addElement2Map(final Map<String, String> dest, final String key, final Object value) {
        if (null != value) {
            String v = value.toString();
            if (v != null && !v.isEmpty()) {
                dest.put(key, value.toString());
            }
        }
    }

    protected void addElement2Map(final Map<String, String> dest, final URLOption option, final Object value) {
        addElement2Map(dest, option.getName(), value);
    }

    protected void addElement2Map(final Map<String, String> dest, final URLBiOption option, final Object value) {
        addElement2Map(dest, option.getName(), value);
    }

    protected Map<String, String> addAttribute2Map(Map<String, String> params) {
        return params;
    }

    protected Map<String, String> addAttribute2Map() {
        Map<String, String> result = new HashMap<>();
        addAttribute2Map(result);
        return result;
    }

    /**
     * 参数验证
     */
    protected void validate() {

    }

}
