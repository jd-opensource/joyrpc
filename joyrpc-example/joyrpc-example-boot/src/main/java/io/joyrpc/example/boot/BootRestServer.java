package io.joyrpc.example.boot;

import io.joyrpc.config.Warmup;
import io.joyrpc.example.service.DemoService;
import io.joyrpc.example.service.impl.RestDemoServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;


public class BootRestServer {

    private static final Logger logger = LoggerFactory.getLogger(BootRestServer.class);

    @Bean("restDemoServiceImpl")
    public DemoService restDemoServiceImpl() {
        return new RestDemoServiceImpl();
    }

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("spring.profiles.active", "rest");
        SpringApplication.run(BootRestServer.class, args);
        Thread.currentThread().join();
    }
}
