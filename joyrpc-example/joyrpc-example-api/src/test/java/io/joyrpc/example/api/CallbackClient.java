package io.joyrpc.example.api;

import io.joyrpc.Callback;
import io.joyrpc.config.ConsumerConfig;
import io.joyrpc.config.RegistryConfig;
import io.joyrpc.example.service.CallbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class CallbackClient {

    private static final Logger logger = LoggerFactory.getLogger(CallbackClient.class);

    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        RegistryConfig registryConfig = new RegistryConfig("fix", "127.0.0.1:22020");

        ConsumerConfig<CallbackService> consumerConfig = new ConsumerConfig<>(); //consumer设置
        consumerConfig.setInterfaceClazz(CallbackService.class.getName());
        consumerConfig.setAlias("joyrpc-demo");
        consumerConfig.setRegistry(registryConfig);

        CallbackService callbackService = consumerConfig.refer().get();

        callbackService.echoGenericCallback(new GenericCallback());


        System.in.read();
    }

    public static class GenericCallback implements Callback {

        @Override
        public Object notify(Object result) {
            logger.info("GenericCallback receive: " + result);
            return result;
        }
    }
}
