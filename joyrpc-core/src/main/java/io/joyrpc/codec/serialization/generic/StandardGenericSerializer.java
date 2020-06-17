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
import io.joyrpc.exception.MethodOverloadException;
import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.message.Call;
import io.joyrpc.util.ClassUtils;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.joyrpc.util.ClassUtils.*;

/**
 * 默认泛型序列化器
 */
@Extension("standard")
public class StandardGenericSerializer implements GenericSerializer {

    /**
     * 普通时间的格式
     */
    protected static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
    public static final String CLASS = "class";

    @Override
    public Object serialize(final Object object) throws CodecException {
        try {
            return normalize(object);
        } catch (CodecException e) {
            throw e;
        } catch (Exception e) {
            throw new CodecException("Error occurs while normalizing, caused by " + e.getMessage(), e);
        }
    }

    @Override
    public Object[] deserialize(final Call invocation) throws CodecException {
        try {
            //计算真实的参数类型
            Type[] types = invocation.computeTypes();
            Class[] argClasses = invocation.getArgClasses();
            Object[] genericArgs = invocation.getArgs();
            Object[] paramArgs = (Object[]) genericArgs[2];
            return realize(paramArgs, argClasses, types);
        } catch (CodecException e) {
            throw e;
        } catch (NoSuchMethodException | MethodOverloadException | ClassNotFoundException e) {
            throw new CodecException("Error occurs while realizing, caused by " + e.getMessage());
        } catch (Exception e) {
            throw new CodecException("Error occurs while realizing, caused by " + e.getMessage(), e);
        }
    }

    /**
     * 标准化
     *
     * @param pojo
     * @return
     */
    protected Object normalize(final Object pojo) throws Exception {
        return normalize(pojo, new IdentityHashMap<>());
    }

    /**
     * 标准化
     *
     * @param pojo
     * @param history
     * @return
     */
    protected Object normalize(final Object pojo, final Map<Object, Object> history) throws Exception {
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

        if (pojoClass.isArray()) {
            return normalizeArray(pojo, history);
        } else if (pojo instanceof Collection<?>) {
            return normalizeCollection((Collection<?>) pojo, history);
        } else if (pojo instanceof Map<?, ?>) {
            return normalizeMap((Map<?, ?>) pojo, history);
        } else {
            return normalizePojo(pojo, history);
        }
    }

    /**
     * 标准化POJO
     *
     * @param pojo
     * @param history
     * @return
     */
    protected Object normalizePojo(final Object pojo, final Map<Object, Object> history) throws Exception {
        Class<?> pojoClass = pojo.getClass();
        Map<String, Object> result = new HashMap<>();
        history.put(pojo, result);
        result.put(CLASS, pojoClass.getName());
        //读方法 //TODO 可能因 get方法new出新对象，无限循环
        for (Map.Entry<String, Method> entry : getGetter(pojoClass).entrySet()) {
            result.put(entry.getKey(), normalize(entry.getValue().invoke(pojo), history));
        }
        int modifiers;
        //公共字段
        for (Field field : getFields(pojoClass)) {
            modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers)
                    && !Modifier.isStatic(modifiers)
                    && !Modifier.isFinal(modifiers) && !field.isSynthetic()
                    && !Modifier.isTransient(modifiers)
                    && !result.containsKey(field.getName())) {
                Object fieldValue = field.get(pojo);
                if (fieldValue != null) {
                    result.putIfAbsent(field.getName(), normalize(fieldValue, history));
                }
            }
        }
        return result;
    }

    /**
     * 标准化MAP
     *
     * @param pojo
     * @param history
     * @return
     */
    protected Object normalizeMap(Map<?, ?> pojo, Map<Object, Object> history) throws Exception {
        Map<Object, Object> result = createMap(pojo.getClass(), pojo.size());
        history.put(pojo, result);
        for (Map.Entry<?, ?> obj : pojo.entrySet()) {
            result.put(normalize(obj.getKey(), history), normalize(obj.getValue(), history));
        }
        return result;
    }

    /**
     * 标准化集合
     *
     * @param pojo
     * @param history
     * @return
     */
    protected Object normalizeCollection(Collection<?> pojo, Map<Object, Object> history) throws Exception {
        Collection<Object> result = createCollection(pojo.getClass(), pojo.size());
        history.put(pojo, result);
        for (Object obj : pojo) {
            result.add(normalize(obj, history));
        }
        return result;
    }

    /**
     * 标准化数组
     *
     * @param pojo
     * @param history
     * @return
     */
    protected Object normalizeArray(final Object pojo, final Map<Object, Object> history) throws Exception {
        if (pojo.getClass().getComponentType().isEnum()) {
            return normalizeEnumArray(pojo, history);
        }
        int len = Array.getLength(pojo);
        Object[] result = new Object[len];
        history.put(pojo, result);
        for (int i = 0; i < len; i++) {
            result[i] = normalize(Array.get(pojo, i), history);
        }
        return result;
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
        String[] result = new String[len];
        history.put(pojo, result);
        for (int i = 0; i < len; i++) {
            result[i] = ((Enum<?>) Array.get(pojo, i)).name();
        }
        return result;
    }

    /**
     * 获取类型
     *
     * @param type     类型
     * @param supplier 类型提供者
     * @return 类型
     */
    protected Class<?> getType(final Type type, final Supplier<Class> supplier) {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
        return supplier == null ? null : supplier.get();
    }

    /**
     * 构建集合类型
     *
     * @param type 类型
     * @param len  长度
     * @return 集合对象
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
     * 创建MAP
     *
     * @param clazz 类型
     * @param size  大小
     * @return Map对象
     */
    protected Map createMap(final Class<?> clazz, final int size) {
        if (HashMap.class == clazz) {
            return new HashMap(size);
        } else if (Hashtable.class == clazz) {
            return new Hashtable(size);
        } else if (IdentityHashMap.class == clazz) {
            return new IdentityHashMap(size);
        } else if (LinkedHashMap.class == clazz) {
            return new LinkedHashMap(size);
        } else if (Properties.class == clazz) {
            return new Properties();
        } else if (TreeMap.class == clazz) {
            return new TreeMap();
        } else if (WeakHashMap.class == clazz) {
            return new WeakHashMap(size);
        } else if (ConcurrentHashMap.class == clazz) {
            return new ConcurrentHashMap(size);
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
     * @param objs            参数
     * @param resolvedClasses 参数类型
     * @param resolvedTypes   参数泛化类型
     * @return
     */
    protected Object[] realize(final Object[] objs, final Class<?>[] resolvedClasses, final Type[] resolvedTypes) throws Exception {
        if (objs.length != resolvedClasses.length) {
            throw new CodecException("args.length != resolvedClasses.length");
        }
        //一个方法下参数共用一个history
        Map<Object, Object> history = new IdentityHashMap<>();
        //反序列化
        Object[] result = new Object[objs.length];
        for (int i = 0; i < objs.length; i++) {
            result[i] = realize(objs[i], resolvedClasses[i], resolvedTypes[i], history);
        }
        return result;
    }

    /**
     * 反序列化
     *
     * @param pojo          源对象
     * @param resolvedClass 真实类型
     * @param resolvedType  真实泛型
     * @param history       对象历史
     * @return 目标对象
     */
    protected Object realize(final Object pojo, final Class<?> resolvedClass, final Type resolvedType,
                             final Map<Object, Object> history) throws Exception {
        if (pojo == null) {
            return null;
        }

        Class<?> clazz = pojo.getClass();
        if (isPrimitive(clazz, PRIMITIVE)) {
            return realizePrimitive(pojo, resolvedClass, resolvedType, history);
        } else if (resolvedClass != null && resolvedClass.isEnum() && clazz == String.class) {
            //枚举
            return Enum.valueOf((Class<Enum>) resolvedClass, (String) pojo);
        } else if (resolvedClass != null && Class.class.isAssignableFrom(resolvedClass) && clazz == String.class) {
            //类
            return ClassUtils.getClass((String) pojo);
        }

        Object o = history.get(pojo);
        if (o != null) {
            return o;
        }
        history.put(pojo, pojo);
        if (clazz.isArray()) {
            return realizeArray(pojo, resolvedClass, resolvedType, history);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            return realizeCollection((Collection<?>) pojo, resolvedClass, resolvedType, history);
        } else if (Map.class.isAssignableFrom(clazz) && resolvedClass != null) {
            return realizeMap((Map<?, ?>) pojo, resolvedClass, resolvedType, history);
        }
        return pojo;
    }

    /**
     * 反序列化MAP
     *
     * @param pojo        字段值
     * @param type        类型
     * @param genericType 泛型
     * @param history     历史
     * @return 反序列化后的对象
     */
    protected Object realizeMap(final Map<?, ?> pojo, final Class<?> type, final Type genericType,
                                final Map<Object, Object> history) throws Exception {
        Object className = pojo.get(CLASS);
        Class<?> realType = type;
        if (className instanceof String && !((String) className).isEmpty()) {
            try {
                realType = forName((String) className);
                if (!type.isAssignableFrom(realType)) {
                    //类型校验
                    throw new CodecException(String.format("%s is not assignable from %s", type, className));
                }
                pojo.remove(CLASS);
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }

        if (Map.class.isAssignableFrom(realType) || realType == Object.class) {
            return realizeMap2Map(pojo, realType, genericType, history);
        } else if (realType.isInterface()) {
            return realizeMap2Intf(pojo, realType, history);
        } else if (realType.isEnum()) {
            Object name = pojo.get("name");
            if (name != null) {
                return Enum.valueOf((Class<Enum>) realType, name.toString());
            }
        }
        return realizeMap2Pojo(pojo, realType, genericType, history);
    }

    /**
     * 反序列化，Map到接口
     *
     * @param pojo          源对象
     * @param resolvedClass 目标类型
     * @param history       历史
     * @return 目标对象
     */
    protected Object realizeMap2Intf(final Map<?, ?> pojo, final Class<?> resolvedClass, final Map<Object, Object> history) {
        Object result = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{resolvedClass}, new PojoWrapper(pojo));
        history.put(pojo, result);
        return result;
    }

    /**
     * 反序列化，Map到POJO
     *
     * @param pojo          源对象
     * @param resolvedClass 目标类型
     * @param history       历史
     * @return 目标对象
     */
    protected Object realizeMap2Pojo(final Map<?, ?> pojo, final Class<?> resolvedClass, final Type resolvedType, final Map<Object, Object> history) throws Exception {
        Object result = newInstance(resolvedClass);
        history.put(pojo, result);
        //根据入参类型，匹配泛型T的真正类型
        Map<TypeVariable, Type> valueRealTypes = getValueRealTypes(resolvedClass, resolvedType);
        //逐一设置字段的值
        setValues(result, pojo, (v, c, t) -> {
            try {
                //如果字段是泛型，设置真正的类型
                if (t instanceof TypeVariable) {
                    Type realType = valueRealTypes == null ? null : valueRealTypes.get(t);
                    if (realType instanceof Class) {
                        c = (Class<?>) realType;
                    } else if (realType instanceof ParameterizedType) {
                        c = (Class<?>) ((ParameterizedType) realType).getRawType();
                        t = realType;
                    }
                }
                return realize(v, c, t, history);
            } catch (CodecException e) {
                throw e;
            } catch (Exception e) {
                throw new CodecException(e.getMessage(), e);
            }
        });
        //异常信息
        if (result instanceof Throwable) {
            Object message = pojo.get("message");
            if (message instanceof String) {
                try {
                    setValue(result.getClass(), "detailMessage", result, message);
                } catch (Exception e) {
                }
            }
        }
        return result;
    }

    /**
     * 根据入参类型，匹配泛型T的真正类型
     *
     * @param resolvedClass 类型
     * @param resolvedType  泛型
     * @return
     */
    protected Map<TypeVariable, Type> getValueRealTypes(final Class<?> resolvedClass, final Type resolvedType) {
        Type genericType = resolvedType;
        Class genericClazz = resolvedClass;
        //泛型为Class，获取父类的泛型
        if (genericType instanceof Class) {
            for (; ; ) {
                Type superGeneric = ((Class) genericType).getGenericSuperclass();
                Class superClazz = genericClazz.getSuperclass();
                if (superGeneric instanceof ParameterizedType) {
                    genericClazz = superClazz;
                    genericType = superGeneric;
                    break;
                } else if (!(superGeneric instanceof Class)) {
                    break;
                } else {
                    genericType = superGeneric;
                    genericClazz = superClazz;
                }
            }
        }
        if (genericType instanceof ParameterizedType) {
            Map<TypeVariable, Type> valueRealTypes = new HashMap<>();
            TypeVariable[] typeVariables = genericClazz.getTypeParameters();
            Type[] genericTypes = ((ParameterizedType) genericType).getActualTypeArguments();
            for (int i = 0; i < typeVariables.length; i++) {
                valueRealTypes.put(typeVariables[i], genericTypes[i]);
            }
            return valueRealTypes;
        }
        return null;
    }

    /**
     * 反序列化，MAP到Map
     *
     * @param pojo          源对象
     * @param resolvedClass 目标类型
     * @param resolvedType  目标泛型
     * @param history       历史
     * @return 目标对象
     */
    protected Object realizeMap2Map(final Map<?, ?> pojo, final Class<?> resolvedClass, final Type resolvedType,
                                    final Map<Object, Object> history) throws Exception {
        Map<Object, Object> result = createMap(resolvedClass == null || resolvedClass.isAssignableFrom(pojo.getClass()) ? pojo.getClass() : resolvedClass, pojo.size());
        Type keyType = resolvedType != null && resolvedType instanceof ParameterizedType ? ((ParameterizedType) resolvedType).getActualTypeArguments()[0] : null;
        Type valueType = resolvedType != null && resolvedType instanceof ParameterizedType ? ((ParameterizedType) resolvedType).getActualTypeArguments()[1] : null;
        history.put(pojo, result);
        Class<?> keyClazz;
        Class<?> valueClazz;
        Object key;
        Object value;
        for (Map.Entry<?, ?> entry : pojo.entrySet()) {
            keyClazz = getType(keyType, () -> (entry.getKey() == null ? null : entry.getKey().getClass()));
            valueClazz = getType(valueType, () -> (entry.getValue() == null ? null : entry.getValue().getClass()));
            key = keyClazz == null ? entry.getKey() : realize(entry.getKey(), keyClazz, keyType, history);
            value = valueClazz == null ? entry.getValue() : realize(entry.getValue(), valueClazz, valueType, history);
            result.put(key, value);
        }
        return result;
    }


    /**
     * 反序列化集合
     *
     * @param pojo          源对象
     * @param resolvedClass 目标类型
     * @param resolvedType  目标泛型
     * @param history       历史
     * @return 目标对象
     */
    protected Object realizeCollection(final Collection<?> pojo, final Class<?> resolvedClass, final Type resolvedType,
                                       final Map<Object, Object> history) throws Exception {
        if (resolvedClass.isArray()) {
            return realizeCollection2Array(pojo, resolvedClass, resolvedType, history);
        } else {
            return realizeCollection2Collection(pojo, resolvedClass, resolvedType, history);
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
    protected Object realizeArray(final Object pojo, final Class<?> type, final Type genericType,
                                  final Map<Object, Object> history) throws Exception {
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
                                        final Map<Object, Object> history) throws Exception {
        Class<?> ctype = type != null && type.isArray() ? type.getComponentType() : pojo.getClass().getComponentType();
        Type cgtype = genericType instanceof GenericArrayType ? ((GenericArrayType) genericType).getGenericComponentType() : null;

        int len = Array.getLength(pojo);
        Object dest = Array.newInstance(ctype, len);
        history.put(pojo, dest);
        for (int i = 0; i < len; i++) {
            Array.set(dest, i, realize(Array.get(pojo, i), cgtype instanceof Class ? (Class<?>) cgtype : ctype, cgtype, history));
        }
        return dest;
    }

    /**
     * 反序列化数组到
     *
     * @param pojo          源对象
     * @param resolvedClass 目标类型
     * @param resolvedType  目标泛型
     * @param history       历史
     * @return 目标对象
     */
    protected Object realizeArray2Collection(final Object pojo, final Class<?> resolvedClass, final Type resolvedType,
                                             final Map<Object, Object> history) throws Exception {
        Class<?> ctype = pojo.getClass().getComponentType();
        Type cgtype = resolvedType instanceof ParameterizedType ? ((ParameterizedType) resolvedType).getActualTypeArguments()[0] : null;
        int len = Array.getLength(pojo);
        Collection result = createCollection(resolvedClass, len);
        history.put(pojo, result);
        for (int i = 0; i < len; i++) {
            result.add(realize(Array.get(pojo, i), cgtype instanceof Class ? (Class) cgtype : ctype, cgtype, history));
        }
        return result;
    }

    /**
     * 反序列化，集合到集合
     *
     * @param pojo          源对象
     * @param resolvedClass 目标类型
     * @param resolvedType  目标泛型
     * @param history       历史
     * @return 目标对象
     */
    protected Object realizeCollection2Collection(final Collection<?> pojo, final Class<?> resolvedClass, final Type resolvedType,
                                                  final Map<Object, Object> history) throws Exception {
        Type clazz = resolvedType != null && resolvedType instanceof ParameterizedType ? ((ParameterizedType) resolvedType).getActualTypeArguments()[0] : null;
        int len = pojo.size();
        Collection<Object> result = createCollection(resolvedClass, len);
        history.put(pojo, result);
        for (Object obj : pojo) {
            result.add(realize(obj, clazz instanceof Class ? (Class) clazz : obj.getClass(), clazz, history));
        }
        return result;
    }

    /**
     * 反序列化，集合到数组
     *
     * @param pojo          源对象
     * @param resolvedClass 目标类型
     * @param resolvedType  目标泛型
     * @param history       历史
     * @return 目标对象
     */
    protected Object realizeCollection2Array(final Collection<?> pojo, final Class<?> resolvedClass, final Type resolvedType,
                                             final Map<Object, Object> history) throws Exception {
        Class<?> ctype = resolvedClass.getComponentType();
        Type cgtype = resolvedType instanceof GenericArrayType ? ((GenericArrayType) resolvedType).getGenericComponentType() : null;
        int len = pojo.size();
        Object result = Array.newInstance(ctype, len);
        history.put(pojo, result);
        int i = 0;
        for (Object obj : pojo) {
            Array.set(result, i, realize(obj, cgtype instanceof Class ? (Class) cgtype : ctype, cgtype, history));
            i++;
        }
        return result;
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
     * @param value         源对象
     * @param resolvedClass 目标类型
     * @param resolvedType  目标泛型
     * @param history       历史
     * @return 目标对象
     */
    protected Object realizePrimitive(final Object value, final Class<?> resolvedClass, final Type resolvedType,
                                      final Map<Object, Object> history) throws Exception {
        if (value == null || resolvedClass == null || resolvedClass.isAssignableFrom(value.getClass())) {
            return value;
        } else if (value instanceof String) {
            return realizeString(value, resolvedClass);
        } else if (value instanceof Number) {
            return realizeNumber(value, resolvedClass);
        } else if (value.getClass().isArray() && Collection.class.isAssignableFrom(resolvedClass)) {
            return realizeArray2Collection(value, resolvedClass, resolvedType, history);
        }
        return value;
    }

    /**
     * 反序列化，数字转换
     *
     * @param value         源对象
     * @param resolvedClass 目标类型
     * @return 目标对象
     */
    protected Object realizeNumber(final Object value, final Class<?> resolvedClass) {
        Number number = (Number) value;
        if (resolvedClass == byte.class || resolvedClass == Byte.class) {
            return number.byteValue();
        } else if (resolvedClass == short.class || resolvedClass == Short.class) {
            return number.shortValue();
        } else if (resolvedClass == int.class || resolvedClass == Integer.class) {
            return number.intValue();
        } else if (resolvedClass == long.class || resolvedClass == Long.class) {
            return number.longValue();
        } else if (resolvedClass == float.class || resolvedClass == Float.class) {
            return number.floatValue();
        } else if (resolvedClass == double.class || resolvedClass == Double.class) {
            return number.doubleValue();
        } else if (resolvedClass == BigInteger.class) {
            return BigInteger.valueOf(number.longValue());
        } else if (resolvedClass == BigDecimal.class) {
            return BigDecimal.valueOf(number.doubleValue());
        } else if (resolvedClass == boolean.class || resolvedClass == Boolean.class) {
            return 0 != number.intValue();
        } else if (resolvedClass == Date.class) {
            return new Date(number.longValue());
        } else if (resolvedClass == java.sql.Date.class) {
            return new java.sql.Date(number.longValue());
        } else if (resolvedClass == java.sql.Timestamp.class) {
            return new java.sql.Timestamp(number.longValue());
        } else if (resolvedClass == java.sql.Time.class) {
            return new java.sql.Time(number.longValue());
        }
        return value;
    }

    /**
     * 反序列化，String转换
     *
     * @param value         源对象
     * @param resolvedClass 目标类型
     * @return 目标对象
     */
    protected Object realizeString(final Object value, final Class<?> resolvedClass) throws Exception {
        String text = (String) value;
        if (char.class.equals(resolvedClass) || Character.class.equals(resolvedClass)) {
            if (text.length() != 1) {
                throw new CodecException(String.format("can not convert %s to char!", text));
            }
            return text.charAt(0);
        } else if (resolvedClass.isEnum()) {
            return Enum.valueOf((Class<Enum>) resolvedClass, text);
        } else if (resolvedClass == Integer.class || resolvedClass == int.class) {
            return new Integer(text);
        } else if (resolvedClass == Long.class || resolvedClass == long.class) {
            return new Long(text);
        } else if (resolvedClass == Double.class || resolvedClass == double.class) {
            return new Double(text);
        } else if (resolvedClass == Boolean.class || resolvedClass == boolean.class) {
            return new Boolean(text);
        } else if (resolvedClass == Byte.class || resolvedClass == byte.class) {
            return new Byte(text);
        } else if (resolvedClass == Short.class || resolvedClass == short.class) {
            return new Short(text);
        } else if (resolvedClass == Float.class || resolvedClass == float.class) {
            return new Float(text);
        } else if (resolvedClass == Date.class) {
            return Date.from(LocalDateTime.parse(text, DATE_TIME_FORMATTER).atZone(ZoneId.systemDefault()).toInstant());
        } else if (resolvedClass == java.sql.Date.class) {
            return java.sql.Date.from(LocalDateTime.parse(text, DATE_TIME_FORMATTER).atZone(ZoneId.systemDefault()).toInstant());
        } else if (resolvedClass == java.sql.Timestamp.class) {
            return java.sql.Timestamp.from(LocalDateTime.parse(text, DATE_TIME_FORMATTER).atZone(ZoneId.systemDefault()).toInstant());
        } else if (resolvedClass == java.sql.Time.class) {
            return java.sql.Time.from(LocalDateTime.parse(text, DATE_TIME_FORMATTER).atZone(ZoneId.systemDefault()).toInstant());
        } else if (resolvedClass == Class.class) {
            return ClassUtils.getClass((String) value);
        } else if (char[].class.equals(resolvedClass)) {
            return text.toCharArray();
        } else if (resolvedClass == BigInteger.class) {
            return new BigInteger(text);
        } else if (resolvedClass == BigDecimal.class) {
            return new BigDecimal(text);
        }
        return value;
    }

    /**
     * 对象调用
     */
    protected class PojoWrapper implements InvocationHandler {

        protected final Map<?, ?> map;

        /**
         * 构造函数
         *
         * @param map
         */
        public PojoWrapper(final Map<?, ?> map) {
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
