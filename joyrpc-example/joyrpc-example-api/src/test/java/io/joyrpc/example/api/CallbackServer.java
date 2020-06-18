package io.joyrpc.example.api;

import io.joyrpc.config.ProviderConfig;
import io.joyrpc.config.RegistryConfig;
import io.joyrpc.config.ServerConfig;
import io.joyrpc.example.service.CallbackService;
import io.joyrpc.example.service.impl.CallbackServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CallbackServer {

    private static final Logger logger = LoggerFactory.getLogger(CallbackServer.class);

    public static void main(String[] args) throws IOException {
        RegistryConfig registryConfig = new RegistryConfig("memory");

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(22020);

        ProviderConfig<CallbackService> providerConfig = new ProviderConfig<>();
        providerConfig.setServerConfig(new ServerConfig());
        providerConfig.setInterfaceClazz(CallbackService.class.getName());
        providerConfig.setRef(new CallbackServiceImpl());
        providerConfig.setAlias("joyrpc-demo");
        providerConfig.setServerConfig(serverConfig);
        providerConfig.setRegistry(registryConfig);

        providerConfig.exportAndOpen().whenComplete((v, t) -> {
            if (t != null) {
                logger.error(t.getMessage(), t);
                System.exit(1);
            }
        });


        providerConfig.setParameter("shutdownTimeout", 15000);
        System.in.read();
    }
}
