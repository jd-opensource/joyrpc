package io.joyrpc.proxy.bytebuddy;

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
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.proxy.ProxyFactory;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.InvocationHandler;

/**
 * The type Byte buddy proxy factory.
 *
 * @date: 1 /23/2019
 */
@Extension("bytebuddy")
@ConditionalOnClass("net.bytebuddy.ByteBuddy")
public class ByteBuddyProxyFactory implements ProxyFactory {

    /**
     * The Byte buddy.
     */
    protected static final ByteBuddy BYTE_BUDDY = new ByteBuddy(ClassFileVersion.ofThisVm(ClassFileVersion.JAVA_V8));

    @Override
    public <T> T getProxy(final Class<T> clz, final InvocationHandler invoker, final ClassLoader classLoader) throws ProxyException {
        Class clazz = BYTE_BUDDY.subclass(clz)
                .method(ElementMatchers.isDeclaredBy(clz))
                .intercept(MethodDelegation.to(new ByteBuddyInvocationHandler(invoker)))
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        try {
            return (T) clazz.newInstance();
        } catch (Exception e) {
            throw new ProxyException("Error occurred while creating bytebuddy proxy of " + clz.getName(), e);
        }

    }

}
