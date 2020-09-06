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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TimerTest {
    protected static final Logger logger = LoggerFactory.getLogger(TimerTest.class);

    @Test
    public void testTimeWheel() throws InterruptedException {

        int tickTime = 100;
        Timer timer = new Timer("default", tickTime, 5, 3);
        CountDownLatch latch = new CountDownLatch(6);
        timer.add("test", SystemClock.now(), new MyTask("1", latch));
        timer.add("test", SystemClock.now() + 200, new MyTask("2", latch));
        timer.add("test", SystemClock.now() + 200, new MyTask("3", latch));
        //1跳
        latch.await(tickTime, TimeUnit.MILLISECONDS);
        timer.add("test", SystemClock.now() + 600, new MyTask("4", latch));
        timer.add("test", SystemClock.now() + 1500, new MyTask("5", latch));
        timer.add("test", SystemClock.now() + 1500, new MyTask("6", latch));

        latch.await();
    }

    protected static class MyTask implements Runnable {
        protected String name;
        protected CountDownLatch latch;

        public MyTask(String name, CountDownLatch latch) {
            this.name = name;
            this.latch = latch;
        }

        @Override
        public void run() {
            logger.info(name + " is running " + SystemClock.now());
            latch.countDown();
        }
    }
}
