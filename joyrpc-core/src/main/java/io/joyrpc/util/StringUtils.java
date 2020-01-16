/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.joyrpc.util;

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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Title: 字符串操作工具类<br>
 */
public class StringUtils {

    /**
     * 按照逗号分号和空白字符分割
     */
    public static final Predicate<Character> SEMICOLON_COMMA_WHITESPACE = o -> {
        switch (o) {
            case ',':
            case ';':
                return true;
            default:
                return Character.isWhitespace(o);
        }
    };

    /**
     * The empty String {@code ""}.
     *
     * @since 2.0
     */
    public static final String EMPTY = "";

    // Empty checks
    //-----------------------------------------------------------------------

    /**
     * <p>Checks if a CharSequence is empty ("") or null.</p>
     *
     * <pre>
     * StringUtils.isEmpty(null)      = true
     * StringUtils.isEmpty("")        = true
     * StringUtils.isEmpty(" ")       = false
     * StringUtils.isEmpty("bob")     = false
     * StringUtils.isEmpty("  bob  ") = false
     * </pre>
     *
     * <p>NOTE: This method changed in Lang version 2.0.
     * It no longer trims the CharSequence.
     * That functionality is available in isBlank().</p>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     * @since 3.0 Changed signature from isEmpty(String) to isEmpty(CharSequence)
     */
    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * <p>Checks if a CharSequence is not empty ("") and not null.</p>
     *
     * <pre>
     * StringUtils.isNotEmpty(null)      = false
     * StringUtils.isNotEmpty("")        = false
     * StringUtils.isNotEmpty(" ")       = true
     * StringUtils.isNotEmpty("bob")     = true
     * StringUtils.isNotEmpty("  bob  ") = true
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is not empty and not null
     * @since 3.0 Changed signature from isNotEmpty(String) to isNotEmpty(CharSequence)
     */
    public static boolean isNotEmpty(CharSequence cs) {
        return !isEmpty(cs);
    }

    /**
     * <p>Checks if a CharSequence is whitespace, empty ("") or null.</p>
     *
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace
     * @since 2.0
     * @since 3.0 Changed signature from isBlank(String) to isBlank(CharSequence)
     */
    public static boolean isBlank(CharSequence cs) {
        int len;
        if (cs == null || (len = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < len; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Checks if a CharSequence is not empty (""), not null and not whitespace only.</p>
     *
     * <pre>
     * StringUtils.isNotBlank(null)      = false
     * StringUtils.isNotBlank("")        = false
     * StringUtils.isNotBlank(" ")       = false
     * StringUtils.isNotBlank("bob")     = true
     * StringUtils.isNotBlank("  bob  ") = true
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is
     * not empty and not null and not whitespace
     * @since 2.0
     * @since 3.0 Changed signature from isNotBlank(String) to isNotBlank(CharSequence)
     */
    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    /**
     * <pre>
     * StringUtils.trim(null)          = null
     * StringUtils.trim("")            = ""
     * StringUtils.trim("     ")       = ""
     * StringUtils.trim("abc")         = "abc"
     * StringUtils.trim("    abc    ") = "abc"
     * </pre>
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed string, {@code null} if null String input
     */
    public static String trim(final String str) {
        return str == null ? null : str.trim();
    }

    /**
     * <pre>
     * StringUtils.trimToNull(null)          = null
     * StringUtils.trimToNull("")            = null
     * StringUtils.trimToNull("     ")       = null
     * StringUtils.trimToNull("abc")         = "abc"
     * StringUtils.trimToNull("    abc    ") = "abc"
     * </pre>
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed String,
     * {@code null} if only chars &lt;= 32, empty or null String input
     * @since 2.0
     */
    public static String trimToNull(final String str) {
        String ts = trim(str);
        return isEmpty(ts) ? null : ts;
    }

    /**
     * <pre>
     * StringUtils.trimToEmpty(null)          = ""
     * StringUtils.trimToEmpty("")            = ""
     * StringUtils.trimToEmpty("     ")       = ""
     * StringUtils.trimToEmpty("abc")         = "abc"
     * StringUtils.trimToEmpty("    abc    ") = "abc"
     * </pre>
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed String, or an empty String if {@code null} input
     * @since 2.0
     */
    public static String trimToEmpty(final String str) {
        return str == null ? EMPTY : str.trim();
    }

    /**
     * 分割字符
     *
     * @param source
     * @param ch
     * @return
     */
    public static String[] split(final String source, final char ch) {
        return split(source, o -> o.charValue() == ch);
    }

    /**
     * 分割字符
     *
     * @param source
     * @param predicate
     * @return
     */
    public static String[] split(final String source, final Predicate<Character> predicate) {
        if (source == null) {
            return null;
        }
        int start = -1;
        int end = -1;
        char ch;
        LinkedList<String> parts = new LinkedList<>();
        int length = source.length();

        //遍历字符
        for (int i = 0; i < length; i++) {
            ch = source.charAt(i);
            //满足分割符号
            if (predicate.test(ch)) {
                //前面有字符片段
                if (start >= 0) {
                    parts.add(source.substring(start, end + 1));
                    start = -1;
                    end = -1;
                }
            } else {
                if (start == -1) {
                    start = i;
                }
                end = i;
            }
        }
        if (start >= 0) {
            parts.add(source.substring(start, length));
        }
        if (parts.isEmpty()) {
            return new String[0];
        }
        return parts.toArray(new String[parts.size()]);
    }

    /**
     * 按照字符串分割
     *
     * @param value
     * @param delimiter
     * @return
     */
    public static String[] split(final String value, final String delimiter) {
        if (delimiter == null || delimiter.isEmpty()) {
            return split(value, ',');
        } else if (delimiter.length() == 1) {
            return split(value, delimiter.charAt(0));
        }
        List<String> parts = new LinkedList<String>();
        int length = value.length();
        int maxPos = delimiter.length() - 1;
        int start = 0;
        int pos = 0;
        int end = 0;
        for (int i = 0; i < length; i++) {
            if (value.charAt(i) == delimiter.charAt(pos)) {
                if (pos++ == maxPos) {
                    if (end > start) {
                        parts.add(value.substring(start, end + 1));
                    }
                    pos = 0;
                    start = i + 1;
                }
            } else {
                end = i;
            }
        }
        if (start < length) {
            parts.add(value.substring(start, length));
        }
        if (parts.isEmpty()) {
            return new String[0];
        }
        return parts.toArray(new String[parts.size()]);
    }

    /**
     * 连接字符串数组
     *
     * @param strings   字符串数组
     * @param separator 分隔符
     * @return 按分隔符分隔的字符串
     */
    public static String join(String[] strings, String separator) {
        if (strings == null || strings.length == 0) {
            return EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        for (String string : strings) {
            if (isNotBlank(string)) {
                sb.append(string).append(separator);
            }
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - separator.length()) : StringUtils.EMPTY;
    }

    /**
     * 对象转string
     *
     * @param o 对象
     * @return 不为null执行toString方法
     */
    public static String toNullString(Object o) {
        return o != null ? o.toString() : null;
    }

    /**
     * 返回堆栈信息（e.printStackTrace()的内容）
     *
     * @param e Throwable
     * @return 异常堆栈信息
     */
    public static String toString(final Throwable e) {
        if (e == null) {
            return null;
        }
        StringWriter writer = new StringWriter(1024);
        PrintWriter pw = new PrintWriter(writer);
        e.printStackTrace(pw);
        pw.flush();
        return writer.toString();
    }

    public static String toSimpleString(final Throwable e) {
        if (e == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        Throwable err = null;
        do {
            if (err == null) {
                err = e;
            } else {
                err = err.getCause();
                sb.append(". Caused by ");
            }
            sb.append(err.getClass().getName()).append(":").append(err.getMessage());
        } while (err.getCause() != null);
        return sb.toString();
    }

    /**
     * String 数组转为Map
     *
     * @param pairs
     * @return
     */
    public static Map<String, String> toMap(final String... pairs) {
        if (pairs != null && pairs.length > 0 && pairs.length % 2 != 0) {
            throw new IllegalArgumentException("the parameters must be paired.");
        }
        Map<String, String> parameters = new HashMap<String, String>();
        if (pairs != null && pairs.length > 0) {
            for (int i = 0; i < pairs.length; i = i + 2) {
                parameters.put(pairs[i], pairs[i + 1]);
            }
        }
        return parameters;
    }

}
