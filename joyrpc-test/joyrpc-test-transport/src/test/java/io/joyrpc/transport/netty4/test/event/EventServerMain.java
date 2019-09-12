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

import io.joyrpc.event.EventHandler;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.DefaultEndpointFactory;
import io.joyrpc.transport.Server;
import io.joyrpc.transport.channel.ChannelHandlerChain;
import io.joyrpc.transport.event.ActiveEvent;
import io.joyrpc.transport.event.InactiveEvent;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.transport.netty4.mock.MockChannelHandler;
import io.joyrpc.transport.netty4.mock.MockCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @date: 2019/1/28
 */
public class EventServerMain {

    private static final Logger logger = LoggerFactory.getLogger(EventServerMain.class);

    public static void main(String[] orgs) throws InterruptedException {
        URL url = URL.valueOf("mock://127.0.0.1:22000");
        Server server = new DefaultEndpointFactory().createServer(url);
        server.setCodec(new MockCodec());
        server.setChannelHandlerChain(
                new ChannelHandlerChain()
                        .addLast(new MockChannelHandler())
        );
        server.addEventHandler(
                new EventHandler<TransportEvent>() {
                    @Override
                    public void handle(TransportEvent event) {
                        if (event instanceof InactiveEvent) {
                            logger.info("inactive: {}", ((InactiveEvent) event).getChannel().getRemoteAddress());
                        } else if (event instanceof ActiveEvent) {
                            logger.info("active: {}", ((ActiveEvent) event).getChannel().getRemoteAddress());
                        }
                    }
                });
        server.open();

        synchronized (EventServerMain.class) {
            while (true) {
                try {
                    EventServerMain.class.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
