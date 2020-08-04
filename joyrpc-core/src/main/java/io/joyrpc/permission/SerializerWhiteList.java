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

import io.joyrpc.util.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.joyrpc.constants.Constants.DEFAULT_SERIALIZER_WHITELIST_ENABLED;
import static io.joyrpc.constants.Constants.SERIALIZER_WHITELIST_ENABLED;
import static io.joyrpc.context.Variable.VARIABLE;
import static io.joyrpc.util.ClassUtils.*;
import static io.joyrpc.util.GenericChecker.NONE_STATIC_FINAL_TRANSIENT_FIELD;
import static io.joyrpc.util.GenericChecker.NONE_STATIC_METHOD;

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
        whites.putIfAbsent(Collections.unmodifiableCollection(new ArrayList<>(0)).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.unmodifiableList(new ArrayList(0)).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.unmodifiableSet(new HashSet<>(0)).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.unmodifiableSortedSet(new TreeSet<>()).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.unmodifiableNavigableSet(new TreeSet<>()).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.unmodifiableList(new LinkedList<>()).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.unmodifiableSortedMap(new TreeMap<>()).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.unmodifiableNavigableMap(new TreeMap<>()).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.synchronizedCollection(new ArrayList<>(0)).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.synchronizedSet(new HashSet<>(0)).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.synchronizedSortedSet(new TreeSet<>()).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.synchronizedNavigableSet(new TreeSet<>()).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.synchronizedList(new ArrayList<>(0)).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.synchronizedList(new LinkedList<>()).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.synchronizedSortedMap(new TreeMap<>()).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.synchronizedNavigableMap(new TreeMap<>()).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.checkedCollection(new ArrayList<>(), Object.class).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.checkedQueue(new LinkedList<>(), Object.class).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.checkedSet(new HashSet<>(), Object.class).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.checkedSortedSet(new TreeSet<>(), Object.class).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.checkedNavigableSet(new TreeSet<>(), Object.class).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.checkedList(new ArrayList<>(), Object.class).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.checkedList(new LinkedList<>(), Object.class).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.checkedSortedMap(new TreeMap<>(), Object.class, Object.class).getClass(), Boolean.TRUE);
        whites.putIfAbsent(Collections.checkedNavigableMap(new TreeMap<>(), Object.class, Object.class).getClass(), Boolean.TRUE);
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
    }

    /**
     * 全局白名单获取器
     *
     * @return
     */
    public static SerializerWhiteListGetter getGlobalWhitelistGetter() {
        return GlobalSerializerWhiteListGetter.GLOBAL_WHITELIST_GETTER;
    }

    /**
     * 全局白名单获取器
     */
    protected static class GlobalSerializerWhiteListGetter {
        protected static final SerializerWhiteListGetter GLOBAL_WHITELIST_GETTER = new SerializerWhiteListGetter();
    }

    /**
     * 白名单获取器
     */
    public static class SerializerWhiteListGetter {

        /**
         * 唯一
         */
        protected Set<Type> uniques = new HashSet<>();

        /**
         * 处理接口类的白名单
         *
         * @param genericClass
         * @param whiteList
         */
        public void handleGenericClass(GenericClass genericClass, Set<String> whiteList) {
            Class clazz = genericClass.getClazz();
            List<Method> methods = getPublicMethod(clazz);
            methods.forEach(method -> {
                if (NONE_STATIC_METHOD.test(method)) {
                    handleGenericMethod(genericClass.get(method), whiteList);
                }
            });
        }

        /**
         * 处理方法的入参与返回值的白名单
         *
         * @param genericMethod
         * @param whiteList
         */
        public void handleGenericMethod(GenericMethod genericMethod, Set<String> whiteList) {
            //处理入参
            Type[] paramTypes = genericMethod.getMethod().getGenericParameterTypes();
            if (paramTypes != null) {
                GenericType[] genericParamTypes = genericMethod.getParameters();
                for (int i = 0; i < paramTypes.length; i++) {
                    handleGenericType(genericParamTypes[i], paramTypes[i], whiteList);
                }
            }
            //处理返回值
            handleGenericType(genericMethod.getReturnType(), genericMethod.getMethod().getGenericReturnType(), whiteList);
            //处理异常类
            Type[] exceptionTypes = genericMethod.getMethod().getGenericExceptionTypes();
            if (exceptionTypes != null) {
                GenericType[] genericExceptionTypes = genericMethod.getExceptions();
                for (int i = 0; i < exceptionTypes.length; i++) {
                    handleGenericType(genericExceptionTypes[i], paramTypes[i], whiteList);
                }
            }
        }

        /**
         * 处理 genericType 的白名单
         *
         * @param genericType
         * @param type
         * @param whiteList
         */
        public void handleGenericType(GenericType genericType, Type type, Set<String> whiteList) {
            if (!uniques.add(type)) {
                //已经处理过
                return;
            } else if (type instanceof Class) {
                handleClass((Class) type, whiteList);
            } else if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                //rawType
                Type rawType = parameterizedType.getRawType();
                handleGenericType(genericType, rawType, whiteList);
                //actualTypeArguments
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments != null && actualTypeArguments.length > 0) {
                    for (Type actualTypeArgument : actualTypeArguments) {
                        handleGenericType(genericType, actualTypeArgument, whiteList);
                    }
                }
            } else if (type instanceof TypeVariable) {
                //变量
                GenericType.Variable variable = genericType.getVariable(((TypeVariable) type).getName());
                handleGenericType(genericType, variable.getGenericType(), whiteList);
            } else if (type instanceof GenericArrayType) {
                //泛型数组
                handleGenericType(genericType, ((GenericArrayType) type).getGenericComponentType(), whiteList);
            } else if (type instanceof WildcardType) {

            }
        }

        /**
         * 处理 class 的白名单
         *
         * @param clazz
         * @param whiteList
         */
        public void handleClass(Class clazz, Set<String> whiteList) {
            //如果是数组，获取真正的class
            Class cl = clazz.isArray() ? clazz.getComponentType() : clazz;
            //加入白名单
            whiteList.add(cl.getName());
            //非基础类型，处理字段
            if (!isPrimitive(cl, null)) {
                GenericClass genericClass = getGenericClass(cl);
                //逐一处理字段
                List<Field> fields = getFields(genericClass.getClazz());
                for (Field field : fields) {
                    if (NONE_STATIC_FINAL_TRANSIENT_FIELD.test(field)) {
                        handleGenericType(genericClass.get(field), field.getGenericType(), whiteList);
                    }
                }
            }
        }

    }

}
