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

import io.joyrpc.constants.Version;
import io.joyrpc.extension.Converts;
import io.joyrpc.util.Resource;
import io.joyrpc.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.joyrpc.Plugin.CONTEXT_SUPPLIER;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;

/**
 * 全局参数
 */
public class GlobalContext {
    protected static final Logger logger = LoggerFactory.getLogger(GlobalContext.class);

    /**
     * 上下文信息，例如instancekey，本机ip等信息
     */
    protected static volatile Map<String, Object> context;

    protected static volatile Integer pid;

    /**
     * 接口配置map<接口名，<key,value>>，
     */
    protected final static Map<String, Map<String, String>> interfaceConfigs = new ConcurrentHashMap<>();
    protected static final Predicate<String> EXPRESSION = v -> v != null && v.startsWith("[") && v.endsWith("]");

    /**
     * 构造上下文
     *
     * @return
     */
    protected static Map<String, Object> getOrCreate() {
        if (context == null) {
            synchronized (GlobalContext.class) {
                if (context == null) {
                    //上下文变量识别插件
                    List<ContextSupplier> suppliers = new LinkedList<>();
                    CONTEXT_SUPPLIER.extensions().forEach(suppliers::add);
                    //变量识别函数
                    Function<String, Object> recognizer = key -> {
                        Object result = null;
                        for (ContextSupplier supplier : suppliers) {
                            result = supplier.recognize(key);
                            if (result != null) {
                                if (result instanceof CharSequence) {
                                    if (((CharSequence) result).length() > 0) {
                                        return result.toString();
                                    }
                                } else {
                                    return result;
                                }
                            }
                        }
                        return null;
                    };
                    Map<String, Object> target = new ConcurrentHashMap<>(100);
                    //允许用户在配置文件里面修改协议版本和名称
                    doPut(target, PROTOCOL_VERSION_KEY, Version.PROTOCOL_VERSION);
                    doPut(target, PROTOCOL_KEY, Version.PROTOCOL);
                    doPut(target, BUILD_VERSION_KEY, Version.BUILD_VERSION);
                    //读取系统内置的和用户的配置
                    loadConfig(new String[]{"META-INF/system_context", "user_context"}, target, recognizer);
                    //打印默认的上下文
                    if (logger.isInfoEnabled()) {
                        String line = System.getProperty("line.separator");
                        StringBuilder builder = new StringBuilder(1000).append("default context:").append(line);
                        target.forEach((k, v) -> builder.append("\t").append(k).append('=').append(v.toString()).append(line));
                        logger.info(builder.toString());
                    }
                    context = target;
                }
            }
        }
        return context;
    }

    /**
     * 加载配置
     *
     * @param resources
     * @param target
     * @param supplier
     */
    protected static void loadConfig(final String[] resources, final Map<String, Object> target, final Function<String, Object> supplier) {
        List<String> lines = Resource.lines(resources, true);
        for (String line : lines) {
            int pos = line.indexOf('=');
            String key = line;
            String value = null;
            Object result;
            if (pos >= 0) {
                key = line.substring(0, pos);
                value = line.substring(pos + 1);
            }
            boolean el = EXPRESSION.test(key);
            if (el) {
                key = key.substring(1, key.length() - 1);
            }
            if (value == null || value.isEmpty()) {
                if (el) {
                    result = supplier.apply(key);
                    if (result != null) {
                        target.put(key, result);
                    }
                }
            } else if (!el) {
                target.put(key, value);
            } else {
                String[] parts = StringUtils.split(value, SEMICOLON_COMMA_WHITESPACE);
                for (String part : parts) {
                    if (EXPRESSION.test(part)) {
                        part = part.substring(1, part.length() - 1);
                        result = supplier.apply(part);
                        if (result != null) {
                            target.put(key, result);
                            break;
                        }
                    } else {
                        target.put(key, part);
                        break;
                    }
                }
            }

        }
    }

    /**
     * 获取进程号
     *
     * @return
     */
    public static Integer getPid() {
        if (pid == null) {
            Object value = get(KEY_PID);
            if (value != null) {
                if (value instanceof Number) {
                    pid = ((Number) value).intValue();
                } else if (value instanceof CharSequence) {
                    pid = Integer.parseInt(value.toString());
                }
            }
        }
        return pid;
    }

    /**
     * 得到上下文信息
     *
     * @param key the key
     * @return the object
     */
    public static Object get(final String key) {
        return key == null ? null : getOrCreate().get(key);
    }

    /**
     * 得到上下文信息
     *
     * @param key the key
     * @return String
     */
    public static String getString(final String key) {
        return getString(key, null);
    }

    /**
     * 得到上下文信息
     *
     * @param key the key
     * @param def the def
     * @return String
     */
    public static String getString(final String key, final String def) {
        Object value = get(key);
        return value == null ? def : value.toString();
    }

    /**
     * 得到上下文信息
     *
     * @param key the key
     * @return Integer
     */
    public static Integer getInteger(final String key) {
        return getInteger(key, 0);
    }

    /**
     * 得到上下文信息
     *
     * @param key the key
     * @param def the def
     * @return Integer
     */
    public static Integer getInteger(final String key, final Integer def) {
        return Converts.getInteger(get(key), def);
    }

    /**
     * 得到上下文信息
     *
     * @param key the key
     * @return Long
     */
    public static Long getLong(final String key) {
        return getLong(key, 0L);
    }

    /**
     * 得到上下文信息
     *
     * @param key the key
     * @return Long
     */
    public static Boolean getBoolean(final String key) {
        return getBoolean(key, false);
    }

    /**
     * 得到上下文信息
     *
     * @param key the key
     * @param def the def
     * @return Long
     */
    public static Boolean getBoolean(final String key, final Boolean def) {
        return Converts.getBoolean(get(key), def);
    }

    /**
     * 得到上下文信息
     *
     * @param key the key
     * @return Long
     */
    public static Long getLong(final String key, final Long def) {
        return Converts.getLong(get(key), def);
    }

    /**
     * 设置上下文信息
     *
     * @param key   the key
     * @param value the value
     * @return the object
     */
    public static Object put(final String key, final Object value) {
        return doUpdate(getOrCreate(), key, value);
    }

    /**
     * 设置上下文
     *
     * @param context
     * @param key
     * @param value
     * @return
     */
    protected static Object doPut(final Map<String, Object> context, final String key, final Object value) {
        return value == null ? null : context.put(key, value);
    }

    /**
     * 设置上下文
     *
     * @param context
     * @param key
     * @param value
     * @return
     */
    protected static Object doUpdate(final Map<String, Object> context, final String key, final Object value) {
        return value == null ? context.remove(key) : context.put(key, value);
    }

    /**
     * 设置上下文信息
     *
     * @param key   the key
     * @param value the value
     * @return the object
     */
    public static Object putIfAbsent(final String key, final Object value) {
        return value != null ? getOrCreate().putIfAbsent(key, value) : null;
    }

    /**
     * 设置上下文信息
     *
     * @param key the key
     * @return the object
     */
    public static Object remove(final String key) {
        return key == null ? null : getOrCreate().remove(key);
    }

    /**
     * 上下文信息
     *
     * @return the context
     */
    public static Map<String, Object> getContext() {
        return getOrCreate();
    }

    /**
     * 获取接口参数
     *
     * @param interfaceId the interface id
     * @param key         the key
     * @param def         the default val
     * @return the interface val
     */
    public static String get(String interfaceId, String key, String def) {
        if (interfaceId == null || key == null) {
            return null;
        }
        Map<String, String> map = interfaceConfigs.get(interfaceId);
        return map == null ? def : map.getOrDefault(key, def);
    }

    /**
     * 设置接口参数
     *
     * @param interfaceId the interface id
     * @param key
     * @param value
     * @see GlobalContext#put(java.lang.String, java.util.Map)
     */
    @Deprecated
    public static void put(final String interfaceId, final String key, final String value) {
        if (interfaceId == null || key == null) {
            return;
        }
        Map<String, String> configs = interfaceConfigs.get(interfaceId);
        if (value != null) {
            configs = new HashMap<>(configs);
            configs.put(key, value);
            interfaceConfigs.put(interfaceId, configs);
        } else if (configs != null) {
            configs = new HashMap<>(configs);
            configs.remove(key);
            interfaceConfigs.put(interfaceId, configs);
        }
    }

    /**
     * 设置接口参数。
     *
     * @param interfaceId the interface id
     * @param configs     the config
     * @see
     */
    public static void put(final String interfaceId, final Map<String, String> configs) {
        if (interfaceId == null) {
            return;
        }
        if (configs == null) {
            interfaceConfigs.remove(interfaceId);
        } else {
            interfaceConfigs.put(interfaceId, Collections.unmodifiableMap(configs));
        }
    }

    /**
     * 得到全部接口下的全部参数
     *
     * @return the config map
     */
    public static Map<String, Map<String, String>> getInterfaceConfigs() {
        return Collections.unmodifiableMap(interfaceConfigs);
    }

    /**
     * 获取接口全部参数
     *
     * @param interfaceId the interface id
     * @return the config map
     */
    public static Map<String, String> getInterfaceConfig(String interfaceId) {
        return interfaceId == null ? null : interfaceConfigs.get(interfaceId);
    }

    /**
     * 获取全局动态配置
     *
     * @return 全局动态配置
     */
    public static Map<String, String> getGlobalSetting() {
        return getInterfaceConfig(GLOBAL_SETTING);
    }
}
