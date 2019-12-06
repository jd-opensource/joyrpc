package io.joyrpc.example.rest.service;

import io.joyrpc.example.service.DemoService;

import javax.annotation.Resource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Resource
@Path("rest")
@Consumes
public class RestDemoServiceImpl implements DemoService {

    @GET
    @Path(value = "/hello/{name}")
    public String sayHello(@PathParam("name") String name) {
        return "Rest service say: Hello~ " + name;
    }
}
