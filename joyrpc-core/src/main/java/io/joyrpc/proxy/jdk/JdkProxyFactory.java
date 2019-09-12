package io.joyrpc.proxy.jdk;

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
import io.joyrpc.extension.Extension;
import io.joyrpc.proxy.ProxyFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;


/**
 * The type JDK proxy factory.
 */
@Extension("jdk")
public class JdkProxyFactory implements ProxyFactory {

    @Override
    public <T> T getProxy(final Class<T> clz, final InvocationHandler invoker, final ClassLoader classLoader) throws ProxyException {
        try {
            return (T) Proxy.newProxyInstance(classLoader, new Class[]{clz}, invoker);
        } catch (IllegalArgumentException e) {
            throw new ProxyException(e.getMessage(), e);
        }
    }
}
