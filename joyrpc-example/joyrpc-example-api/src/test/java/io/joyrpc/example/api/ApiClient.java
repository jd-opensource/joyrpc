package io.joyrpc.example.api;

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

import io.joyrpc.config.ConsumerConfig;
import io.joyrpc.config.RegistryConfig;
import io.joyrpc.example.service.DemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Quick Start Client
 */
public class ApiClient {
    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);

    public static void main(String[] args) {
        ConsumerConfig<DemoService> consumerConfig = new ConsumerConfig<>(); //consumer设置
        consumerConfig.setInterfaceClazz(DemoService.class.getName());
        consumerConfig.setAlias("joyrpc-demo");
        consumerConfig.setRegistry(new RegistryConfig("broadcast"));
        try {
            CompletableFuture<DemoService> future = consumerConfig.refer();
            DemoService service = future.get();

            String echo = service.sayHello("hello"); //发起服务调用
            logger.info("Get msg: {} ", echo);

            System.in.read();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }
}
