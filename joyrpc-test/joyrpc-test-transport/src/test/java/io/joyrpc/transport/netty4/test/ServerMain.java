package io.joyrpc.transport.netty4.test;

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

import io.joyrpc.extension.URL;
import io.joyrpc.transport.DefaultEndpointFactory;
import io.joyrpc.transport.Server;
import io.joyrpc.transport.channel.ChannelHandlerChain;
import io.joyrpc.transport.netty4.mock.MockChannelHandler;
import io.joyrpc.transport.netty4.mock.MockCodec;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @date: 2019/1/28
 */
public class ServerMain {

    public static void main(String[] orgs) throws InterruptedException {
        URL url = URL.valueOf("mock://127.0.0.1:22000");
        Server server = new DefaultEndpointFactory().createServer(url);
        server.setCodec(new MockCodec());
        server.setChannelHandlerChain(
                new ChannelHandlerChain()
                        .addLast(new MockChannelHandler())
        );
        server.setBizThreadPool((ThreadPoolExecutor) Executors.newFixedThreadPool(100));
        server.open();

        synchronized (ServerMain.class) {
            while (true) {
                try {
                    ServerMain.class.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
