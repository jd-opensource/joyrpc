package io.joyrpc.constants;

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
 * 头部键信息
 */
public class Head {

    /**
     * 值
     */
    protected byte key;
    /**
     * 类型
     */
    protected Class<?> type;

    /**
     * 构造函数
     *
     * @param key  键
     * @param type 值类型
     */
    public Head(final byte key, final Class<?> type) {
        this.key = key;
        this.type = type;
    }

    public byte getKey() {
        return key;
    }

    public Class<?> getType() {
        return type;
    }

}
