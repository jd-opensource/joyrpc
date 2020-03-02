package io.joyrpc.example.boot;

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

import io.joyrpc.context.RequestContext;
import io.joyrpc.example.service.AsyncDemoService;
import io.joyrpc.exception.NoAliveProviderException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

@SpringBootApplication
public class BootAsyncClient {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "async-client");
        ConfigurableApplicationContext run = SpringApplication.run(BootAsyncClient.class, args);
        AsyncDemoService consumer = run.getBean(AsyncDemoService.class);
        AtomicLong counter = new AtomicLong(0);
        while (true) {
            try {
                long value = counter.incrementAndGet();
                RequestContext.getContext().setAttachment("counter", value);
                consumer.sayHello("helloWold").whenComplete(new MyConsumer(value, Thread.currentThread()));
                Thread.sleep(200L);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    protected static class MyConsumer implements BiConsumer<String, Throwable> {
        protected long counter;
        protected Thread thread;

        public MyConsumer(long counter, Thread thread) {
            this.counter = counter;
            this.thread = thread;
        }

        @Override
        public void accept(String s, Throwable throwable) {
            long cnt = RequestContext.getContext().getAttachment("counter");
            if (throwable == null) {
                System.out.println("thread switch:" + (Thread.currentThread() != thread) + ",counter:" + cnt + ",response:" + s);
            } else if (throwable instanceof NoAliveProviderException) {
                System.out.println("thread switch:" + (Thread.currentThread() != thread) + ",counter:" + cnt + ",error:" + throwable.getMessage());
            } else {
                throwable.printStackTrace();
            }
        }
    }
}

