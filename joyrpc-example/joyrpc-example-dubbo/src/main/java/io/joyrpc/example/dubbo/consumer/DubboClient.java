package io.joyrpc.example.dubbo.consumer;

import io.joyrpc.example.service.DemoService;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.spring.context.annotation.EnableDubboConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
@EnableDubboConfig
public class DubboClient {

    @Reference(version = "0.0.0", group = "2.0-Boot", url = "127.0.0.1:22000")
    private DemoService demoService;

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "client");

        ConfigurableApplicationContext run = SpringApplication.run(DubboClient.class, args);
        DemoService consumer = run.getBean(DemoService.class);
        AtomicLong counter = new AtomicLong(0);
        while (true) {
            try {
                String hello = consumer.sayHello("dubbo-" + String.valueOf(counter.incrementAndGet()));
                System.out.println(hello);
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                break;
            } catch (Throwable e) {
                e.printStackTrace();
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                }
            }
        }
    }
}
