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
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.joyrpc.Plugin.ENVIRONMENT;
import static io.joyrpc.cluster.Region.DATA_CENTER;
import static io.joyrpc.cluster.Region.REGION;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.util.PropertiesUtils.read;

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

    /**
     * 构造上下文
     *
     * @return
     */
    protected static Map<String, Object> getOrCreate() {
        if (context == null) {
            synchronized (GlobalContext.class) {
                if (context == null) {
                    //加载环境变量
                    Environment environment = ENVIRONMENT.get();
                    Collection<Property> properties = environment.properties();
                    Map<String, Object> map = new ConcurrentHashMap<>(Math.max(properties.size(), 200));
                    //允许用户在配置文件里面修改协议版本和名称
                    doPut(map, PROTOCOL_VERSION_KEY, Version.PROTOCOL_VERSION);
                    doPut(map, PROTOCOL_KEY, Version.PROTOCOL);
                    doPut(map, BUILD_VERSION_KEY, Version.BUILD_VERSION);
                    //优先读取系统内置的配置
                    loadResource(map, "META-INF/system_context.properties");
                    //环境变量覆盖
                    properties.forEach(o -> map.put(o.getKey(), o.getValue()));
                    //变量兼容
                    doPut(map, KEY_APPAPTH, map.get(Environment.APPLICATION_PATH));
                    doPut(map, KEY_APPID, map.get(Environment.APPLICATION_ID));
                    doPut(map, KEY_APPNAME, map.get(Environment.APPLICATION_NAME));
                    doPut(map, KEY_APPINSID, map.get(Environment.APPLICATION_INSTANCE));
                    //读取用户的配置
                    loadResource(map, environment.getString(CONTEXT_RESOURCE, "global_context.properties"));
                    //打印默认的上下文
                    if (logger.isInfoEnabled()) {
                        String line = System.getProperty("line.separator");
                        StringBuilder builder = new StringBuilder(1000).append("default context:").append(line);
                        map.forEach((k, v) -> builder.append("\t").append(k).append('=').append(v.toString()).append(line));
                        logger.info(builder.toString());
                    }
                    context = map;
                }
            }
        }
        return context;
    }

    /**
     * 从资源文件加载
     *
     * @param map
     * @param resource
     */
    protected static void loadResource(final Map<String, Object> map, final String resource) {
        try {
            read(resource, (k, v) -> doPut(map, k, v));
        } catch (Exception e) {
            logger.error("Error occurs while reading global config from " + resource, e);
        }
    }

    /**
     * 获取进程号
     *
     * @return
     */
    public static Integer getPid() {
        if (pid == null) {
            pid = ENVIRONMENT.get().getInteger(Environment.PID);
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
     * 重置region上下文信息
     *
     * @param region
     */
    public static void updateRegion(final String region) {
        if (region != null && !region.isEmpty()) {
            ENVIRONMENT.get().put(Environment.REGION, region);
            getOrCreate().put(REGION, region);
        }
    }

    /**
     * 重置dataCenter上下文信息
     *
     * @param dataCenter
     */
    public static void updateDataCenter(final String dataCenter) {
        if (dataCenter != null && !dataCenter.isEmpty()) {
            ENVIRONMENT.get().put(Environment.DATA_CENTER, dataCenter);
            getOrCreate().put(DATA_CENTER, dataCenter);
        }
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
     * 作为参数对象，便于获取值
     *
     * @return
     */
    public static Parametric asParametric() {
        return new MapParametric(getOrCreate());
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
     * 作为参数对象，便于获取值
     *
     * @return
     */
    public static Parametric asParametric(final String interfaceId) {
        return new MapParametric(getInterfaceConfig(interfaceId));
    }
}
