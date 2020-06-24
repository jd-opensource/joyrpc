package io.joyrpc.example.service.impl;

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

import io.joyrpc.annotation.Provider;
import io.joyrpc.example.service.DemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 服务同时支持JSF&Restful调用
 */
@Resource
@Path("rest")
@Consumes
@Provider(name = "provider-demoService", alias = "2.0-Boot")
public class DemoServiceImpl implements DemoService {

    private Set<EchoCallback> callbacks = new HashSet<>();

    private static final Logger logger = LoggerFactory.getLogger(DemoServiceImpl.class);

    public DemoServiceImpl() {
        ScheduledExecutorService scheduled = Executors.newSingleThreadScheduledExecutor();
        scheduled.scheduleWithFixedDelay(() -> {
            callbacks.forEach(callback -> {
                try {
                    boolean res = callback.echo("callback time: " + System.currentTimeMillis());
                    logger.info("send callback is succeed! return " + res);
                } catch (Exception e) {
                    logger.error("send callback is failed, cause by " + e.getMessage(), e);
                }
            });
        }, 5000, 5000, TimeUnit.MILLISECONDS);
    }


    @Override
    public int test(int count) {
        return count;
    }

    @Override
    public <T> T generic(T value) {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }
        return value;
    }

    @GET
    @Path(value = "/hello/{name}")
    @Override
    public String sayHello(@PathParam("name") String str) {
        System.out.println("Hi " + str + ", request from consumer.");
        return "Hi " + str + ", response from provider. ";
    }

    @Override
    public void echoCallback(EchoCallback callback) {
        callbacks.add(callback);
    }
}
