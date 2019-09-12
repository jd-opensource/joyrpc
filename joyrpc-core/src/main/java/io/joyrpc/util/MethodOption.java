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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 方法选项缓存
 *
 * @param <T>
 */
public class MethodOption<M, T> {

    /**
     * 方法作为Key的函数
     */
    public static final Function<Method, Method> METHOD_KEY = method -> method;
    /**
     * 名字作为Key的转换函数
     */
    protected static final Function<Method, String> NAME_KEY = method -> method.getName();
    /**
     * 方法缓存数据
     */
    protected Map<M, T> options;
    /**
     * 键函数
     */
    protected Function<Method, M> keyFunction;
    /**
     * 记录数量
     */
    protected int size;

    /**
     * 构造函数
     */
    protected MethodOption() {

    }

    /**
     * 构造函数
     *
     * @param clazz         接口类
     * @param keyFunction   键函数
     * @param valueFunction 值函数
     */
    public MethodOption(final Class clazz, final Function<Method, M> keyFunction, final Function<Method, T> valueFunction) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(keyFunction);
        Objects.requireNonNull(valueFunction);
        buildOptions(clazz, keyFunction, valueFunction);
    }

    /**
     * 构造选项
     *
     * @param clazz
     * @param keyFunction
     * @param valueFunction
     */
    protected void buildOptions(final Class clazz, final Function<Method, M> keyFunction, final Function<Method, T> valueFunction) {
        this.keyFunction = keyFunction;
        this.options = new HashMap<>();
        ClassUtils.getPublicMethod(clazz).forEach(o -> {
            M key = keyFunction.apply(o);
            T value = valueFunction.apply(o);
            if (value != null) {
                options.put(key, value);
            }
        });
        size = options.size();
    }

    /**
     * 获取缓存的数据
     *
     * @param method
     * @return
     */
    public T get(final Method method) {
        return method == null || size == 0 ? null : options.get(keyFunction.apply(method));
    }


    /**
     * 以Method作为key的方法选项缓存
     *
     * @param <T>
     */
    public static class MethodKeyOption<T> extends MethodOption<Method, T> {

        /**
         * 构造函数
         *
         * @param clazz
         * @param valueFunction
         */
        public MethodKeyOption(Class clazz, Function<Method, T> valueFunction) {
            super(clazz, METHOD_KEY, valueFunction);
        }
    }

    /**
     * 以名称作为Key的方法选项缓存，重载方法会有相同的选项
     *
     * @param <T>
     */
    public static class NameKeyOption<T> extends MethodOption<String, T> {

        /**
         * 类名
         */
        protected String className;

        /**
         * 键函数
         */
        protected Function<String, T> nameFunction;
        /**
         * 根据方法查询
         */
        protected Function<Method, T> methodGetter;
        /**
         * 根据名称查询
         */
        protected Function<String, T> nameGetter;

        /**
         * 构造函数
         *
         * @param clazz
         * @param valueFunction
         */
        public NameKeyOption(final Class clazz, final Function<Method, T> valueFunction) {
            super(clazz, NAME_KEY, valueFunction);
        }

        /**
         * 构造函数
         *
         * @param className
         * @param nameFunction
         */
        public NameKeyOption(final String className, final Function<String, T> nameFunction) {
            this(null, className, nameFunction);
        }

        @Override
        protected void buildOptions(final Class clazz, final Function<Method, String> keyFunction, final Function<Method, T> valueFunction) {
            super.buildOptions(clazz, keyFunction, valueFunction);
            this.methodGetter = (method) -> size == 0 ? null : options.get(keyFunction.apply(method));
            this.nameGetter = (name) -> size == 0 ? null : options.get(name);
        }

        /**
         * 构造函数
         *
         * @param clazz
         * @param className
         * @param nameFunction
         */
        public NameKeyOption(final Class clazz, final String className, final Function<String, T> nameFunction) {
            Objects.requireNonNull(nameFunction);
            if (className != null && clazz == null) {
                this.className = className;
                this.nameFunction = nameFunction;
                this.options = new ConcurrentHashMap<>();
                this.methodGetter = (method) -> options.computeIfAbsent(method.getName(), name -> nameFunction.apply(name));
                this.nameGetter = (name) -> options.computeIfAbsent(name, n -> nameFunction.apply(n));
            } else if (clazz != null) {
                buildOptions(clazz, NAME_KEY, method -> nameFunction.apply(keyFunction.apply(method)));
            } else {
                throw new IllegalArgumentException("class or className can not be null.");
            }

        }

        @Override
        public T get(final Method method) {
            return method == null ? null : methodGetter.apply(method);
        }

        /**
         * 根据方法名称获取
         *
         * @param name
         * @return
         */
        public T get(final String name) {
            return name == null ? null : nameGetter.apply(name);
        }
    }
}
