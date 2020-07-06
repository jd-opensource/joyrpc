package org.apache.dubbo.common.serialize.protostuff;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protostuff can only serialize/deserialize POJOs, for those it can't deal with, use this Wrapper.
 */
public class Wrapper<T> {
    protected static transient final Set<Class<?>> WRAPPER_SET = new HashSet<>();

    public static final byte[] CLASS_NAMES = Wrapper.class.getName().getBytes();

    protected T data;

    static {
        WRAPPER_SET.add(Map.class);
        WRAPPER_SET.add(HashMap.class);
        WRAPPER_SET.add(TreeMap.class);
        WRAPPER_SET.add(Hashtable.class);
        WRAPPER_SET.add(SortedMap.class);
        WRAPPER_SET.add(LinkedHashMap.class);
        WRAPPER_SET.add(ConcurrentHashMap.class);

        WRAPPER_SET.add(List.class);
        WRAPPER_SET.add(ArrayList.class);
        WRAPPER_SET.add(LinkedList.class);

        WRAPPER_SET.add(Vector.class);

        WRAPPER_SET.add(Set.class);
        WRAPPER_SET.add(HashSet.class);
        WRAPPER_SET.add(TreeSet.class);
        WRAPPER_SET.add(BitSet.class);

        WRAPPER_SET.add(StringBuffer.class);
        WRAPPER_SET.add(StringBuilder.class);

        WRAPPER_SET.add(BigDecimal.class);
        WRAPPER_SET.add(Date.class);
        WRAPPER_SET.add(Calendar.class);
        WRAPPER_SET.add(Time.class);
        WRAPPER_SET.add(Timestamp.class);
        WRAPPER_SET.add(java.sql.Date.class);

        WRAPPER_SET.add(Wrapper.class);

    }

    public Wrapper(T data) {
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    /**
     * Determine if the object needs wrap
     *
     * @param clazz object type
     * @return need wrap
     */
    public static boolean needWrapper(Class<?> clazz) {
        return WRAPPER_SET.contains(clazz) || clazz.isArray() || clazz.isEnum();
    }

    /**
     * Determine if the object needs wrap
     *
     * @param obj object
     * @return need wrap
     */
    public static boolean needWrapper(Object obj) {
        return needWrapper(obj.getClass());
    }
}
