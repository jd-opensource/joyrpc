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

import java.lang.reflect.Constructor;

/**
 * 泛型构造函数
 */
public class GenericConstructor extends GenericExecutable<Constructor> {

    /**
     * 构造函数
     *
     * @param method     构造函数
     * @param parameters 泛型参数
     * @param exceptions 泛型异常
     */
    public GenericConstructor(final Constructor method, final GenericType[] parameters, final GenericType[] exceptions) {
        super(method, parameters, exceptions);
    }
}
