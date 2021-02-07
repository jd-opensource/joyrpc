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
import io.joyrpc.transport.Client;
import io.joyrpc.transport.DefaultEndpointFactory;
import io.joyrpc.transport.channel.ChannelChain;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.netty4.mock.ClientMockCodec;
import io.joyrpc.transport.netty4.mock.MockMessage;
import io.joyrpc.transport.netty4.mock.MockResponseChannelHandler;
import io.joyrpc.transport.netty4.mock.MsgType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.joyrpc.constants.Constants.*;

/**
 * @date: 2019/8/13
 */
public class SslClientMain {

    private static final Logger logger = LoggerFactory.getLogger(SslClientMain.class);

    public static void main(String[] orgs) throws Exception {
        String pkPath = System.getProperty("user.dir") + "/laf-rpc-test/laf-rpc-transport-test/src/test/resources/ssl/clientStore.jks";
        URL url = URL.valueOf("mock://127.0.0.1:22000")
                .addIfAbsent(SSL_ENABLE.getName(), true)
                .addIfAbsent(SSL_PK_PATH.getName(), pkPath)
                .addIfAbsent(SSL_CA_PATH.getName(), pkPath)
                .addIfAbsent(SSL_PASSWORD.getName(), "nettyDemo");
        ;
        Client client = new DefaultEndpointFactory().createClient(url);
        client.setCodec(new ClientMockCodec());
        ChannelChain channelHandlerChain =
                new ChannelChain()
                        .addLast(new MockResponseChannelHandler());
        client.setChannelHandlerChain(channelHandlerChain);
        client.open();

        //while (true){
        try {
            Message responseMsg = client.sync(MockMessage.createMockMessage("first sync mock", MsgType.BizReq), 5000);
            logger.info("receive sync response: " + responseMsg);
            Thread.sleep(5000);
        } catch (Exception e) {
            Thread.sleep(1000);
        } finally {
            client.close();
        }
        //}


        /*client.oneway(createMockMessage("first oneway mock", MsgType.BizReq));
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
        latch.await(1500, TimeUnit.MILLISECONDS);*/
        //client.close();
    }

}
