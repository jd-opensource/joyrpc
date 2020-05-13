package io.joyrpc.example.dubbo.consumer;

import io.joyrpc.example.service.DemoService;
import io.joyrpc.example.service.vo.Java8TimeObj;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.apache.dubbo.config.spring.context.annotation.EnableDubboConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.junit.Assert;

import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
@EnableDubboConfig
@DubboComponentScan(basePackages = "io.joyrpc.example.dubbo.consumer")
public class DubboClient {

    @Reference(version = "0.0.0", group = "2.0-Boot", url = "127.0.0.1:22000", timeout = 50000)
    private DemoService demoService;

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "client");

        ConfigurableApplicationContext run = SpringApplication.run(DubboClient.class, args);
        DubboClient application = run.getBean(DubboClient.class);
        DemoService consumer = application.getDemoService();
        AtomicLong counter = new AtomicLong(0);
        while (true) {
            try {
                //调用
                /*String hello = consumer.sayHello("dubbo-" + String.valueOf(counter.incrementAndGet()));
                System.out.println(hello);*/
                // 验证java8 时间对象
                Java8TimeObj java8TimeObj = Java8TimeObj.newJava8TimeObj();
                Java8TimeObj java8TimeObjRes = consumer.echoJava8TimeObj(java8TimeObj);
                Assert.assertEquals(java8TimeObj, java8TimeObjRes);
                Thread.sleep(100000L);
            } catch (InterruptedException e) {
                break;
            } catch (Throwable e) {
                e.printStackTrace();
                try {
                    Thread.sleep(100000L);
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    public DemoService getDemoService() {
        return demoService;
    }

    public void setDemoService(DemoService demoService) {
        this.demoService = demoService;
    }
}
