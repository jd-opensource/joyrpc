package io.joyrpc.quickstart;

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
import io.joyrpc.config.ServerConfig;
import io.joyrpc.service.DemoService;
import io.joyrpc.service.impl.DemoServiceImpl;

/**
 * Quick Start Server
 */
public class ServerAPI {

    public static void main(String[] args) throws Exception {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setRegistry("memory");//内存注册中心

        DemoService demoService = new DemoServiceImpl(); //服务提供者设置
        ProviderConfig<DemoService> providerConfig = new ProviderConfig<>();
        providerConfig.setServerConfig(new ServerConfig());

        providerConfig.setInterfaceClazz("io.joyrpc.service.DemoService");
        providerConfig.setRef(demoService);
        providerConfig.setAlias("joyrpc-demo");
        providerConfig.setRegistry(registryConfig);

        providerConfig.export().whenComplete((v, t) -> {//发布服务
            providerConfig.open();
        });
        System.in.read();
    }
}
