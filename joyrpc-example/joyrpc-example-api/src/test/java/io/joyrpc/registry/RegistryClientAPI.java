package io.joyrpc.registry;

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
import io.joyrpc.service.DemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Quick Start client
 */
public class RegistryClientAPI {
    private static final Logger logger = LoggerFactory.getLogger(RegistryClientAPI.class);

    public static void main(String[] args) {
        RegistryConfig joyrpcRegistry = new RegistryConfig("broadcast", "127.0.0.1:6702");// 注册中心

        ConsumerConfig<DemoService> consumerConfig = new ConsumerConfig<>(); //consumer设置
        consumerConfig.setInterfaceClazz("io.joyrpc.service.DemoService");
        consumerConfig.setAlias("joyrpc-demo");
        consumerConfig.setRegistry(joyrpcRegistry);
        try {
            CompletableFuture<DemoService> future = consumerConfig.refer();
            DemoService service = future.get();

            String echo = service.sayHello("hello"); //发起服务调用
            logger.info("Get msg: {}", echo);

            System.in.read();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
