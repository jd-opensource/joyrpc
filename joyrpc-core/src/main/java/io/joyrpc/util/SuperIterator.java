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

import java.util.Iterator;
import java.util.function.Predicate;

/**
 * 类及超类迭代器
 */
public class SuperIterator implements Iterator<Class<?>> {
    /**
     * 遍历的类
     */
    protected Class<?> clazz;
    /**
     * 判断
     */
    protected Predicate<Class<?>> predicate;

    /**
     * 构造函数
     *
     * @param clazz
     */
    public SuperIterator(Class<?> clazz) {
        this(clazz, null);
    }

    /**
     * 构造函数
     *
     * @param clazz
     * @param predicate
     */
    public SuperIterator(Class<?> clazz, Predicate<Class<?>> predicate) {
        this.clazz = clazz;
        this.predicate = predicate == null ? o -> o.equals(Object.class) : predicate;
    }

    @Override
    public boolean hasNext() {
        return clazz != null && !predicate.test(clazz);
    }

    @Override
    public Class<?> next() {
        Class<?> result = clazz;
        clazz = clazz.getSuperclass();
        return result;
    }

    @Override
    public void remove() {

    }
}
