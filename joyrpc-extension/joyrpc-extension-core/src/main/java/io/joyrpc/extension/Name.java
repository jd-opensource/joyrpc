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
 * 扩展名称
 */
public class Name<T, M> {
    /**
     * 类型
     */
    private final Class<T> clazz;
    /**
     * 名称
     */
    private final M name;

    public Name(Class<T> clazz) {
        this(clazz, null);
    }

    public Name(Class<T> clazz, M name) {
        this.clazz = clazz;
        this.name = name;
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public M getName() {
        return name;
    }

}
