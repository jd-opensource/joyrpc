package io.joyrpc.example.boot;

import io.joyrpc.GenericService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class BootGeneric {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "generic");
        ConfigurableApplicationContext run = SpringApplication.run(BootGeneric.class, args);
        GenericService consumer = run.getBean(GenericService.class);
        while (true) {
            try {
                System.out.println(consumer.$invoke("sayHello", null, new Object[]{"helloWold"}).get());
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                }
                e.printStackTrace();
            }
        }
    }
}

