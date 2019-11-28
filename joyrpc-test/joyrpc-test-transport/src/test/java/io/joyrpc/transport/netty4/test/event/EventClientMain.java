package io.joyrpc.transport.netty4.test.event;

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
import io.joyrpc.transport.event.ActiveEvent;
import io.joyrpc.transport.event.InactiveEvent;
import io.joyrpc.transport.event.ReconnectedEvent;
import io.joyrpc.transport.netty4.mock.MockChannelHandler;
import io.joyrpc.transport.netty4.mock.MockCodec;
import io.joyrpc.transport.netty4.mock.MockResponseChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @date: 2019/1/28
 */
public class EventClientMain {

    private static final Logger logger = LoggerFactory.getLogger(EventClientMain.class);

    public static void main(String[] orgs) throws Exception {
        URL url = URL.valueOf("mock://127.0.0.1:22000?channelManagerFactory=shared");
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                Client client = new DefaultEndpointFactory().createClient(url);
                client.setCodec(new MockCodec());
                ChannelHandlerChain channelHandlerChain =
                        new ChannelHandlerChain()
                                .addLast(new MockResponseChannelHandler())
                                .addLast(new MockChannelHandler());
                client.setChannelHandlerChain(channelHandlerChain);
                client.addEventHandler(
                        event -> {
                            if (event instanceof InactiveEvent) {
                                logger.info("inactive: {}", ((InactiveEvent) event).getChannel().getRemoteAddress());
                                new Thread(() -> {
                                    while (true) {
                                        try {
                                            client.open();
                                            break;
                                        } catch (Exception e) {
                                            logger.info("error: {}", e.getMessage(), e);
                                            try {
                                                Thread.sleep(1000);
                                            } catch (InterruptedException el) {
                                            }
                                        }
                                    }
                                }).start();
                            } else if (event instanceof ActiveEvent) {
                                logger.info("active: {}", ((ActiveEvent) event).getChannel().getRemoteAddress());
                            } else if (event instanceof ReconnectedEvent) {
                                logger.info("reconnect: {}", event);
                            }
                        }
                );
                try {
                    client.open();
                } catch (Exception e) {
                    logger.info("error: {}", e.getMessage(), e);
                }
            }).start();
        }


        while (true) {
            //Message responseMsg = client.sync(createMockMessage("sync mock", MsgType.BizReq), 5000);
            //logger.info("receive sync response: " + responseMsg);
            Thread.sleep(2000);
        }

    }

}
