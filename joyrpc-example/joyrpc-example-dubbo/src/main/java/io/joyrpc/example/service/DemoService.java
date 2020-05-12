package io.joyrpc.example.service;

public interface DemoService {

    String sayHello(String str) throws Throwable;

    int test(int count);

    <T> T generic(T value);

    default String echo(String str) throws Throwable {
        return sayHello(str);
    }

    static String hello(String v) {
        return v;
    }
}
