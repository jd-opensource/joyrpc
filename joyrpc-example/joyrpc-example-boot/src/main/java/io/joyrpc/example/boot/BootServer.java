package io.joyrpc.example.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BootServer {

    public static void main(String[] args) {
        SpringApplication.run(BootServer.class, args);

//        //hold住本地服务
//        synchronized (ServerMainBoot.class) {
//            while (true) {
//                try {
//                    ServerMainBoot.class.wait();
//                } catch (InterruptedException e) {
//                }
//            }
//        }
    }

}

