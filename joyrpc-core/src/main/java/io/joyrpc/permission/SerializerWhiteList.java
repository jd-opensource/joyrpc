package io.joyrpc.permission;

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

import io.joyrpc.util.ClassUtils;
import io.joyrpc.util.Resource;

import java.lang.reflect.InvocationTargetException;
import java.time.Year;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.joyrpc.constants.Constants.DEFAULT_SERIALIZER_WHITELIST_ENABLED;
import static io.joyrpc.constants.Constants.SERIALIZER_WHITELIST_ENABLED;
import static io.joyrpc.context.Variable.VARIABLE;

/**
 * 序列化白名单，处理安全漏洞
 */
public class SerializerWhiteList implements WhiteList<Class<?>>, WhiteList.WhiteListAware {

    /**
     * 是否启用
     */
    protected boolean enabled;

    /**
     * 白名单
     */
    protected Map<Class<?>, Boolean> whites = new ConcurrentHashMap<>();

    /**
     * 白名单文件
     */
    protected String[] whiteListFiles;

    public SerializerWhiteList(String... whiteListFiles) {
        this.enabled = VARIABLE.getBoolean(SERIALIZER_WHITELIST_ENABLED, DEFAULT_SERIALIZER_WHITELIST_ENABLED);
        this.whiteListFiles = whiteListFiles;
        if (whiteListFiles != null) {
            updateWhite(Resource.lines(whiteListFiles, true, true));
        }
    }

    /**
     * 设置白名单是否开启
     *
     * @param enabled 是否开启
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isWhite(Class<?> target) {
        if (!enabled) {
            return true;
        }
        if (!whites.containsKey(target)) {
            if (Throwable.class.isAssignableFrom(target)) {
                //异常
                whites.putIfAbsent(target, Boolean.TRUE);
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public synchronized void updateWhite(final Collection<String> targets) {
        if (targets != null) {
            targets.forEach(target -> {
                try {
                    whites.putIfAbsent(ClassUtils.forName(target), Boolean.TRUE);
                } catch (Throwable e) {
                }
            });
        }
    }

    /**
     * 添加白名单
     *
     * @param targets 白名单列表
     */
    public void addWhite(final Collection<Class<?>> targets) {
        if (targets != null) {
            targets.forEach(target -> whites.putIfAbsent(target, Boolean.TRUE));
        }
    }

    /**
     * 添加全局白名单
     *
     * @param targets 白名单列表
     */
    public static void addGlobalWhite(final Collection<Class<?>> targets) {
        getGlobalWhitelist().addWhite(targets);
    }

    /**
     * 全局配置的序列化白名单
     *
     * @return 全局配置的序列化白名单
     */
    public static SerializerWhiteList getGlobalWhitelist() {
        return GlobalSerializerWhiteList.GLOBAL_WHITELIST;
    }

    /**
     * 全局序列化白名单
     */
    protected static class GlobalSerializerWhiteList {
        /**
         * 全局的白名单
         */
        protected static final SerializerWhiteList GLOBAL_WHITELIST = new SerializerWhiteList(
                "META-INF/system_serialization_type", "user_serialization_type");

        static {
            GLOBAL_WHITELIST.whites.put(Collections.unmodifiableCollection(new ArrayList<>(0)).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.unmodifiableList(new ArrayList<>(0)).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.unmodifiableList(new LinkedList<>()).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.unmodifiableMap(new HashMap<>(0)).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.unmodifiableNavigableMap(new TreeMap<>()).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.unmodifiableNavigableSet(new TreeSet<>()).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.unmodifiableSortedMap(new TreeMap<>()).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.unmodifiableSortedSet(new TreeSet<>()).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.unmodifiableSet(new HashSet<>()).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.synchronizedCollection(new ArrayList<>(0)).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.synchronizedList(new ArrayList<>(0)).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.synchronizedList(new LinkedList<>()).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.synchronizedMap(new HashMap<>(0)).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.synchronizedNavigableMap(new TreeMap<>()).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.synchronizedNavigableSet(new TreeSet<>()).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.synchronizedSet(new HashSet<>()).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.synchronizedSortedMap(new TreeMap<>()).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.synchronizedSortedSet(new TreeSet<>()).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.checkedCollection(new ArrayList<>(0), Integer.class).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.checkedList(new ArrayList<>(0), Integer.class).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.checkedList(new LinkedList<>(), Integer.class).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.checkedMap(new HashMap<>(0), String.class, String.class).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.checkedNavigableMap(new TreeMap<>(), String.class, String.class).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.checkedNavigableSet(new TreeSet<>(), String.class).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.checkedSet(new HashSet<>(), String.class).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.checkedSortedMap(new TreeMap<>(), String.class, String.class).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.checkedSortedSet(new TreeSet<>(), String.class).getClass(), Boolean.TRUE);
            GLOBAL_WHITELIST.whites.put(Collections.checkedQueue(new LinkedList<>(), String.class).getClass(), Boolean.TRUE);
            try {
                GLOBAL_WHITELIST.whites.put(Year.class.getMethod("writeReplace").invoke(Year.of(2000)).getClass(), Boolean.TRUE);
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            } catch (NoSuchMethodException e) {
            }
        }
    }

}
