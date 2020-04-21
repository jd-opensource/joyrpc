package io.joyrpc.example;

import io.joyrpc.config.ProviderConfig;
import io.joyrpc.config.RegistryConfig;
import io.joyrpc.config.ServerConfig;
import io.joyrpc.example.service.DemoService;
import io.joyrpc.example.service.DemoServiceImpl;
import org.junit.Test;

public class ConsulProviderStartTest {

    @Test
    public void exportDTest() throws InterruptedException {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(22000);
        ProviderConfig<DemoService> providerConfig = new ProviderConfig<>();
        providerConfig.setServerConfig(serverConfig);
        providerConfig.setRegistry(new RegistryConfig("consul", "127.0.0.1:8500"));
        //providerConfig.setRegistry(new RegistryConfig("memory"));
        providerConfig.setInterfaceClazz(DemoService.class.getName());
        providerConfig.setRef(new DemoServiceImpl());
        providerConfig.setAlias("test");
        providerConfig.exportAndOpen().whenComplete((v, t) -> {
            if (t != null) {
                t.printStackTrace();
                Thread.currentThread().interrupt();
            }
        });
        Thread.currentThread().join();
    }
}
