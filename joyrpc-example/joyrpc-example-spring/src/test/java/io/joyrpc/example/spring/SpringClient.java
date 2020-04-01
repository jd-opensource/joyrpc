package io.joyrpc.example.spring;

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
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Quick Start client
 */
public class SpringClient {

    public static void main(String[] args) throws Throwable {
        ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext("classpath:spring/joyrpc-consumer.xml");
        DemoService consumer = (DemoService) appContext.getBean("demoService");

        while (true) {
            System.out.println(consumer.sayHello("helloWold"));
            Thread.sleep(1000L);
        }

    }
}
