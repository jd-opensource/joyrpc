package io.joyrpc.extension;

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

import java.util.function.Supplier;

/**
 * URL配置项
 *
 * @param <T>
 */
public class URLOption<T> extends Option<T> implements Cloneable {
    /**
     * 配置项名称
     */
    protected String name;
    /**
     * 选项提供者
     */
    protected Supplier<T> supplier;

    /**
     * 默认构造函数
     */
    public URLOption() {
    }

    /**
     * 构造函数
     *
     * @param name  名称
     * @param value 值
     */
    public URLOption(String name, T value) {
        super(value);
        this.name = name;
    }

    /**
     * 构造函数
     *
     * @param name
     * @param supplier
     */
    public URLOption(String name, Supplier<T> supplier) {
        this.name = name;
        this.supplier = supplier;
    }

    /**
     * 构造函数
     *
     * @param name  名称
     * @param value 值
     */
    public URLOption(URLKey name, T value) {
        super(value);
        this.name = name.getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public T getValue() {
        if (value == null && supplier != null) {
            value = supplier.get();
        }
        return value;
    }

    @Override
    public URLOption<T> clone() {
        try {
            return (URLOption<T>) super.clone();
        } catch (CloneNotSupportedException ignored) {
            return null;
        }
    }
}
