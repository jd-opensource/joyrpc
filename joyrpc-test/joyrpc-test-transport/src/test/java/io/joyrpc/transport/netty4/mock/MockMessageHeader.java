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


import io.joyrpc.transport.message.Header;
import io.joyrpc.transport.session.Session;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @date: 2019/1/28
 */
public class MockMessageHeader implements Header {

    private static AtomicInteger atomicInteger = new AtomicInteger();

    private int msgType;

    private long msgId;

    protected Session session;

    public MockMessageHeader(int msgType) {
        this.msgType = msgType;
    }

    public MockMessageHeader(long msgId, int msgType) {
        this.msgId = msgId;
        this.msgType = msgType;
    }

    @Override
    public long getMsgId() {
        return msgId;
    }

    @Override
    public void setMsgId(long id) {
        this.msgId = id;
    }

    @Override
    public int getMsgType() {
        return msgType;
    }

    @Override
    public Header clone() {
        return new MockMessageHeader(msgId, msgType);
    }

    @Override
    public Short getHeaderLength() {
        return null;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public void setSession(Session session) {
        this.session = session;
    }

    @Override
    public String toString() {
        return "MockMessageHeader{" +
                "msgType=" + msgType +
                ", id=" + msgId +
                '}';
    }
}
