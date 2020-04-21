package io.joyrpc.example;

import io.joyrpc.config.ConsumerConfig;
import io.joyrpc.config.RegistryConfig;
import io.joyrpc.example.service.DemoService;
import org.junit.Test;

public class ConsulConsumerStartTest {

    @Test
    public void testConsume() throws Exception {
        ConsumerConfig<DemoService> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setRegistry(new RegistryConfig("consul", "127.0.0.1:8500"));
        consumerConfig.setInterfaceClazz(DemoService.class.getName());
        consumerConfig.setAlias("test");
        consumerConfig.setTimeout(500000);
        DemoService demoService = consumerConfig.refer().get();
        System.out.println(demoService);
        while (true) {
            try {
                String res = demoService.sayHello("consul");
                System.out.println("res==" + res);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Thread.sleep(2000);
            }
        }

    }
}
