package io.joyrpc.example.boot;

import io.joyrpc.example.service.DemoService;
import io.joyrpc.exception.NoAliveProviderException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class BootClient {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "client");
        ConfigurableApplicationContext run = SpringApplication.run(BootClient.class, args);
        DemoService consumer = run.getBean(DemoService.class);
        while (true) {
            try {
                System.out.println(consumer.sayHello("helloWold"));
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                }
                if (e instanceof NoAliveProviderException) {
                    System.out.println(e.getMessage());
                } else {
                    e.printStackTrace();
                }
            }
        }
    }
}

