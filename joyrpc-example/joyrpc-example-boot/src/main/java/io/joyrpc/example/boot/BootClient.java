package io.joyrpc.example.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource({"classpath:application-client.properties"})
public class BootClient {

    public static void main(String[] args) {
        SpringApplication.run(BootClient.class, args);
    }
}

