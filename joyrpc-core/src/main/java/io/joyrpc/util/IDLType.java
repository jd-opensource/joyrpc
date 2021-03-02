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

/**
 * 接口描述语言产生的类型描述
 */
public class IDLType {

    /**
     * 请求类型
     */
    protected final Class<?> clazz;
    /**
     * 包装请求
     */
    protected final boolean wrapper;
    /**
     * 转换函数
     */
    protected final IDLConverter conversion;

    /**
     * 构造函数
     *
     * @param clazz   类型
     * @param wrapper 包装类型标识
     */
    public IDLType(Class<?> clazz, boolean wrapper) {
        this.clazz = clazz;
        this.wrapper = wrapper;
        this.conversion = wrapper ? ClassUtils.getConversion(clazz) : null;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public boolean isWrapper() {
        return wrapper;
    }

    public IDLConverter getConversion() {
        return conversion;
    }
}
