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

import io.joyrpc.context.RequestContext;
import io.joyrpc.annotation.Provider;
import io.joyrpc.example.service.EchoService;

import javax.annotation.Resource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * 服务同时支持JSF&Restful调用
 */
@Resource
@Path("rest")
@Consumes
@Provider(name = "provider-echoService", alias = "2.0-Boot")
public class EchoServiceImpl implements EchoService {

    @GET
    @Path(value = "/echo/{name}")
    @Override
    public String sayHello(@PathParam("name") String str) {
        RequestContext context = RequestContext.getContext();
        System.out.println("Echo " + str + ", request from consumer.");
        return String.format("Echo %s, response from provider. EchoService(counter=%d, tracer=%s)", str, context.getAttachment("counter"), context.getAttachment("tracer"));
    }
}
