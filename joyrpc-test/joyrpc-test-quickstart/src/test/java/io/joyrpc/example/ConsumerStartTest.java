package io.joyrpc.example;

import io.joyrpc.config.ConsumerConfig;
import io.joyrpc.config.RegistryConfig;
import io.joyrpc.example.service.DemoService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

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

public class ConsumerStartTest {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerStartTest.class);

    @Test
    public void referTest() throws ExecutionException, InterruptedException {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setRegistry("broadcast");

        ConsumerConfig<DemoService> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setRegistry(registryConfig);
        consumerConfig.setInterfaceClazz(DemoService.class.getName());
        consumerConfig.setAlias("JOY-DEMO");
        consumerConfig.setTimeout(500000);
        DemoService demoService = consumerConfig.refer().get();

        while (true) {
            try {
                String res = demoService.sayHello("joyrpc");
                System.out.println("res==" + res);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Thread.sleep(2000);
            }
        }

    }
}
