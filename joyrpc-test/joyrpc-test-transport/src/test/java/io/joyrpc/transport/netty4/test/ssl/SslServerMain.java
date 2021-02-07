package io.joyrpc.transport.netty4.test.ssl;

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
import io.joyrpc.transport.channel.ChannelChain;
import io.joyrpc.transport.netty4.mock.MockChannelHandler;
import io.joyrpc.transport.netty4.mock.MockCodec;
import io.joyrpc.transport.netty4.test.http.SimpleDeduction;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static io.joyrpc.constants.Constants.*;

/**
 * @date: 2019/8/13
 */
public class SslServerMain {

    public static void main(String[] orgs) throws InterruptedException {
        String pkPath = System.getProperty("user.dir") + "/laf-rpc-test/laf-rpc-transport-test/src/test/resources/ssl/serverStore.jks";
        URL url = URL.valueOf("mock://127.0.0.1:22000")
                .addIfAbsent(SSL_ENABLE.getName(), true)
                .addIfAbsent(SSL_PK_PATH.getName(), pkPath)
                .addIfAbsent(SSL_CA_PATH.getName(), pkPath)
                .addIfAbsent(SSL_CLIENT_AUTH.getName(), "REQUIRE")
                .addIfAbsent(SSL_PASSWORD.getName(), "nettyDemo");
        Server server = new DefaultEndpointFactory().createServer(url);
        server.setDeduction(new SimpleDeduction());
        server.setCodec(new MockCodec());
        server.setChannelHandlerChain(
                new ChannelChain()
                        .addLast(new MockChannelHandler())
        );
        server.setBizThreadPool((ThreadPoolExecutor) Executors.newFixedThreadPool(100));
        server.open();

        synchronized (SslServerMain.class) {
            while (true) {
                try {
                    SslServerMain.class.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
