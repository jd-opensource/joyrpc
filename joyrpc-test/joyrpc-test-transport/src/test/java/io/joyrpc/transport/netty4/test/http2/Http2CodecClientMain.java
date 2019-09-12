package io.joyrpc.transport.netty4.test.http2;

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
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.netty4.mock.MockResponseChannelHandler;
import io.joyrpc.transport.netty4.mock.MsgType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.joyrpc.transport.netty4.mock.MockMessage.createMockMessage;

/**
 * @date: 2019/4/10
 */
public class Http2CodecClientMain {
    private static final Logger logger = LoggerFactory.getLogger(Http2CodecClientMain.class);

    public static void main(String[] args) throws Exception {
        URL url = URL.valueOf("http2://127.0.0.1:22000");
        Client client = new DefaultEndpointFactory().createClient(url);
        client.setCodec(new MockHttp2Codec());
        ChannelHandlerChain channelHandlerChain =
                new ChannelHandlerChain()
                        .addLast(new MockCoverChannelHandler())
                        .addLast(new MockResponseChannelHandler());
        client.setChannelHandlerChain(channelHandlerChain);
        client.open();

        Message responseMsg = client.sync(createMockMessage("first sync mock", MsgType.BizReq), 500000);
        logger.info("receive sync response: " + responseMsg);

        client.oneway(createMockMessage("first oneway mock", MsgType.BizReq));
        client.oneway(createMockMessage("second oneway mock", MsgType.BizReq));

        CountDownLatch latch = new CountDownLatch(1);
        client.async(
                createMockMessage("first async mock", MsgType.BizReq),
                (msg, err) -> {
                    if (err != null) {
                        logger.error("get resp error:{}" + err.getMessage(), err);
                    } else {
                        logger.info("get resp" + msg);
                    }
                    latch.countDown();
                },
                1000);
        latch.await(1500, TimeUnit.MILLISECONDS);
        client.close();

    }
}
