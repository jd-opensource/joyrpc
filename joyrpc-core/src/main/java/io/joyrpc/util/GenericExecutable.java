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

import java.lang.reflect.Executable;

/**
 * 泛型方法
 */
public abstract class GenericExecutable<T extends Executable> {

    /**
     * 方法
     */
    protected T method;

    /**
     * 参数泛型
     */
    protected GenericType[] parameters;
    /**
     * 异常
     */
    protected GenericType[] exceptions;


    /**
     * 构造函数
     *
     * @param method
     * @param parameters
     * @param exceptions
     */
    public GenericExecutable(final T method, final GenericType[] parameters, final GenericType[] exceptions) {
        this.method = method;
        this.parameters = parameters;
        this.exceptions = exceptions;
    }

    public T getMethod() {
        return method;
    }

    public GenericType[] getParameters() {
        return parameters;
    }

    public GenericType[] getExceptions() {
        return exceptions;
    }
}
