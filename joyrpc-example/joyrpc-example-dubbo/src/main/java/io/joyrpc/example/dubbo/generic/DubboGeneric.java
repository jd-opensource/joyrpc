package io.joyrpc.example.dubbo.generic;

import io.joyrpc.example.dubbo.consumer.DubboClient;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.apache.dubbo.config.spring.context.annotation.EnableDubboConfig;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
@EnableDubboConfig
@DubboComponentScan(basePackages = "io.joyrpc.example.dubbo.generic")
public class DubboGeneric {

    @Reference(
            version = "0.0.0",
            group = "2.0-Boot",
            url = "127.0.0.1:22000",
            interfaceName = "io.joyrpc.example.service.DemoService",
            generic = true)
    private GenericService genericService;

    public GenericService getGenericService() {
        return genericService;
    }

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "generic");

        ConfigurableApplicationContext run = SpringApplication.run(DubboClient.class, args);
        GenericService consumer = run.getBean(DubboGeneric.class).getGenericService();
        AtomicLong counter = new AtomicLong(0);
        while (true) {
            try {
                Object res = consumer.$invoke(
                        "sayHello",
                        new String[]{String.class.getName()},
                        new Object[]{"dubbo-" + counter.incrementAndGet()});
                System.out.println(res);
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
