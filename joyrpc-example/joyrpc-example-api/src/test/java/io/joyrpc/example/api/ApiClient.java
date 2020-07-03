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
import io.joyrpc.example.service.vo.EchoData;
import io.joyrpc.example.service.vo.EchoHeader;
import io.joyrpc.example.service.vo.EchoRequest;
import io.joyrpc.example.service.vo.EchoResponse;
import io.joyrpc.example.service.vo.Java8TimeObj;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Quick Start Client
 */
public class ApiClient {
    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);

    public static void main(String[] args) {
        /*ConsumerConfig<DemoService> consumerConfig = new ConsumerConfig<>(); //consumer设置
        consumerConfig.setInterfaceClazz(DemoService.class.getName());
        consumerConfig.setAlias("joyrpc-demo");
        //consumerConfig.setRegistry(new RegistryConfig("broadcast"));
        consumerConfig.setRegistry(new RegistryConfig("fix", "127.0.0.1:22000"));
        consumerConfig.setSerialization("json");
        consumerConfig.setTimeout(50000);
        try {
            CompletableFuture<DemoService> future = consumerConfig.refer();
            DemoService service = future.get();

            EchoResponse response = service.echoRequest(new EchoRequest<>(new EchoHeader(), new EchoData(1,"msg 1")));

            System.out.println(response);
            System.in.read();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }*/
        int i = 5;
        int j = 10;
        System.out.println(i/j);
    }
}
