package io.joyrpc.proxy;

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

import io.joyrpc.exception.ProxyException;
import io.joyrpc.extension.Extensible;
import io.joyrpc.util.ClassUtils;

import java.lang.reflect.InvocationHandler;

/**
 * The interface Proxy factory.
 */
@Extensible("proxy")
public interface ProxyFactory {

    /**
     * Gets proxy.
     *
     * @param <T>     the type parameter
     * @param clz     the clz
     * @param invoker the invoker
     * @return the proxy
     * @throws ProxyException
     */
    default <T> T getProxy(final Class<T> clz, final InvocationHandler invoker) throws ProxyException {
        return getProxy(clz, invoker, ClassUtils.getCurrentClassLoader());
    }

    /**
     * Gets proxy.
     *
     * @param <T>         the type parameter
     * @param clz         the clz
     * @param invoker     the invoker
     * @param classLoader the class loader
     * @return the proxy
     * @throws ProxyException
     */
    <T> T getProxy(Class<T> clz, InvocationHandler invoker, ClassLoader classLoader) throws ProxyException;

}
