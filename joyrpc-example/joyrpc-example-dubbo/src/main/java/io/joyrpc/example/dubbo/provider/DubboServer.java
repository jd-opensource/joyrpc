package io.joyrpc.example.dubbo.provider;

import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.config.spring.context.annotation.EnableDubboConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@DubboComponentScan(basePackages="io.joyrpc.example.service.impl")
@EnableDubboConfig
public class DubboServer {

    private static final Logger logger = LoggerFactory.getLogger(DubboServer.class);


    public static void main(String[] args) throws InterruptedException {

        System.setProperty("spring.profiles.active", "server");
        SpringApplication.run(DubboServer.class, args);
        Thread.currentThread().join();
    }
}
