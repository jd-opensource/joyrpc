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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;

import static io.joyrpc.util.ClassUtils.getCurrentClassLoader;

/**
 * Properties工具类，加快序列化操作
 */
public class PropertiesUtils {

    /**
     * Convert a nibble to a hex character
     *
     * @param nibble the nibble to convert.
     */
    protected static char toHex(int nibble) {
        return hexDigit[(nibble & 0xF)];
    }

    /**
     * A table of hex digits
     */
    protected static final char[] hexDigit = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    protected static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * 按照Properties格式存储
     * Converts unicodes to encoded &#92;uxxxx and escapes
     * special characters with a preceding slash
     *
     * @param builder
     * @param value
     * @param escapeSpace
     * @param escapeUnicode
     * @return
     */
    public static StringBuilder store(final StringBuilder builder, final String value,
                                      final boolean escapeSpace, final boolean escapeUnicode) {
        if (value == null) {
            return builder;
        }
        int len = value.length();
        char aChar;
        for (int x = 0; x < len; x++) {
            aChar = value.charAt(x);
            // Handle common case first, selecting largest block that
            // avoids the specials below
            if ((aChar > 61) && (aChar < 127)) {
                if (aChar == '\\') {
                    builder.append('\\');
                    builder.append('\\');
                    continue;
                }
                builder.append(aChar);
                continue;
            }
            switch (aChar) {
                case ' ':
                    if (x == 0 || escapeSpace) {
                        builder.append('\\');
                    }
                    builder.append(' ');
                    break;
                case '\t':
                    builder.append('\\');
                    builder.append('t');
                    break;
                case '\n':
                    builder.append('\\');
                    builder.append('n');
                    break;
                case '\r':
                    builder.append('\\');
                    builder.append('r');
                    break;
                case '\f':
                    builder.append('\\');
                    builder.append('f');
                    break;
                case '=': // Fall through
                case ':': // Fall through
                case '#': // Fall through
                case '!':
                    builder.append('\\');
                    builder.append(aChar);
                    break;
                default:
                    if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode) {
                        builder.append('\\');
                        builder.append('u');
                        builder.append(toHex((aChar >> 12) & 0xF));
                        builder.append(toHex((aChar >> 8) & 0xF));
                        builder.append(toHex((aChar >> 4) & 0xF));
                        builder.append(toHex(aChar & 0xF));
                    } else {
                        builder.append(aChar);
                    }
            }
        }
        return builder;
    }

    /**
     * 按照Properties格式存储字符串
     *
     * @param builder
     * @param properties
     * @return
     */
    public static StringBuilder store(final StringBuilder builder, final Properties properties) {
        if (builder != null && properties != null) {
            int count = 0;
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                if (count++ > 0) {
                    builder.append(LINE_SEPARATOR);
                }
                store(builder, (String) entry.getKey(), true, false).append('=');
                store(builder, (String) entry.getValue(), false, false);
            }
        }
        return builder;
    }

    /**
     * 按照Properties格式输出字符串
     *
     * @param properties
     * @return
     */
    public static String toString(final Properties properties) {
        if (properties == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(8192);
        store(builder, properties);
        return builder.toString();
    }

    /**
     * 读取资源文件
     *
     * @param resource
     * @throws IOException
     */
    public static Properties read(final String resource) throws IOException {
        if (resource != null && !resource.isEmpty()) {
            try (InputStream application = getCurrentClassLoader().getResourceAsStream(resource)) {
                if (application != null) {
                    Properties p = new Properties();
                    p.load(application);
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * 读取资源文件
     *
     * @param resource
     * @param consumer
     * @throws IOException
     */
    public static void read(final String resource, final BiConsumer<String, String> consumer) throws IOException {
        if (consumer != null) {
            Properties p = read(resource);
            if(null == p) return;
            for (Map.Entry<Object, Object> entry : p.entrySet()) {
                consumer.accept((String) entry.getKey(), (String) entry.getValue());
            }
        }
    }
}
