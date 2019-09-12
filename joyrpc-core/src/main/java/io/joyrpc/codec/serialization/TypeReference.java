package io.joyrpc.codec.serialization;

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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 来源于fastjson，用户json插件接口
 *
 * @param <T>
 */
public abstract class TypeReference<T> {
    protected static ConcurrentMap<Type, Type> TYPE = new ConcurrentHashMap<Type, Type>(16, 0.75f, 1);
    public final static Type LIST_STRING = new TypeReference<List<String>>() {
    }.getType();

    protected final Type type;

    protected TypeReference() {
        Type superClass = getClass().getGenericSuperclass();
        Type type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
        this.type = putIfAbsent(type);
    }

    protected TypeReference(final Type rawType, Type[] argTypes) {
        Type type = new ParameterizedTypeImpl(argTypes, this.getClass(), rawType);
        this.type = putIfAbsent(type);
    }

    protected TypeReference(final Type... actualTypeArguments) {
        Class<?> thisClass = this.getClass();
        Type superClass = thisClass.getGenericSuperclass();
        ParameterizedType argType = (ParameterizedType) ((ParameterizedType) superClass).getActualTypeArguments()[0];

        Type type = handle(thisClass, argType, actualTypeArguments, 0);
        this.type = putIfAbsent(type);
    }

    protected Type putIfAbsent(final Type type) {
        return TYPE.computeIfAbsent(type, k -> k);
    }

    /**
     * 处理参数类型
     *
     * @param thisClass
     * @param type
     * @param actualTypeArguments
     * @param actualIndex
     * @return
     */
    protected Type handle(final Class<?> thisClass, final ParameterizedType type, final Type[] actualTypeArguments, int actualIndex) {
        Type[] types = type.getActualTypeArguments();

        for (int i = 0; i < types.length; ++i) {
            //设置成注入的类型
            if (types[i] instanceof TypeVariable && actualIndex < actualTypeArguments.length) {
                types[i] = actualTypeArguments[actualIndex++];
            }
            // fix for openjdk and android env
            if (types[i] instanceof GenericArrayType) {
                types[i] = checkPrimitiveArray((GenericArrayType) types[i]);
            }
            // 嵌套
            if (types[i] instanceof ParameterizedType) {
                types[i] = handle(thisClass, (ParameterizedType) types[i], actualTypeArguments, actualIndex);
            }
        }

        return new ParameterizedTypeImpl(types, thisClass, type.getRawType());
    }

    /**
     * 基本数组类型
     *
     * @param genericArrayType
     * @return
     */
    protected Type checkPrimitiveArray(final GenericArrayType genericArrayType) {
        Type clz = genericArrayType;

        //处理嵌套的泛型数组类型，得到数组的元素类型
        Type genericComponentType = genericArrayType.getGenericComponentType();
        String prefix = "[";
        while (genericComponentType instanceof GenericArrayType) {
            genericComponentType = ((GenericArrayType) genericComponentType)
                    .getGenericComponentType();
            prefix += prefix;
        }

        if (genericComponentType instanceof Class<?>) {
            Class<?> ck = (Class<?>) genericComponentType;
            if (ck.isPrimitive()) {
                try {
                    if (ck == boolean.class) {
                        clz = Class.forName(prefix + "Z");
                    } else if (ck == char.class) {
                        clz = Class.forName(prefix + "C");
                    } else if (ck == byte.class) {
                        clz = Class.forName(prefix + "B");
                    } else if (ck == short.class) {
                        clz = Class.forName(prefix + "S");
                    } else if (ck == int.class) {
                        clz = Class.forName(prefix + "I");
                    } else if (ck == long.class) {
                        clz = Class.forName(prefix + "J");
                    } else if (ck == float.class) {
                        clz = Class.forName(prefix + "F");
                    } else if (ck == double.class) {
                        clz = Class.forName(prefix + "D");
                    }
                } catch (ClassNotFoundException e) {
                }
            }
        }

        return clz;
    }

    public Type getType() {
        return type;
    }

    /**
     * 参数化类型
     */
    protected class ParameterizedTypeImpl implements ParameterizedType {

        /**
         * 真实类型
         */
        protected final Type[] actualTypeArguments;
        protected final Type ownerType;
        protected final Type rawType;

        /**
         * 构造函数
         *
         * @param actualTypeArguments
         * @param ownerType
         * @param rawType
         */
        public ParameterizedTypeImpl(Type[] actualTypeArguments, Type ownerType, Type rawType) {
            this.actualTypeArguments = actualTypeArguments;
            this.ownerType = ownerType;
            this.rawType = rawType;
        }

        public Type[] getActualTypeArguments() {
            return actualTypeArguments;
        }

        public Type getOwnerType() {
            return ownerType;
        }

        public Type getRawType() {
            return rawType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ParameterizedTypeImpl that = (ParameterizedTypeImpl) o;

            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(actualTypeArguments, that.actualTypeArguments)) {
                return false;
            }
            if (ownerType != null ? !ownerType.equals(that.ownerType) : that.ownerType != null) {
                return false;
            }
            return rawType != null ? rawType.equals(that.rawType) : that.rawType == null;
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(actualTypeArguments);
            result = 31 * result + (ownerType != null ? ownerType.hashCode() : 0);
            result = 31 * result + (rawType != null ? rawType.hashCode() : 0);
            return result;
        }
    }

}
