package io.joyrpc.example.boot;

/*-
 * #%L
 * joyrpc
 * %%
 * Copyright (C) 2019 joyrpc.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import io.joyrpc.GenericService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class BootGeneric {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "generic");
        ConfigurableApplicationContext run = SpringApplication.run(BootGeneric.class, args);
        GenericService consumer = run.getBean(GenericService.class);

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
        body.put("class", "io.joyrpc.example.service.vo.EchoData");
        //param
        param.put("header", header);
        param.put("body", body);

        Object res1 = consumer.$invoke("echoRequest", null, new Object[]{param});
        Object res2 = consumer.$invoke("echoRequestGeneric", null, new Object[]{param});

        System.out.println("generic test is end, res is " + res1 + ", " + res2);
        /*while (true) {
            try {
                System.out.println(consumer.$invoke("sayHello", null, new Object[]{"helloWold"}));
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
        }*/
    }
}

