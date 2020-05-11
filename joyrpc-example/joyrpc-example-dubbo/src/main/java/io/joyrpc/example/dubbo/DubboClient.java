package io.joyrpc.example.dubbo;

import io.joyrpc.example.dubbo.service.DubboDemoService;
import io.joyrpc.exception.NoAliveProviderException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
public class DubboClient {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "client");
        ConfigurableApplicationContext run = SpringApplication.run(DubboClient.class, args);
        DubboDemoService consumer = run.getBean(DubboDemoService.class);
        AtomicLong counter = new AtomicLong(0);
        while (true) {
            try {
                String hello = consumer.sayHello("dubbo-" + String.valueOf(counter.incrementAndGet()));
                System.out.println(hello);
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                break;
            } catch (Throwable e) {
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
