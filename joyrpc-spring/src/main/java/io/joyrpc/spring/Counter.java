package io.joyrpc.spring;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 计数器，用于保证上下文、消费者和服务提供者的启动顺序
 */
public class Counter {
    /**
     * slf4j logger for this class
     */
    private static final Logger logger = LoggerFactory.getLogger(Counter.class);

    /**
     * 上下文Bean数量
     */
    protected static final AtomicInteger CONTEXT_BEANS = new AtomicInteger();
    /**
     * 消费者计数器
     */
    protected static final AtomicInteger CONSUMER_BEANS = new AtomicInteger(0);
    /**
     * 服务提供者计数器
     */
    protected static final AtomicInteger PROVIDER_BEANS = new AtomicInteger(0);
    /**
     * 启动的计数器
     */
    protected static final AtomicInteger STARTING_BEANS = new AtomicInteger(0);
    /**
     * 未启动成功计数器
     */
    protected static final AtomicInteger UNSUCCESS_BEANS = new AtomicInteger(0);
    /**
     * 未启动成功的上下文计数器
     */
    protected static final AtomicInteger UNSUCCESS_CONTEXT_BEANS = new AtomicInteger(0);
    /**
     * 未启动成功的消费者计数器
     */
    protected static final AtomicInteger UNSUCCESS_CONSUMER_BEANS = new AtomicInteger(0);
    /**
     * 未启动成功的服务提供者计数器
     */
    protected static final AtomicInteger UNSUCCESS_PROVIDER_BEANS = new AtomicInteger(0);
    /**
     * 等待所有启动成功
     */
    protected static final CountDownLatch LATCH = new CountDownLatch(1);

    /**
     * 是否有消费者
     *
     * @return 消费者标识
     */
    public static boolean hasConsumer() {
        return CONSUMER_BEANS.get() > 0;
    }

    /**
     * 是否有上下文
     *
     * @return 上下文标识
     */
    public static boolean hasContext() {
        return CONTEXT_BEANS.get() > 0;
    }

    /**
     * 启动
     */
    public static void startBean() {
        if (STARTING_BEANS.decrementAndGet() == 0) {
            try {
                LATCH.await();
            } catch (InterruptedException e) {
                //出了异常
                logger.error(String.format("The system is about to exit, caused by %s", e.getMessage()));
                System.exit(1);
            }
        }
    }

    /**
     * 添加上下文计数器
     *
     * @return
     */
    public static int incContext() {
        int result = CONTEXT_BEANS.incrementAndGet();
        UNSUCCESS_CONTEXT_BEANS.incrementAndGet();
        return result;
    }

    /**
     * 添加消费者计数器
     */
    public static void incConsumer() {
        STARTING_BEANS.incrementAndGet();
        UNSUCCESS_BEANS.incrementAndGet();
        CONSUMER_BEANS.incrementAndGet();
        UNSUCCESS_CONSUMER_BEANS.incrementAndGet();
    }

    /**
     * 添加消费者计数器
     */
    public static void incProvider() {
        STARTING_BEANS.incrementAndGet();
        UNSUCCESS_BEANS.incrementAndGet();
        PROVIDER_BEANS.incrementAndGet();
        UNSUCCESS_PROVIDER_BEANS.incrementAndGet();
    }

    /**
     * 成功启动上下文参数
     */
    public static void successContext(final Runnable runnable) {
        if (UNSUCCESS_CONTEXT_BEANS.decrementAndGet() == 0) {
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    /**
     * 成功启动计数器
     */
    public static void successConsumer(final Runnable runnable) {
        if (UNSUCCESS_CONSUMER_BEANS.decrementAndGet() == 0) {
            if (runnable != null) {
                runnable.run();
            }
        }
        if (UNSUCCESS_BEANS.decrementAndGet() == 0) {
            LATCH.countDown();
        }
    }

    /**
     * 成功启动计数器
     *
     * @param runnable 执行器
     */
    public static void successProvider(final Runnable runnable) {
        if (UNSUCCESS_PROVIDER_BEANS.decrementAndGet() == 0) {
            if (runnable != null) {
                runnable.run();
            }
        }
        if (UNSUCCESS_BEANS.decrementAndGet() == 0) {
            LATCH.countDown();
        }
    }
}
