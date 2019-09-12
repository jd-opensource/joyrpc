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

import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.channel.ChannelHandler;
import io.joyrpc.transport.http2.DefaultHttp2RequestMessage;
import io.joyrpc.transport.http2.Http2RequestMessage;
import io.joyrpc.transport.http2.Http2ResponseMessage;
import io.joyrpc.transport.netty4.mock.MockMessage;
import io.joyrpc.transport.netty4.mock.MockMessageHeader;
import io.joyrpc.transport.netty4.mock.MsgType;
import io.netty.util.AsciiString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * @date: 2019/4/10
 */
public class MockCoverChannelHandler implements ChannelHandler {

    private static final Logger logger = LoggerFactory.getLogger(MockCoverChannelHandler.class);

    @Override
    public Object received(ChannelContext context, Object message) {
        if (message instanceof Http2ResponseMessage) {
            int bizId = ((Http2ResponseMessage) message).getBizMsgId();
            MockMessage respMsg = new MockMessage();
            respMsg.setHeader(new MockMessageHeader(bizId, MsgType.BizResp.getType()));
            respMsg.setPayLoad(("response message, id: " + respMsg.getMsgId()).getBytes());
            return respMsg;
        }
        return message;
    }

    @Override
    public Object wrote(ChannelContext context, Object message) {
        if (message instanceof MockMessage && ((MockMessage) message).isRequest()) {
            MockMessage mockMsg = (MockMessage) message;
            Http2RequestMessage http2ReqMsg = new DefaultHttp2RequestMessage(0, mockMsg.getMsgId(), mockMsg.getPayLoad());
            InetSocketAddress remoteAddress = context.getChannel().getRemoteAddress();
            http2ReqMsg.headers().authority(new AsciiString(remoteAddress.getHostName() + ":" + remoteAddress.getPort()));
            http2ReqMsg.headers().scheme(AsciiString.of("http"));
            http2ReqMsg.headers().add("user-agent", new AsciiString("joyrpc/2.0"));
            return http2ReqMsg;
        }
        return message;

    }

    @Override
    public void caught(ChannelContext context, Throwable throwable) {
        logger.error(throwable.getMessage(), throwable);
    }
}
