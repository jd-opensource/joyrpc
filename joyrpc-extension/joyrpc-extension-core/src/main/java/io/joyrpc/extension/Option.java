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

/**
 * 选项
 *
 * @param <T>
 */
public class Option<T> {
    /**
     * 值
     */
    T value;

    /**
     * 构造函数
     */
    public Option() {
    }

    /**
     * 构造函数
     *
     * @param value
     */
    public Option(T value) {
        this.value = value;
    }

    /**
     * 获取值
     *
     * @return
     */
    public T get() {
        return getValue();
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }


    /**
     * 选项值提供者
     *
     * @param <T>
     */
    public interface OptionSupplier<T> {

        /**
         * 获取值
         *
         * @return
         */
        T get();
    }

}
