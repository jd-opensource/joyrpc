package io.joyrpc.example.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BootServer {

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("spring.profiles.active", "server");
        SpringApplication.run(BootServer.class, args);
        Thread.currentThread().join();
    }
}

