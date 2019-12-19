package io.joyrpc.util;

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
