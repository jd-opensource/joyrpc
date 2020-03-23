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

import io.joyrpc.config.Warmup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.CompletableFuture;

@SpringBootApplication
public class BootServer {

    private static final Logger logger = LoggerFactory.getLogger(BootServer.class);

    @Bean("warmup")
    public Warmup warmup() {
        return config -> {
            logger.info("load warmup data........");
            return CompletableFuture.completedFuture(null);
        };
    }

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("spring.profiles.active", "server");
        //Map<String, Lan> blacks = new HashMap<>();
        //blacks.put("*", new Lan("127.0.0.1"));
        //IPPermissionConfiguration.IP_PERMISSION.update(DemoService.class.getName(), new IPPermission(true, null, blacks));
        ConfigurableApplicationContext ctx = SpringApplication.run(BootServer.class, args);
        Thread.currentThread().join();
    }
}

