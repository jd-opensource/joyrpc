package io.joyrpc.example.service;

import io.joyrpc.example.service.vo.Java8TimeObj;

public interface DemoService {

    String sayHello(String str) throws Throwable;

    int test(int count);

    <T> T generic(T value);

    default String echo(String str) throws Throwable {
        return sayHello(str);
    }

    default Java8TimeObj echoJava8TimeObj(Java8TimeObj timeObj) {
        return timeObj;
    }

    static String hello(String v) {
        return v;
    }
}
