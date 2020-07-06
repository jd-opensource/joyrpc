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
 * URL配置项，具有两个配置项名称
 *
 * @param <T>
 */
public class URLBiOption<T> extends Option<T> implements Cloneable {
    /**
     * 配置项名称
     */
    protected String name;
    /**
     * 候选配置项名称
     */
    protected String candidate;
    /**
     * 选项提供者
     */
    protected Supplier<T> supplier;

    /**
     * 默认构造函数
     */
    public URLBiOption() {
    }

    /**
     * 构造函数
     *
     * @param name  名称
     * @param value 值
     */
    public URLBiOption(String name, String candidate, T value) {
        super(value);
        this.name = name;
        this.candidate = candidate;
    }

    /**
     * 构造函数
     *
     * @param name
     * @param candidate
     * @param supplier
     */
    public URLBiOption(String name, String candidate, Supplier<T> supplier) {
        this.name = name;
        this.candidate = candidate;
        this.supplier = supplier;
    }

    /**
     * 构造函数
     *
     * @param name      名称
     * @param candidate 名称
     * @param value     值
     */
    public URLBiOption(URLKey name, URLKey candidate, T value) {
        super(value);
        this.name = name.getName();
        this.candidate = candidate.getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCandidate() {
        return candidate;
    }

    public void setCandidate(String candidate) {
        this.candidate = candidate;
    }

    @Override
    public T getValue() {
        if (value == null && supplier != null) {
            value = supplier.get();
        }
        return value;
    }

    @Override
    public URLBiOption<T> clone() {
        try {
            return (URLBiOption<T>) super.clone();
        } catch (CloneNotSupportedException ignored) {
            return null;
        }
    }
}
