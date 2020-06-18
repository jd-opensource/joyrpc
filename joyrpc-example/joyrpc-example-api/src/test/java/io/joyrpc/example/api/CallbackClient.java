package io.joyrpc.example.api;

import io.joyrpc.Callback;
import io.joyrpc.config.ConsumerConfig;
import io.joyrpc.config.RegistryConfig;
import io.joyrpc.example.service.CallbackService;
import io.joyrpc.example.service.vo.*;
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

        callbackService.echoRequestCallback(new RequestCallback());

        callbackService.echoGenericRequestCallback(new GenericRequestCallback());

        callbackService.echoDataRequestCallback(new DataRequestCallback());

        callbackService.echoGenericListener(new GenericListener());

        callbackService.echoRequestListener(new RequestListener());


        System.in.read();
    }

    public static class GenericCallback implements Callback {

        @Override
        public Object notify(Object result) {
            logger.info("GenericCallback receive: " + result);
            return result;
        }
    }

    public static class RequestCallback implements Callback<EchoRequest<EchoData>, EchoResponse<EchoData>> {

        @Override
        public EchoResponse<EchoData> notify(EchoRequest<EchoData> request) {
            logger.info("RequestCallback receive: " + request);
            return new EchoResponse<>(request.getHeader(), request.getBody());
        }
    }

    public static class GenericRequestCallback implements Callback<EchoRequest, EchoResponse> {

        @Override
        public EchoResponse notify(EchoRequest request) {
            logger.info("GenericRequestCallback receive: " + request);
            return new EchoResponse<>(request.getHeader(), request.getBody());
        }
    }

    public static class DataRequestCallback implements Callback<EchoDataRequest, EchoDataResponse> {

        @Override
        public EchoDataResponse notify(EchoDataRequest request) {
            logger.info("GenericRequestCallback receive: " + request);
            return new EchoDataResponse(request.getHeader(), request.getBody());
        }
    }

    public static class GenericListener implements CallbackService.Listener {

        @Override
        public Object notify(Object result) {
            logger.info("GenericListener receive: " + result);
            return result;
        }
    }

    public static class RequestListener implements CallbackService.Listener<EchoRequest<EchoData>, EchoResponse<EchoData>> {

        @Override
        public EchoResponse<EchoData> notify(EchoRequest<EchoData> request) {
            logger.info("RequestListener receive: " + request);
            return new EchoResponse<>(request.getHeader(), request.getBody());
        }
    }
}
