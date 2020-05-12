package io.joyrpc.example.service.impl;

import io.joyrpc.example.service.DemoService;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.rpc.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service(version = "0.0.0", group = "2.0-Boot", registry = {"my-registry"})
public class DemoServiceImpl implements DemoService {

    private static final Logger logger = LoggerFactory.getLogger(DemoServiceImpl.class);

    @Override
    public String sayHello(String name) {
        logger.info("Receive " + name + ", request from consumer: " + RpcContext.getContext().getRemoteAddress());
        return "Hello " + name + ", response from provider: " + RpcContext.getContext().getLocalAddress();
    }

    @Override
    public int test(int count) {
        return count;
    }

    @Override
    public <T> T generic(T value) {
        return value;
    }
}
