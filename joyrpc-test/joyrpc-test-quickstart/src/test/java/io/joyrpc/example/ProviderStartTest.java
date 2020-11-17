package io.joyrpc.example;

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
import io.joyrpc.example.service.DemoService;
import io.joyrpc.example.service.DemoServiceImpl;
import org.junit.jupiter.api.Test;

public class ProviderStartTest {

    @Test
    public void exportTest() throws InterruptedException {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(22000);

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setRegistry("broadcast");

        DemoService demoService = new DemoServiceImpl();

        ProviderConfig<DemoService> providerConfig = new ProviderConfig<DemoService>();
        providerConfig.setServerConfig(serverConfig);
        providerConfig.setRegistry(registryConfig);
        providerConfig.setInterfaceClazz(DemoService.class.getName());
        providerConfig.setRef(demoService);
        providerConfig.setAlias("JOY-DEMO");
        providerConfig.exportAndOpen().whenComplete((v, t) -> {
            if (t != null) {
                t.printStackTrace();
                Thread.currentThread().interrupt();
            }
        });

        Thread.currentThread().join();
    }

    @Test
    public void exportTest1() throws InterruptedException {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(22001);

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setRegistry("broadcast");

        DemoService demoService = new DemoServiceImpl();

        ProviderConfig<DemoService> providerConfig = new ProviderConfig<DemoService>();
        providerConfig.setServerConfig(serverConfig);
        providerConfig.setRegistry(registryConfig);
        providerConfig.setInterfaceClazz(DemoService.class.getName());
        providerConfig.setRef(demoService);
        providerConfig.setAlias("JOY-DEMO");
        providerConfig.exportAndOpen().whenComplete((v, t) -> {
            if (t != null) {
                t.printStackTrace();
                Thread.currentThread().interrupt();
            }
        });

        Thread.currentThread().join();
    }
}
