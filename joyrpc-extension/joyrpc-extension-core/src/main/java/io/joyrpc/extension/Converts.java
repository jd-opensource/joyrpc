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

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

/**
 * 参数转换
 */
public abstract class Converts {

    /**
     * 获取字符串，数组和集合以逗号分隔
     *
     * @param value 对象
     * @return
     */
    public static String getString(final Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof CharSequence) {
            return value.toString();
        } else if (value instanceof Collection) {
            StringBuilder builder = new StringBuilder();
            Collection<?> collection = (Collection<?>) value;
            int count = 0;
            for (Object item : collection) {
                if (count++ > 0) {
                    builder.append(',');
                }
                if (item != null) {
                    builder.append(item.toString());
                }
            }
            return builder.toString();
        } else if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            StringBuilder builder = new StringBuilder();
            Object item;
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                item = Array.get(value, i);
                if (item != null) {
                    builder.append(item.toString());
                }
            }
            return builder.toString();
        }
        return value.toString();

    }

    /**
     * 获取字符串参数值
     *
     * @param value 对象
     * @param def   默认值
     * @return 参数值
     */
    public static String getString(final Object value, final String def) {
        String text = getString(value);
        if (text == null || text.isEmpty()) {
            return def;
        }
        return text;
    }

    /**
     * 获取日期参数值，日期是从EPOCH的毫秒数
     *
     * @param value 对象
     * @param def   默认值
     * @return 参数值
     */
    public static Date getDate(final Object value, final Date def) {
        if (value == null) {
            return def;
        } else if (value instanceof Date) {
            return (Date) value;
        } else if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        } else if (value instanceof CharSequence) {
            String text = value.toString();
            try {
                return new Date((Long.parseLong(text)));
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    /**
     * 获取日期参数值，日期格式为字符串
     *
     * @param value  对象
     * @param format 日期格式
     * @return 参数值
     */
    public static Date getDate(final Object value, final SimpleDateFormat format) {
        return getDate(value, format, null);
    }

    /**
     * 获取日期参数值，日期格式为字符串
     *
     * @param value  对象
     * @param parser 日期转换
     * @param def    默认值
     * @return 参数值
     */
    public static Date getDate(final Object value, final DateParser parser, final Date def) {
        if (value == null) {
            return def;
        } else if (value instanceof Date) {
            return (Date) value;
        } else if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        } else if (parser == null) {
            return def;
        } else if (value instanceof CharSequence) {
            String text = value.toString();
            try {
                Date result = parser.parse(text);
                return result == null ? def : result;
            } catch (ParseException e) {
                return def;
            }
        }
        return def;
    }

    /**
     * 获取日期参数值，日期格式为字符串
     *
     * @param value  对象
     * @param format 日期格式
     * @param def    默认值
     * @return 参数值
     */
    public static Date getDate(final Object value, final SimpleDateFormat format, final Date def) {
        return getDate(value, new DateParser.SimpleDateParser(format), def);
    }

    /**
     * 获取日期参数
     *
     * @param value  对象
     * @param format 日期格式
     * @param def    默认值
     * @return
     */
    public static Date getDate(final Object value, final String format, final String def) {
        SimpleDateFormat sdf = format == null || format.isEmpty() ? null : new SimpleDateFormat(format);
        Date result = getDate(value, sdf);
        try {
            return result == null ? (sdf == null || def == null || def.isEmpty() ? null : sdf.parse(def)) : result;
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 获取单精度浮点数参数值
     *
     * @param value 对象
     * @return 参数值
     */
    public static Float getFloat(final Object value) {
        return getFloat(value, null);
    }

    /**
     * 获取单精度浮点数参数值
     *
     * @param value 对象
     * @param def   默认值
     * @return 参数值
     */
    public static Float getFloat(final Object value, final Float def) {
        if (value == null) {
            return def;
        } else if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else if (value instanceof CharSequence || value instanceof Character) {
            String text = value.toString();
            if (text == null || text.isEmpty()) {
                return def;
            }
            try {
                return Float.parseFloat(text);
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    /**
     * 获取双精度浮点数参数值
     *
     * @param value 对象
     * @return 参数值
     */
    public static Double getDouble(final Object value) {
        return getDouble(value, null);
    }

    /**
     * 获取双精度浮点数参数值
     *
     * @param value 对象
     * @param def   默认值
     * @return 参数值
     */
    public static Double getDouble(final Object value, final Double def) {
        if (value == null) {
            return def;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof CharSequence || value instanceof Character) {
            String text = value.toString();
            if (text == null || text.isEmpty()) {
                return def;
            }
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    /**
     * 获取长整形参数值
     *
     * @param value 对象
     * @return 参数值
     */
    public static Long getLong(final Object value) {
        return getLong(value, null);
    }

    /**
     * 获取长整形参数值
     *
     * @param value 对象
     * @param def   默认值
     * @return 参数值
     */
    public static Long getLong(final Object value, final Long def) {
        if (value == null) {
            return def;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof CharSequence || value instanceof Character) {
            String text = value.toString();
            if (text == null || text.isEmpty()) {
                return def;
            }
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    /**
     * 获取整形参数值
     *
     * @param value 对象
     * @return 参数值
     */
    public static Integer getInteger(final Object value) {
        return getInteger(value, null);
    }

    /**
     * 获取整形参数值
     *
     * @param value 对象
     * @param def   默认值
     * @return 参数值
     */
    public static Integer getInteger(final Object value, final Integer def) {
        if (value == null) {
            return def;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof CharSequence || value instanceof Character) {
            String text = value.toString();
            if (text == null || text.isEmpty()) {
                return def;
            }
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    /**
     * 获取短整形参数值
     *
     * @param value 对象
     * @return 参数值
     */
    public static Short getShort(final Object value) {
        return getShort(value, null);
    }

    /**
     * 获取短整形参数值
     *
     * @param value 对象
     * @param def   默认值
     * @return 参数值
     */
    public static Short getShort(final Object value, final Short def) {
        if (value == null) {
            return def;
        } else if (value instanceof Number) {
            return ((Number) value).shortValue();
        } else if (value instanceof CharSequence || value instanceof Character) {
            String text = value.toString();
            if (text == null || text.isEmpty()) {
                return def;
            }
            try {
                return Short.parseShort(text);
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    /**
     * 获取字节参数值
     *
     * @param value 对象
     * @return 参数值
     */
    public static Byte getByte(final Object value) {
        return getByte(value, null);
    }


    /**
     * 获取字节参数值
     *
     * @param value 对象
     * @param def   默认值
     * @return 参数值
     */
    public static Byte getByte(final Object value, final Byte def) {
        if (value == null) {
            return def;
        } else if (value instanceof Number) {
            return ((Number) value).byteValue();
        } else if (value instanceof CharSequence || value instanceof Character) {
            String text = value.toString();
            if (text == null || text.isEmpty()) {
                return def;
            }
            try {
                return Byte.parseByte(text);
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    /**
     * 获取布尔参数值
     *
     * @param value 对象
     * @return 参数值
     */
    public static Boolean getBoolean(final Object value) {
        return getBoolean(value, null);
    }

    /**
     * 获取布尔参数值
     *
     * @param value 对象
     * @param def   默认值
     * @return 参数值
     */
    public static Boolean getBoolean(final Object value, final Boolean def) {
        if (value == null) {
            return def;
        } else if (value instanceof Number) {
            return ((Number) value).longValue() != 0;
        } else if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Character) {
            return ((Character) value) != '0';
        } else if (value instanceof CharSequence) {
            String text = value.toString();
            if ("true".equalsIgnoreCase(text)) {
                return true;
            } else if ("false".equalsIgnoreCase(text)) {
                return false;
            }
            try {
                return Long.parseLong(text) != 0;
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    /**
     * 获取长整形自然数参数值
     *
     * @param value 对象
     * @param def   默认值
     * @return 参数值
     */
    public static Long getNatural(final Object value, final Long def) {
        Long result = getLong(value, null);
        return result == null || result < 0 ? def : result;
    }

    /**
     * 获取整形自然数参数值
     *
     * @param value 对象
     * @param def   默认值
     * @return 参数值
     */
    public static Integer getNatural(final Object value, final Integer def) {
        Integer result = getInteger(value, null);
        return result == null || result < 0 ? def : result;
    }

    /**
     * 获取短整形自然数参数值
     *
     * @param value 对象
     * @param def   默认值
     * @return 参数值
     */
    public static Short getNatural(final Object value, final Short def) {
        Short result = getShort(value, null);
        return result == null || result < 0 ? def : result;
    }

    /**
     * 获取字节自然数参数值
     *
     * @param value 对象
     * @param def   默认值
     * @return 参数值
     */
    public static Byte getNatural(final Object value, final Byte def) {
        Byte result = getByte(value, null);
        return result == null || result < 0 ? def : result;
    }

    /**
     * 获取长整形正整数参数值
     *
     * @param value 对象
     * @param def   默认值
     * @return 参数值
     */
    public static Long getPositive(final Object value, final Long def) {
        Long result = getLong(value, null);
        return result == null || result <= 0 ? def : result;
    }

    /**
     * 获取整形正整数参数值
     *
     * @param value 对象
     * @param def   默认值
     * @return 参数值
     */
    public static Integer getPositive(final Object value, final Integer def) {
        Integer result = getInteger(value, null);
        return result == null || result <= 0 ? def : result;
    }

    /**
     * 获取短整形正整数参数值
     *
     * @param value 对象
     * @param def   默认值
     * @return 参数值
     */
    public static Short getPositive(final Object value, final Short def) {
        Short result = getShort(value, null);
        return result == null || result <= 0 ? def : result;
    }

    /**
     * 获取字节正整数参数值
     *
     * @param value 对象
     * @param def   默认值
     * @return 参数值
     */
    public static Byte getPositive(final Object value, final Byte def) {
        Byte result = getByte(value, null);
        return result == null || result <= 0 ? def : result;
    }
}
