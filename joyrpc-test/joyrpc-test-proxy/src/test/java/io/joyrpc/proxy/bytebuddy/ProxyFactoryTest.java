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

import io.joyrpc.proxy.ProxyFactory;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

/**
 * @date: 1 /23/2019
 */
public class ProxyFactoryTest {

    /**
     * The Proxy factory.
     */
    ProxyFactory proxyFactory;
    /**
     * The List.
     */
    List list = new ArrayList();
    /**
     * The Map.
     */
    Map map = new HashMap();
    /**
     * The Hello.
     */
    static Hello hello = new Hello();
    /**
     * The World.
     */
    World world = new World();
    /**
     * The Exception.
     */
    Exception exception = new Exception();
    /**
     * The S.
     */
    String s = "sayHello";

    /**
     * Sets up.
     *
     * @throws Exception the exception
     */

    public ProxyFactoryTest() {

    }

    @Before
    public void setUp() throws Exception {
        list.add(hello);
        map.put("hello", hello);
        proxyFactory = new ByteBuddyProxyFactory();
    }

    /**
     * Gets byte buddy proxy.
     */
    @Test
    public void getByteBuddyProxy() {
        long time = System.currentTimeMillis();
        HelloService helloService = proxyFactory.getProxy(HelloService.class, new MockProxyInvoker());
        time = System.currentTimeMillis() - time;
        System.out.println(proxyFactory.getClass() + ": " + time + " ms");
        assertEquals(s, helloService.sayHello());
        assertEquals(hello, helloService.getHello(1L));
        assertEquals(list, helloService.searchHello("hello", 1, 1));
        assertEquals(map, helloService.buildDict(list));
        try {
            assertEquals("throwException", helloService.throwException(map));
        } catch (Exception e) {

        }
    }

    /**
     * Test thread safe proxy.
     *
     * @throws Exception the exception
     */
    @Test
    public void testThreadSafeProxy() throws Exception {

        ThreadRun(proxyFactory);
    }

    /**
     * Thread run.
     *
     * @param proxy the proxy
     * @throws InterruptedException the interrupted exception
     */
    public void ThreadRun(ProxyFactory proxy) throws InterruptedException {
        int numberOfCatchers = 10000;
        CountDownLatch down = new CountDownLatch(numberOfCatchers);
        ExecutorService executor = Executors.newFixedThreadPool(10);

        long l = System.nanoTime();
        for (int i = 0; i < numberOfCatchers; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    proxy.getProxy(HelloService.class, new MockProxyInvoker());
                    down.countDown();
                }
            });

        }
        down.await();
        System.out.println(proxy.getClass().getName() + " create proxy:" + (System.nanoTime() - l) / 1000000 + "." + (System.nanoTime() - l) % 1000000);
//
        HelloService helloService = proxy.getProxy(HelloService.class, new MockProxyInvoker());
        CountDownLatch down1 = new CountDownLatch(numberOfCatchers);
        Long ll = System.nanoTime();
        for (int i = 0; i < numberOfCatchers; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    assertEquals(s, helloService.sayHello());
                    assertEquals(hello, helloService.getHello(1L));
                    assertEquals(list, helloService.searchHello("hello", 1, 1));
                    assertEquals(map, helloService.buildDict(list));
                    try {
                        assertEquals("throwException", helloService.throwException(map));
                    } catch (Exception e) {

                    }
                    down1.countDown();
                }
            });

        }
        down1.await();
        System.out.println(proxy.getClass().getName() + "  execute method:" + (System.nanoTime() - ll) / 1000000 + "." + (System.nanoTime() - ll) % 1000000);
    }

    /**
     * The type Mock proxy invoker.
     */
    class MockProxyInvoker implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (proxy instanceof HelloService) {
                switch (method.getName()) {
                    case "sayHello":
                        return "sayHello";
                    case "throwException":
                        return "throwException";
                    case "saveHello":
                        return null;
                    case "getHello":
                        return hello;
                    case "searchHello":
                        return list;
                    case "buildDict":
                        return map;
                    default:
                        break;
                }
            }
            return "Proxy must be HelloService";
        }
    }
}
