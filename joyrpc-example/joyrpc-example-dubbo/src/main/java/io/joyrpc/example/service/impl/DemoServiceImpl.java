package io.joyrpc.example.service.impl;

import io.joyrpc.example.service.DemoService;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.rpc.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service(version = "0.0.0", group = "2.0-Boot", registry = {"my-registry"})
public class DemoServiceImpl implements DemoService {

    private Set<EchoCallback> callbacks = new HashSet<>();

    private static final Logger logger = LoggerFactory.getLogger(DemoServiceImpl.class);

    public DemoServiceImpl() {
        ScheduledExecutorService scheduled = Executors.newSingleThreadScheduledExecutor();
        scheduled.scheduleWithFixedDelay(() -> {
            callbacks.forEach(callback -> {
                try {
                    boolean res = callback.echo("callback time: " + System.currentTimeMillis());
                    logger.info("send callback is succeed! return " + res);
                } catch (Exception e) {
                    logger.error("send callback is failed, cause by " + e.getMessage(), e);
                }
            });
        }, 5000, 5000, TimeUnit.MILLISECONDS);
    }

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

    @Override
    public void echoCallback(EchoCallback callback) {
        callbacks.add(callback);
    }


}
