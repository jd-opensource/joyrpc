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

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.joyrpc.example.service.DemoServiceGrpc;
import io.joyrpc.example.service.HelloRequest;
import io.joyrpc.example.service.HelloResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

/**
 * 客户端
 */
@SpringBootApplication
public class BootGrpcServer {

    public static void main(String[] args) throws IOException, InterruptedException {
        ConfigurableApplicationContext run = SpringApplication.run(BootGrpcServer.class, args);
        final Server server = ServerBuilder.forPort(22000).addService(new DemoServiceImpl()).build().start();
        System.out.println("服务开始启动-------");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("------shutting down gRPC server since JVM is shutting down-------");
                server.shutdown();
                System.err.println("------server shut down------");
            }
        });
        Thread.currentThread().join();
    }

    /**
     * 服务实现
     */
    protected static class DemoServiceImpl extends DemoServiceGrpc.DemoServiceImplBase {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
            HelloResponse build = HelloResponse.newBuilder().setMessage(request.getName()).build();
            //onNext()方法向客户端返回结果
            responseObserver.onNext(build);
            //告诉客户端这次调用已经完成
            responseObserver.onCompleted();
        }
    }

}

