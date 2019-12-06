package io.joyrpc.example.rest;

import io.joyrpc.config.ProviderConfig;
import io.joyrpc.config.RegistryConfig;
import io.joyrpc.config.ServerConfig;
import io.joyrpc.example.service.impl.RestDemoServiceImpl;
import io.joyrpc.example.service.DemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RestServer {

    private static final Logger logger = LoggerFactory.getLogger(RestServer.class);

    public static void main(String[] args) throws IOException {
        ServerConfig restServer = new ServerConfig();
        restServer.setHost("127.0.0.1");
        restServer.setPort(9900);
        restServer.setEndpointFactory("resteasy");


        DemoService demoService = new RestDemoServiceImpl(); //服务提供者设置
        ProviderConfig<DemoService> providerConfig = new ProviderConfig<>();
        providerConfig.setServerConfig(new ServerConfig());
        providerConfig.setInterfaceClazz(DemoService.class.getName());
        providerConfig.setRef(demoService);
        providerConfig.setAlias("joyrpc-demo");
        providerConfig.setRegistry(new RegistryConfig("memory"));
        providerConfig.setServerConfig(restServer);

        providerConfig.exportAndOpen().whenComplete((v, t) -> {
            if (t != null) {
                logger.error(t.getMessage(), t);
                System.exit(1);
            }
        });
        System.in.read();
    }
}
