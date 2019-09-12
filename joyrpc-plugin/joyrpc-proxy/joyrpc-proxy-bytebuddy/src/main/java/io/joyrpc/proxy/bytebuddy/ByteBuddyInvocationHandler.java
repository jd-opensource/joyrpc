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


import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * The type Byte buddy invocation handler.
 *
 * @date: 2 /19/2019
 */
public class ByteBuddyInvocationHandler {


    /**
     * The Invoker.
     */
    protected InvocationHandler invoker;

    /**
     * Instantiates a new Byte buddy invocation handler.
     *
     * @param invoker the invoker
     */
    public ByteBuddyInvocationHandler(InvocationHandler invoker) {
        this.invoker = invoker;
    }

    /**
     * Invoke object.
     *
     * @param proxy  the proxy
     * @param method the method
     * @param param  the param
     * @return the object
     * @throws Throwable the throwable
     */
    @RuntimeType
    public Object invoke(@This final Object proxy, @Origin final Method method, @AllArguments final Object[] param) throws Throwable {
        return invoker.invoke(proxy, method, param);
    }


}
