package io.joyrpc.example.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BootRestServer {

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("spring.profiles.active", "rest");
        SpringApplication.run(BootRestServer.class, args);
        Thread.currentThread().join();
    }
}
