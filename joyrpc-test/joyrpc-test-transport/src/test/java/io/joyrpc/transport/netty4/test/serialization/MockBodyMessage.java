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

import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.netty4.mock.MockMessageHeader;
import io.joyrpc.transport.netty4.mock.MsgType;

/**
 * @date: 2019/3/22
 */
public class MockBodyMessage implements Message<MockMessageHeader, MessageBody> {

    private MockMessageHeader header;

    private MessageBody body;

    @Override
    public MockMessageHeader getHeader() {
        return header;
    }

    @Override
    public void setHeader(MockMessageHeader header) {
        this.header = header;
    }

    @Override
    public MessageBody getPayLoad() {
        return body;
    }

    @Override
    public void setPayLoad(MessageBody data) {
        this.body = data;
    }

    @Override
    public boolean isRequest() {
        return getMsgType() == MsgType.BizReq.getType()
                || getMsgType() == MsgType.HbReq.getType();
    }

    public static MockBodyMessage createMockMessage(String msg, MsgType msgType) {
        MockMessageHeader header = new MockMessageHeader(msgType.getType());
        MockBodyMessage mockMessage = new MockBodyMessage();
        mockMessage.setHeader(header);
        mockMessage.setPayLoad(new MessageBody("req-" + header.getMsgId(), "request"));
        return mockMessage;
    }

    @Override
    public String toString() {
        return "MockBodyMessage{" +
                "header=" + header +
                ", body=" + body +
                '}';
    }
}
