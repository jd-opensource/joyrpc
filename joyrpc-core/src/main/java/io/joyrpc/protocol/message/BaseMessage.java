package io.joyrpc.protocol.message;

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

import java.io.Serializable;

/**
 * @date: 8/1/2019
 */
public abstract class BaseMessage<T> implements Message<T>, Serializable {
    /**
     * 消息头
     */
    protected transient MessageHeader header;

    /**
     * 构造函数
     *
     * @param header
     */
    public BaseMessage(final MessageHeader header) {
        this.header = header;
    }

    /**
     * 构造函数
     *
     * @param header
     * @param msgType
     */
    public BaseMessage(final MessageHeader header, final byte msgType) {
        this.header = header;
        if (header != null) {
            header.setMsgType(msgType);
        }
    }

    @Override
    public MessageHeader getHeader() {
        return header;
    }

    @Override
    public void setHeader(MessageHeader header) {
        this.header = header;
    }

    @Override
    public int getMsgId() {
        return header == null ? -1 : header.msgId;
    }

    @Override
    public void setMsgId(final int id) {
        header.msgId = id;
    }

    @Override
    public int getMsgType() {
        return header == null ? -1 : header.msgType;
    }

    @Override
    public int getSessionId() {
        return header == null ? -1 : header.sessionId;
    }

    @Override
    public void setSessionId(final int sessionId) {
        header.sessionId = sessionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseMessage)) {
            return false;
        }

        BaseMessage that = (BaseMessage) o;

        if (!header.equals(that.header)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return header.hashCode();
    }

    @Override
    public String toString() {
        return "BaseMessage{" +
                "header=" + header +
                '}';
    }

}
