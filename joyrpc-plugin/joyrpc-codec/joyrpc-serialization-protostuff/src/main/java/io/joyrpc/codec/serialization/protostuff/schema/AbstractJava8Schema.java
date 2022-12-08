package io.joyrpc.codec.serialization.protostuff.schema;

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

import io.joyrpc.exception.SerializerException;
import io.protostuff.Schema;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public abstract class AbstractJava8Schema<T> implements Schema<T> {

    protected Class<T> clazz;

    public AbstractJava8Schema(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public boolean isInitialized(final T message) {
        return true;
    }

    @Override
    public String messageName() {
        return clazz.getSimpleName();
    }

    @Override
    public String messageFullName() {
        return clazz.getName();
    }

    @Override
    public Class<? super T> typeClass() {
        return clazz;
    }

    /**
     * 设置值
     *
     * @param field
     * @param target
     * @param value
     */
    protected static void setValue(final Field field, final Object target, final Object value) {
        try {
            field.set(target, value);
        } catch (Exception e) {
            throw new SerializerException("Error occurs while setting field " + field.getName() + " of "
                    + field.getDeclaringClass().getName(), e);
        }
    }

    /**
     * 获取值
     *
     * @param field
     * @param target
     */
    protected static Object getValue(final Field field, final Object target) {
        try {
            return field.get(target);
        } catch (Exception e) {
            throw new SerializerException("Error occurs while getting field " + field.getName() + " of "
                    + field.getDeclaringClass().getName(), e);
        }
    }

    /**
     * 获取字段
     *
     * @param clazz
     * @param name
     * @return
     */
    protected static Field getWriteableField(final Class clazz, final String name) {
        try {
            Field result = clazz.getDeclaredField(name);
            result.setAccessible(true);
            return result;
        } catch (Exception e) {
            throw new SerializerException("Error occurs while getting field " + name + " of " + clazz.getName(), e);
        }
    }

    /**
     * 获取方法
     *
     * @param clazz
     * @param name
     * @param types
     * @return
     */
    protected static Method getMethod(final Class clazz, final String name, final Class<?>... types) {
        try {
            Method result = clazz.getDeclaredMethod(name, types);
            result.setAccessible(true);
            return result;
        } catch (Exception e) {
            throw new SerializerException("Error occurs while getting method " + name + " of " + clazz.getName(), e);
        }
    }

    /**
     * 调用方法
     *
     * @param method
     * @param target
     * @param values
     * @return
     */
    protected static Object invoke(final Method method, final Object target, final Object... values) {
        try {
            return method.invoke(target, values);
        } catch (Exception e) {
            throw new SerializerException("Error occurs while invoking method " + method.getName() + " of " + method.getDeclaringClass().getName(), e);
        }
    }

}
