package io.joyrpc.example.dubbo.service.impl;

import io.joyrpc.annotation.Provider;
import io.joyrpc.context.RequestContext;
import io.joyrpc.example.dubbo.service.DubboDemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider(name = "provider-dubboDemo-service")
public class DubboDemoServiceImpl implements DubboDemoService {

    private static final Logger logger = LoggerFactory.getLogger(DubboDemoServiceImpl.class);

    @Override
    public String sayHello(String name) {
        logger.info("Receive " + name + ", request from consumer: " + RequestContext.getContext().getRemoteAddress());
        return "Hello " + name + ", response from provider: " + RequestContext.getContext().getLocalAddress();
    }
}
