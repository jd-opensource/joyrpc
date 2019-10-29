package io.joyrpc.codec.serialization.generic;

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

import io.joyrpc.codec.serialization.GenericSerializer;
import io.joyrpc.exception.CodecException;
import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.util.ClassUtils;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Predicate;

import static io.joyrpc.util.ClassUtils.*;

/**
 * 默认泛型序列化器
 */
@Extension("standard")
public class StandardGenericSerializer implements GenericSerializer {

    /**
     * 普通时间的格式
     */
    protected static final DateTimeFormatter DATE_FORMAT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 判断是否是基本类型
     */
    protected static final Predicate<Class> PRIMITIVE = (o) -> {
        Class target = o;
        if (o.isArray()) { // 数组，检查数组类型
            target = o.getComponentType();
        }
        return target.isPrimitive() // 基本类型
                || Boolean.class.isAssignableFrom(target)
                || Character.class.isAssignableFrom(target)
                || Number.class.isAssignableFrom(target)
                || String.class.isAssignableFrom(target)
                || Date.class.isAssignableFrom(target);
    };

    @Override
    public Object serialize(final Object object) throws CodecException {
        return normalize(object);
    }

    @Override
    public Object[] deserialize(final Invocation invocation) throws CodecException {
        Object[] genericArgs = invocation.getArgs();
        Object[] paramArgs = genericArgs == null || genericArgs.length < 3 ? new Object[0] : (Object[]) genericArgs[2];
        return realize(paramArgs, invocation.getArgClasses(), invocation.getMethod().getGenericParameterTypes());
    }

    /**
     * 标准化
     *
     * @param pojo
     * @return
     */
    protected Object normalize(final Object pojo) throws CodecException {
        return normalize(pojo, new IdentityHashMap<>());
    }

    /**
     * 标准化
     *
     * @param pojo
     * @param history
     * @return
     */
    protected Object normalize(final Object pojo, final Map<Object, Object> history) throws CodecException {
        if (pojo == null) {
            return null;
        }
        Class<?> pojoClass = pojo.getClass();
        if (isPrimitive(pojoClass, PRIMITIVE)) {
            //float,double,boolean,short,int,long,char
            //String,Date,Character,Boolean,Number
            //获取数组元素是这些
            return pojo;
        } else if (pojoClass.isEnum()) {
            return ((Enum<?>) pojo).name();
        } else if (pojo instanceof Class) {
            return ((Class) pojo).getName();
        }
        Object o = history.get(pojo);
        if (o != null) {
            return o;
        }
        history.put(pojo, pojo);

        try {
            if (pojoClass.isArray()) {
                return normalizeArray(pojo, history);
            } else if (pojo instanceof Collection<?>) {
                return normalizeCollection((Collection<?>) pojo, history);
            } else if (pojo instanceof Map<?, ?>) {
                return normalizeMap((Map<?, ?>) pojo, history);
            } else {
                return normalizePojo(pojo, history);
            }
        } catch (CodecException e) {
            throw e;
        } catch (Exception e) {
            throw new CodecException("Error occurs while normalizing, caused by " + e.getMessage(), e);
        }
    }

    /**
     * 标准化POJO
     *
     * @param pojo
     * @param history
     * @return
     */
    protected Object normalizePojo(Object pojo, Map<Object, Object> history) throws InvocationTargetException, IllegalAccessException {
        Class<?> pojoClass = pojo.getClass();
        Map<String, Object> map = new HashMap<>();
        history.put(pojo, map);
        map.put("class", pojoClass.getName());
        for (Method method : getPublicMethod(pojoClass)) {
            if (isReader(method)) {
                map.put(getReaderProperty(method), normalize(method.invoke(pojo), history));
            }
        }
        // public field
        for (Field field : getFields(pojoClass)) {
            if (isPublicInstanceField(field)) {
                Object fieldValue = field.get(pojo);
                // public filed同时也有get/set方法，如果get/set存取的不是前面那个 public field 该如何处理
                if (history.containsKey(pojo)) {
                    Object pojoGenerilizedValue = history.get(pojo);
                    if (pojoGenerilizedValue instanceof Map
                            && ((Map) pojoGenerilizedValue).containsKey(field.getName())) {
                        continue;
                    }
                }
                if (fieldValue != null) {
                    map.put(field.getName(), normalize(fieldValue, history));
                }

            }
        }
        return map;
    }

    /**
     * 标准化MAP
     *
     * @param pojo
     * @param history
     * @return
     */
    protected Object normalizeMap(Map<?, ?> pojo, Map<Object, Object> history) {
        Map<Object, Object> dest = createMap(pojo.getClass());
        history.put(pojo, dest);
        for (Map.Entry<?, ?> obj : pojo.entrySet()) {
            dest.put(normalize(obj.getKey(), history), normalize(obj.getValue(), history));
        }
        return dest;
    }

    /**
     * 标准化集合
     *
     * @param pojo
     * @param history
     * @return
     */
    protected Object normalizeCollection(Collection<?> pojo, Map<Object, Object> history) {
        int len = pojo.size();
        Collection<Object> dest = (pojo instanceof List<?>) ? new ArrayList<>(len) : new HashSet<>(len);
        history.put(pojo, dest);
        for (Object obj : pojo) {
            dest.add(normalize(obj, history));
        }
        return dest;
    }

    /**
     * 标准化数组
     *
     * @param pojo
     * @param history
     * @return
     */
    protected Object normalizeArray(final Object pojo, final Map<Object, Object> history) {
        if (pojo.getClass().getComponentType().isEnum()) {
            return normalizeEnumArray(pojo, history);
        }
        int len = Array.getLength(pojo);
        Object[] dest = new Object[len];
        history.put(pojo, dest);
        for (int i = 0; i < len; i++) {
            dest[i] = normalize(Array.get(pojo, i), history);
        }
        return dest;
    }

    /**
     * 标准化枚举数组
     *
     * @param pojo
     * @param history
     * @return
     */
    protected Object normalizeEnumArray(final Object pojo, final Map<Object, Object> history) {
        int len = Array.getLength(pojo);
        String[] values = new String[len];
        history.put(pojo, values);
        for (int i = 0; i < len; i++) {
            values[i] = ((Enum<?>) Array.get(pojo, i)).name();
        }
        return values;
    }

    /**
     * 创建MAP
     *
     * @param clazz
     * @return
     */
    protected Map createMap(Class<?> clazz) {
        if (HashMap.class == clazz) {
            return new HashMap();
        } else if (Hashtable.class == clazz) {
            return new Hashtable();
        } else if (IdentityHashMap.class == clazz) {
            return new IdentityHashMap();
        } else if (LinkedHashMap.class == clazz) {
            return new LinkedHashMap();
        } else if (Properties.class == clazz) {
            return new Properties();
        } else if (TreeMap.class == clazz) {
            return new TreeMap();
        } else if (WeakHashMap.class == clazz) {
            return new WeakHashMap();
        } else if (ConcurrentHashMap.class == clazz) {
            return new ConcurrentHashMap();
        } else if (ConcurrentSkipListMap.class == clazz) {
            return new ConcurrentSkipListMap();
        } else {

            Map result = null;
            try {
                result = (Map) clazz.newInstance();
            } catch (Exception e) { /* ignore */ }

            if (result == null) {
                try {
                    Constructor<?> constructor = clazz.getConstructor(Map.class);
                    result = (Map) constructor.newInstance(Collections.EMPTY_MAP);
                } catch (Exception e) { /* ignore */ }
            }
            return result == null ? new HashMap<>() : result;
        }
    }

    /**
     * 反序列化
     *
     * @param objs   参数
     * @param types  参数类型
     * @param gtypes 参数泛化类型
     * @return
     */
    protected Object[] realize(final Object[] objs, final Class<?>[] types, final Type[] gtypes) {
        try {
            if (objs.length != types.length || objs.length != gtypes.length) {
                throw new CodecException("args.length != types.length");
            }
            Object[] result = new Object[objs.length];
            for (int i = 0; i < objs.length; i++) {
                result[i] = realize(objs[i], types[i], gtypes[i], new IdentityHashMap<>());
            }
            return result;
        } catch (CodecException e) {
            throw e;
        } catch (Exception e) {
            throw new CodecException("Error occurs while realizing, caused by " + e.getMessage(), e);
        }
    }

    /**
     * 反序列化
     *
     * @param pojo
     * @param type
     * @param genericType
     * @param history
     * @return
     */
    protected Object realize(final Object pojo, final Class<?> type, final Type genericType, final Map<Object, Object> history) {
        if (pojo == null) {
            return null;
        } else if (isPrimitive(pojo.getClass(), PRIMITIVE)) {
            return realizePrimitive(pojo, type, genericType, history);
        } else if (type != null && type.isEnum() && pojo.getClass() == String.class) {
            //枚举
            return Enum.valueOf((Class<Enum>) type, (String) pojo);
        } else if (type != null && Class.class.isAssignableFrom(type) && pojo.getClass() == String.class) {
            //类
            try {
                return ClassUtils.getClass((String) pojo);
            } catch (ClassNotFoundException e) {
                throw new CodecException("Error occurs while realizing, caused by " + e.getMessage(), e);
            }
        }

        Object o = history.get(pojo);
        if (o != null) {
            return o;
        }
        history.put(pojo, pojo);
        if (pojo.getClass().isArray()) {
            return realizeArray(pojo, type, genericType, history);
        } else if (pojo instanceof Collection<?>) {
            return realizeCollection((Collection<?>) pojo, type, genericType, history);
        } else if (pojo instanceof Map<?, ?> && type != null) {
            return realizeMap((Map<?, ?>) pojo, type, genericType, history);
        }
        return pojo;
    }

    /**
     * 反序列化MAP
     *
     * @param pojo
     * @param type
     * @param genericType
     * @param history
     * @return
     */
    protected Object realizeMap(final Map<?, ?> pojo, Class<?> type, final Type genericType,
                                final Map<Object, Object> history) {
        Object className = pojo.get("class");
        if (className != null && className instanceof String) {
            try {
                type = forName((String) className);
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }
        if (Map.class.isAssignableFrom(type) || type == Object.class) {
            return realizeMap2Map(pojo, type, genericType, history);
        } else if (type.isInterface()) {
            return realizeMap2Intf(pojo, type, history);
        } else {
            return realizeMap2Pojo(pojo, type, history);
        }
    }

    /**
     * 反序列化，Map到接口
     *
     * @param pojo
     * @param type
     * @param history
     * @return
     */
    protected Object realizeMap2Intf(final Map<?, ?> pojo, final Class<?> type, final Map<Object, Object> history) {
        Object dest = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[]{type},
                new PojoInvocationHandler(pojo));
        history.put(pojo, dest);
        return dest;
    }

    /**
     * 反序列化，Map到POJO
     *
     * @param pojo
     * @param type
     * @param history
     * @return
     */
    protected Object realizeMap2Pojo(final Map<?, ?> pojo, final Class<?> type, final Map<Object, Object> history) {
        Object dest = newInstance(type);
        history.put(pojo, dest);
        for (Map.Entry<?, ?> entry : pojo.entrySet()) {
            if (entry.getKey() instanceof String && entry.getValue() != null) {
                if (entry.getValue() != null) {
                    setValue(dest.getClass(), (String) entry.getKey(), dest, (c, t) -> realize(entry.getValue(), c, t, history));
                }
            }
        }
        //异常信息
        if (dest instanceof Throwable) {
            Object message = pojo.get("message");
            if (message instanceof String) {
                try {
                    Field filed = Throwable.class.getDeclaredField("detailMessage");
                    if (!filed.isAccessible()) {
                        filed.setAccessible(true);
                    }
                    filed.set(dest, message);
                } catch (Exception e) {
                }
            }
        }
        return dest;
    }

    /**
     * 反序列化，MAP到Map
     *
     * @param pojo
     * @param genericType
     * @param history
     * @return
     */
    protected Object realizeMap2Map(final Map<?, ?> pojo, final Class<?> type, final Type genericType, final Map<Object, Object> history) {
        final Map<Object, Object> result = createMap(type == null || type.isAssignableFrom(pojo.getClass()) ? pojo.getClass() : type);
        Type keyType = genericType != null && genericType instanceof ParameterizedType ? ((ParameterizedType) genericType).getActualTypeArguments()[0] : null;
        Type valueType = genericType != null && genericType instanceof ParameterizedType ? ((ParameterizedType) genericType).getActualTypeArguments()[1] : null;
        history.put(pojo, result);
        Class<?> keyClazz;
        Class<?> valueClazz;
        Object key;
        Object value;
        for (Map.Entry<?, ?> entry : pojo.entrySet()) {
            keyClazz = keyType != null && keyType instanceof Class ? (Class<?>) keyType : (entry.getKey() == null ? null : entry.getKey().getClass());
            valueClazz = valueType != null && valueType instanceof Class ? (Class<?>) valueType : (entry.getValue() == null ? null : entry.getValue().getClass());
            key = keyClazz == null ? entry.getKey() : realize(entry.getKey(), keyClazz, keyType, history);
            value = valueClazz == null ? entry.getValue() : realize(entry.getValue(), valueClazz, valueType, history);
            result.put(key, value);
        }
        return result;
    }

    /**
     * 反序列化计划
     *
     * @param pojo
     * @param type
     * @param genericType
     * @param history
     * @return
     */
    protected Object realizeCollection(final Collection<?> pojo, final Class<?> type, final Type genericType,
                                       final Map<Object, Object> history) {
        if (type.isArray()) {
            return realizeCollection2Array(pojo, type, genericType, history);
        } else {
            return realizeCollection2Collection(pojo, type, genericType, history);
        }
    }

    /**
     * 反序列化数组
     *
     * @param pojo
     * @param type
     * @param genericType
     * @param history
     * @return
     */
    protected Object realizeArray(final Object pojo, final Class<?> type, final Type genericType, Map<Object, Object> history) {
        if (Collection.class.isAssignableFrom(type)) {
            return realizeArray2Collection(pojo, type, genericType, history);
        } else {
            return realizeArray2Array(pojo, type, genericType, history);
        }
    }

    /**
     * 反序列化数组到数组
     *
     * @param pojo
     * @param type
     * @param genericType
     * @param history
     * @return
     */
    protected Object realizeArray2Array(final Object pojo, final Class<?> type, final Type genericType,
                                        final Map<Object, Object> history) {
        Class<?> ctype = type != null && type.isArray() ? type.getComponentType() : pojo.getClass().getComponentType();
        Type cgtype = genericType instanceof GenericArrayType ? ((GenericArrayType) genericType).getGenericComponentType() : null;

        int len = Array.getLength(pojo);
        Object dest = Array.newInstance(ctype, len);
        history.put(pojo, dest);
        for (int i = 0; i < len; i++) {
            Array.set(dest, i, realize(Array.get(pojo, i), ctype, cgtype, history));
        }
        return dest;
    }

    /**
     * 反序列化数组到计划
     *
     * @param pojo
     * @param type
     * @param genericType
     * @param history
     * @return
     */
    protected Object realizeArray2Collection(final Object pojo, final Class<?> type, final Type genericType,
                                             final Map<Object, Object> history) {
        Class<?> ctype = pojo.getClass().getComponentType();
        Type cgtype = genericType instanceof ParameterizedType ? ((ParameterizedType) genericType).getActualTypeArguments()[0] : null;
        int len = Array.getLength(pojo);
        Collection dest = createCollection(type, len);
        history.put(pojo, dest);
        for (int i = 0; i < len; i++) {
            dest.add(realize(Array.get(pojo, i), cgtype instanceof Class ? (Class) cgtype : ctype, cgtype, history));
        }
        return dest;
    }

    /**
     * 反序列化，集合到集合
     *
     * @param pojo
     * @param type
     * @param genericType
     * @param history
     * @return
     */
    protected Object realizeCollection2Collection(final Collection<?> pojo, final Class<?> type, final Type genericType,
                                                  final Map<Object, Object> history) {
        Type clazz = genericType != null && genericType instanceof ParameterizedType ? ((ParameterizedType) genericType).getActualTypeArguments()[0] : null;
        int len = pojo.size();
        Collection<Object> dest = createCollection(type, len);
        history.put(pojo, dest);
        for (Object obj : pojo) {
            dest.add(realize(obj, clazz instanceof Class ? (Class) clazz : obj.getClass(), clazz, history));
        }
        return dest;
    }

    /**
     * 反序列化，集合到数组
     *
     * @param pojo
     * @param type
     * @param genericType
     * @param history
     * @return
     */
    protected Object realizeCollection2Array(final Collection<?> pojo, final Class<?> type, final Type genericType,
                                             final Map<Object, Object> history) {
        Class<?> ctype = type.getComponentType();
        Type cgtype = genericType instanceof GenericArrayType ? ((GenericArrayType) genericType).getGenericComponentType() : null;
        int len = pojo.size();
        Object dest = Array.newInstance(ctype, len);
        history.put(pojo, dest);
        int i = 0;
        for (Object obj : pojo) {
            Array.set(dest, i, realize(obj, cgtype instanceof Class ? (Class) cgtype : ctype, cgtype, history));
            i++;
        }
        return dest;
    }

    /**
     * 构建集合类型
     *
     * @param type
     * @param len
     * @return
     */
    protected Collection<Object> createCollection(final Class<?> type, final int len) {
        if (List.class == type) {
            return new ArrayList<>(len);
        } else if (Set.class == type) {
            return new HashSet<>(len);
        } else if (SortedSet.class == type) {
            return new TreeSet<>();
        } else if (!type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
            try {
                return (Collection<Object>) type.newInstance();
            } catch (Exception e) {
                // ignore
            }
        }
        return new ArrayList<>();
    }

    /**
     * 实例化对象
     *
     * @param cls
     * @return
     */
    protected Object newInstance(final Class<?> cls) {
        try {
            return cls.newInstance();
        } catch (Throwable t) {
            try {
                Constructor<?>[] constructors = cls.getConstructors();
                if (constructors == null || constructors.length == 0) {
                    throw new CodecException("Illegal constructor: " + cls.getName());
                }
                Constructor<?> constructor = constructors[0];
                if (constructor.getParameterTypes().length > 0) {
                    //找到最小参数的构造函数
                    for (Constructor<?> c : constructors) {
                        if (c.getParameterTypes().length < constructor.getParameterTypes().length) {
                            constructor = c;
                            if (constructor.getParameterTypes().length == 0) {
                                break;
                            }
                        }
                    }
                }
                return constructor.newInstance(new Object[constructor.getParameterTypes().length]);
            } catch (InstantiationException e) {
                throw new CodecException(e.getMessage(), e);
            } catch (IllegalAccessException e) {
                throw new CodecException(e.getMessage(), e);
            } catch (InvocationTargetException e) {
                throw new CodecException(e.getMessage(), e);
            }
        }
    }

    /**
     * 兼容类型转换。null值是OK的。如果不需要转换，则返回原来的值。
     * 进行的兼容类型转换如下：（基本类对应的Wrapper类型不再列出。）
     * <ul>
     * <li> String -> char, enum, Date
     * <li> byte, short, int, long -> byte, short, int, long
     * <li> float, double -> float, double
     * </ul>
     *
     * @param value
     * @param type
     * @param genericType
     * @param history
     */
    protected Object realizePrimitive(final Object value, final Class<?> type, final Type genericType,
                                      final Map<Object, Object> history) {
        if (value == null || type == null || type.isAssignableFrom(value.getClass())) {
            return value;
        } else if (value instanceof String) {
            return realizeString(value, type);
        } else if (value instanceof Number) {
            return realizeNumber(value, type);
        } else if (value.getClass().isArray() && Collection.class.isAssignableFrom(type)) {
            return realizeArray2Collection(value, type, genericType, history);
        }
        return value;
    }

    /**
     * 反序列化，数字转换
     *
     * @param value
     * @param type
     * @return
     */
    protected Object realizeNumber(Object value, Class<?> type) {
        Number number = (Number) value;
        if (type == byte.class || type == Byte.class) {
            return number.byteValue();
        } else if (type == short.class || type == Short.class) {
            return number.shortValue();
        } else if (type == int.class || type == Integer.class) {
            return number.intValue();
        } else if (type == long.class || type == Long.class) {
            return number.longValue();
        } else if (type == float.class || type == Float.class) {
            return number.floatValue();
        } else if (type == double.class || type == Double.class) {
            return number.doubleValue();
        } else if (type == BigInteger.class) {
            return BigInteger.valueOf(number.longValue());
        } else if (type == BigDecimal.class) {
            return BigDecimal.valueOf(number.doubleValue());
        } else if (type == Date.class) {
            return new Date(number.longValue());
        }
        return value;
    }

    /**
     * 反序列化，String转换
     *
     * @param value
     * @param type
     * @return
     */
    protected Object realizeString(Object value, Class<?> type) {
        String string = (String) value;
        if (char.class.equals(type) || Character.class.equals(type)) {
            if (string.length() != 1) {
                throw new CodecException(String.format("can not convert String(%s) to char!" +
                        " when convert String to char, the String MUST only 1 char.", string));
            }
            return string.charAt(0);
        } else if (type.isEnum()) {
            return Enum.valueOf((Class<Enum>) type, string);
        } else if (type == BigInteger.class) {
            return new BigInteger(string);
        } else if (type == BigDecimal.class) {
            return new BigDecimal(string);
        } else if (type == Short.class || type == short.class) {
            return new Short(string);
        } else if (type == Integer.class || type == int.class) {
            return new Integer(string);
        } else if (type == Long.class || type == long.class) {
            return new Long(string);
        } else if (type == Double.class || type == double.class) {
            return new Double(string);
        } else if (type == Float.class || type == float.class) {
            return new Float(string);
        } else if (type == Byte.class || type == byte.class) {
            return new Byte(string);
        } else if (type == Boolean.class || type == boolean.class) {
            return new Boolean(string);
        } else if (type == Date.class) {
            try {
                return LocalDateTime.parse(string, DATE_FORMAT_TIME);
            } catch (DateTimeParseException e) {
                throw new CodecException("Failed to parse date " + value + " by format " + DATE_FORMAT_TIME + ", cause: " + e.getMessage(), e);
            }
        } else if (type == Class.class) {
            try {
                return ClassUtils.getClass((String) value);
            } catch (ClassNotFoundException e) {
                throw new CodecException(e.getMessage(), e);
            }
        }
        return value;
    }

    /**
     * 对象调用
     */
    protected class PojoInvocationHandler implements InvocationHandler {

        protected final Map<?, ?> map;

        /**
         * 构造函数
         *
         * @param map
         */
        public PojoInvocationHandler(final Map<?, ?> map) {
            this.map = map;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(map, args);
            }
            String methodName = method.getName();
            Object value;
            if (methodName.length() > 3 && methodName.startsWith("get")) {
                value = map.get(methodName.substring(3, 4).toLowerCase() + methodName.substring(4));
            } else if (methodName.length() > 2 && methodName.startsWith("is")) {
                value = map.get(methodName.substring(2, 3).toLowerCase() + methodName.substring(3));
            } else {
                value = map.get(methodName.substring(0, 1).toLowerCase() + methodName.substring(1));
            }
            if (value != null && value instanceof Map<?, ?> && !Map.class.isAssignableFrom(method.getReturnType())) {
                value = realize(value, method.getReturnType(), method.getGenericReturnType(), new IdentityHashMap<>());
            }
            return value;
        }
    }

}
