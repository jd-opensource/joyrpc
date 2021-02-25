package io.joyrpc.example.dubbo.generic;

import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.apache.dubbo.config.spring.context.annotation.EnableDubboConfig;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableDubboConfig
@DubboComponentScan(basePackages = "io.joyrpc.example.dubbo.generic")
public class DubboGeneric {

    @Reference(
            version = "0.0.0",
            group = "2.0-Boot",
            url = "10.0.16.232:22000",
            interfaceName = "io.joyrpc.example.service.DemoService",
            generic = true)
    private GenericService genericService;

    public GenericService getGenericService() {
        return genericService;
    }

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "generic");

        ConfigurableApplicationContext run = SpringApplication.run(DubboGeneric.class, args);
        DubboGeneric application = run.getBean(DubboGeneric.class);
        GenericService consumer = application.getGenericService();

        Map<String, Object> param = new HashMap<>();
        //header
        Map<String, Object> header = new HashMap<>();
        Map<String, Object> headerAttrs = new HashMap<>();
        headerAttrs.put("type", "generic");
        headerAttrs.put("bodyType", "EchoData");
        header.put("attrs", headerAttrs);
        //body
        Map<String, Object> body = new HashMap<>();
        body.put("code", 1);
        body.put("message", "this is body");
        //param
        param.put("header", header);
        param.put("body", body);


        Object res1 = consumer.$invoke("echoRequest", null, new Object[]{param});

        System.out.println("res:"+res1);

        /*AtomicLong counter = new AtomicLong(0);
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
        }*/
    }

}
