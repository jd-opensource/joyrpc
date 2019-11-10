package io.joyrpc.example.boot;

import io.joyrpc.example.service.DemoService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
public class BootClient {

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("spring.profiles.active", "client");
        ConfigurableApplicationContext run = SpringApplication.run(BootClient.class, args);
        DemoService consumer = run.getBean(DemoService.class);
        while (true) {
            System.out.println(consumer.sayHello("helloWold"));
            Thread.sleep(1000L);
        }
    }
}

