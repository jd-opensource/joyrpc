package io.joyrpc;

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

import io.joyrpc.extension.URL;

/**
 * 感知接口，初始化
 */
public interface InvokerAware {

    /**
     * 设置URL
     *
     * @param url
     */
    default void setUrl(URL url) {

    }

    /**
     * 代理类，泛型调用情况下，代理类和类名可能不一致
     *
     * @param clazz
     */
    default void setClass(Class clazz) {
    }

    /**
     * 设置真实的类名
     *
     * @param className
     */
    default void setClassName(String className) {

    }

    /**
     * 初始化
     */
    default void setup() {
    }
}
