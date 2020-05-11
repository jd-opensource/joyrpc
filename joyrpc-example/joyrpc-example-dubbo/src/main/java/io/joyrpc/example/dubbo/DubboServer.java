package io.joyrpc.example.dubbo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class DubboServer {

    private static final Logger logger = LoggerFactory.getLogger(DubboServer.class);


    public static void main(String[] args) throws InterruptedException {
        System.setProperty("spring.profiles.active", "server");
        ConfigurableApplicationContext ctx = SpringApplication.run(DubboServer.class, args);
        Thread.currentThread().join();
    }
}
