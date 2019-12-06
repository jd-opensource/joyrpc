package io.joyrpc.example.boot;

import io.joyrpc.example.service.DemoService;
import io.joyrpc.example.service.impl.RestDemoServiceImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BootRestServer {

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
