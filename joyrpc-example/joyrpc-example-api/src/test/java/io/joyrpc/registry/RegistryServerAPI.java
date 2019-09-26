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

import io.joyrpc.config.ProviderConfig;
import io.joyrpc.config.RegistryConfig;
import io.joyrpc.service.DemoService;
import io.joyrpc.service.impl.DemoServiceImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Quick Start Server
 */
public class RegistryServerAPI {

    public static void main(String[] args) throws Exception {
        DemoService demoService = new DemoServiceImpl();
        /**
         * 服务发布到A、B两个注册中心
         */
        RegistryConfig joyrpcRegistryA = new RegistryConfig("zk", "192.168.1.100:2181");// 注册中心A
        RegistryConfig joyrpcRegistryB = new RegistryConfig("zk", "192.168.1.101:2181");// 注册中心B
        List<RegistryConfig> list = new ArrayList<>();
        list.add(joyrpcRegistryA);
        list.add(joyrpcRegistryB);
        ProviderConfig<DemoService> providerConfig = new ProviderConfig<DemoService>();
        providerConfig.setRegistry(list);

        providerConfig.setInterfaceClazz("io.joyrpc.service.DemoService");
        providerConfig.setRef(demoService);
        providerConfig.setAlias("joyrpc-demo");

        providerConfig.export().whenComplete((v, t) -> {//发布服务
            providerConfig.open();
        });
        System.in.read();
    }
}
