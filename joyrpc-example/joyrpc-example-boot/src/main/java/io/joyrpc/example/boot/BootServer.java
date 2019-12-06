package io.joyrpc.example.boot;

import io.joyrpc.config.Warmup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.CompletableFuture;

@SpringBootApplication
public class BootServer {

    private static final Logger logger = LoggerFactory.getLogger(BootServer.class);

    @Bean("warmup")
    public Warmup warmup() {
        return config -> {
            logger.info("load warmup data........");
            return CompletableFuture.completedFuture(null);
        };
    }

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("spring.profiles.active", "server");
        SpringApplication.run(BootServer.class, args);
        Thread.currentThread().join();
    }
}

