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

import io.joyrpc.example.service.DemoService;
import io.joyrpc.exception.NoAliveProviderException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
public class BootClient {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "client");
        ConfigurableApplicationContext run = SpringApplication.run(BootClient.class, args);
        DemoService consumer = run.getBean(DemoService.class);
        AtomicLong counter = new AtomicLong(0);
        while (true) {
            try {
                String hello = consumer.generic(String.valueOf(counter.incrementAndGet()));
                System.out.println(hello);
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                break;
            } catch (Throwable e) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                }
                if (e instanceof NoAliveProviderException) {
                    System.out.println(e.getMessage());
                } else {
                    e.printStackTrace();
                }
            }
        }
    }
}

