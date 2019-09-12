package io.joyrpc.transport.netty4.test.heartbeat;

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
import io.joyrpc.transport.Client;
import io.joyrpc.transport.DefaultEndpointFactory;
import io.joyrpc.transport.channel.ChannelHandlerChain;
import io.joyrpc.transport.heartbeat.HeartbeatStrategy;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.netty4.mock.MockCodec;
import io.joyrpc.transport.netty4.mock.MockMessage;
import io.joyrpc.transport.netty4.mock.MockResponseChannelHandler;
import io.joyrpc.transport.netty4.mock.MsgType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * @date: 2019/2/14
 */
public class IdleHeartbeatClientMain {

    private static final Logger logger = LoggerFactory.getLogger(IdleHeartbeatClientMain.class);

    public static void main(String[] orgs) throws Exception {
        URL url = URL.valueOf("mock://127.0.0.1:22000");
        Client client = new DefaultEndpointFactory().createClient(url);
        client.setCodec(new MockCodec());
        ChannelHandlerChain channelHandlerChain =
                new ChannelHandlerChain()
                        .addLast(new MockResponseChannelHandler());
        client.setChannelHandlerChain(channelHandlerChain);
        client.setHeartbeatStrategy(new HeartbeatStrategy() {

            @Override
            public Supplier<Message> getHeartbeat() {
                return () -> MockMessage.createMockMessage("heartbeat req", MsgType.HbReq);
            }

            @Override
            public HeartbeatMode getHeartbeatMode() {
                return HeartbeatMode.IDLE;
            }

        });
        client.open();

        synchronized (IdleHeartbeatClientMain.class) {
            while (true) {
                try {
                    IdleHeartbeatClientMain.class.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

}
