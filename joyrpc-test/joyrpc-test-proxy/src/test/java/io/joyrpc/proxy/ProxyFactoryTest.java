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

import io.joyrpc.Plugin;
import io.joyrpc.util.GrpcType;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

import static io.joyrpc.Plugin.PROXY;

/**
 * @date: 1 /23/2019
 */
public class ProxyFactoryTest {

    @Test
    public void testProxy() {
        List<String> types = PROXY.names();
        ProxyFactory factory;
        HelloService helloService;
        InvocationHandler handler = new MockProxyInvoker();
        for (String type : types) {
            factory = PROXY.get(type);
            helloService = factory.getProxy(HelloService.class, handler);
            helloService.sayHello("hello");
        }
    }

    @Test
    public void testTps() {

        List<String> types = PROXY.names();

        long startTime;
        long endTime;
        long totalTime;
        long count = 500000;
        ProxyFactory factory;
        HelloService helloService;
        InvocationHandler handler = new MockProxyInvoker();
        //性能测试
        for (String type : types) {
            factory = PROXY.get(type);
            totalTime = 0;
            helloService = factory.getProxy(HelloService.class, handler);
            for (int i = 0; i < count; i++) {
                startTime = System.nanoTime();
                helloService.sayHello("hello");
                endTime = System.nanoTime();
                totalTime += endTime - startTime;
            }
            System.out.println(String.format("%s time %d, tps %d", type, totalTime, (int) (1000000000.0 / totalTime * count)));
        }
    }

    @Test
    public void testGrpType() throws NoSuchMethodException, NoSuchFieldException {
        Method method = HelloService.class.getMethod("sayHello", String.class);
        for (GrpcFactory factory : Plugin.GRPC_FACTORY.extensions()) {
            GrpcType type = factory.generate(HelloService.class, method);
            Class<?> clazz = type.getRequest().getClazz();
            Field[] fields = clazz.getDeclaredFields();
            Assert.assertEquals(fields.length, 1);
            clazz = type.getResponse().getClazz();
            fields = clazz.getDeclaredFields();
            Assert.assertEquals(fields.length, 1);
        }
    }

    /**
     * The type Mock proxy invoker.
     */
    static class MockProxyInvoker implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "sayHello":
                    return args[0];
            }
            throw new IllegalArgumentException();
        }
    }
}
