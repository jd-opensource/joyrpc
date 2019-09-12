package io.joyrpc.transport.netty4.test.serialization;

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
import io.joyrpc.transport.netty4.mock.MockMessageHeader;
import io.joyrpc.transport.netty4.mock.MsgType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @date: 2019/1/28
 */
public class MockMsgBodyChannelHandler implements ChannelHandler {

    private static final Logger logger = LoggerFactory.getLogger(MockMsgBodyChannelHandler.class);


    @Override
    public Object received(ChannelContext context, Object msgObj) {
        logger.info("reveive message: " + msgObj.toString());
        if (msgObj instanceof MockBodyMessage) {
            MockBodyMessage msg = (MockBodyMessage) msgObj;
            if (msg.getMsgType() == MsgType.BizReq.getType()) {
                MockBodyMessage respMsg = new MockBodyMessage();
                respMsg.setHeader(new MockMessageHeader(msg.getMsgId(), MsgType.BizResp.getType()));
                respMsg.setPayLoad(new MessageBody("reps-" + msg.getMsgId(), "this is reps"));
                context.getChannel().send(respMsg, null);
            } else if (msg.getMsgType() == MsgType.HbReq.getType()) {
                MockBodyMessage respMsg = new MockBodyMessage();
                respMsg.setHeader(new MockMessageHeader(msg.getMsgId(), MsgType.HbResp.getType()));
                respMsg.setPayLoad(new MessageBody("reps-hb-" + msg.getMsgId(), "this is hb reps"));
                context.getChannel().send(respMsg, null);
            }
        }
        return msgObj;
    }

    @Override
    public Object wrote(ChannelContext context, Object message) {
        return message;
    }

    @Override
    public void caught(ChannelContext context, Throwable throwable) {
        logger.error(throwable.getMessage(), throwable);
    }
}
