package io.joyrpc.transport.netty4.mock;

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

/**
 * @date: 2019/1/28
 */
public class MockMessage implements Message<MockMessageHeader, byte[]> {

    private byte[] data;

    private MockMessageHeader header;

    @Override
    public MockMessageHeader getHeader() {
        return header;
    }

    @Override
    public void setHeader(MockMessageHeader header) {
        this.header = header;
    }

    @Override
    public byte[] getPayLoad() {
        return data;
    }

    @Override
    public void setPayLoad(byte[] data) {
        this.data = data;
    }

    @Override
    public int getMsgId() {
        return getHeader().getMsgId();
    }

    @Override
    public int getMsgType() {
        return getHeader().getMsgType();
    }

    @Override
    public boolean isRequest() {
        return getMsgType() == MsgType.BizReq.getType()
                || getMsgType() == MsgType.HbReq.getType();
    }

    @Override
    public String toString() {
        return "MockMessage{" +
                "data=" + new String(data) +
                ", header=" + header +
                '}';
    }

    public static MockMessage createMockMessage(String msg, MsgType msgType) {
        MockMessageHeader header = new MockMessageHeader(msgType.getType());
        MockMessage mockMessage = new MockMessage();
        mockMessage.setHeader(header);
        mockMessage.setPayLoad(msg.getBytes());
        return mockMessage;
    }
}
