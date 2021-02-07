package io.joyrpc.transport.netty4.test.bizthreadpool;

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
import io.joyrpc.transport.channel.ChannelChain;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.netty4.mock.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @date: 2019/1/28
 */
public class BizClientMain {

    private static final Logger logger = LoggerFactory.getLogger(BizClientMain.class);

    public static void main(String[] orgs) throws Exception {
        URL url = URL.valueOf("mock://127.0.0.1:22000");
        Client client = new DefaultEndpointFactory().createClient(url);
        client.setCodec(new MockCodec());
        ChannelChain channelHandlerChain =
                new ChannelChain()
                        .addLast(new MockResponseChannelHandler())
                        .addLast(new MockChannelHandler());
        client.setChannelHandlerChain(channelHandlerChain);
        client.setBizThreadPool((ThreadPoolExecutor) Executors.newFixedThreadPool(50));
        client.open();

        Message responseMsg = client.sync(MockMessage.createMockMessage("first sync mock", MsgType.BizReq), 5000);
        logger.info("receive sync response: " + responseMsg);

        client.oneway(MockMessage.createMockMessage("first oneway mock", MsgType.BizReq));
        client.oneway(MockMessage.createMockMessage("second oneway mock", MsgType.BizReq));

        client.async(
                MockMessage.createMockMessage("first async mock", MsgType.BizReq),
                (msg, err) -> {
                    if (err != null) {
                        logger.error("get resp error:{}" + err.getMessage(), err);
                    } else {
                        logger.info("get resp" + msg);
                    }
                },
                1000);

        client.close();
    }

}
