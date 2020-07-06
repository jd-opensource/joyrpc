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

import io.joyrpc.protocol.MsgType;
import io.joyrpc.transport.message.Message;

/**
 * 应答消息
 */
public class ResponseMessage<T> extends BaseMessage<T> implements Response {
    /**
     * 响应结果
     */
    protected T response;

    /**
     * 构造函数
     */
    public ResponseMessage() {
        this(new MessageHeader(MsgType.BizResp.getType()));
    }

    /**
     * 构造函数
     *
     * @param msgType
     */
    public ResponseMessage(byte msgType) {
        this(new MessageHeader(msgType));
    }

    /**
     * 构造函数
     *
     * @param msgType
     * @param msgId
     */
    public ResponseMessage(byte msgType, long msgId) {
        this(new MessageHeader(msgType, msgId));
    }

    /**
     * 构造函数
     *
     * @param header
     */
    public ResponseMessage(final MessageHeader header) {
        super(header);
    }

    /**
     * 构造函数
     *
     * @param header
     * @param msgType
     */
    public ResponseMessage(final MessageHeader header, final byte msgType) {
        super(header, msgType);
    }

    /**
     * 构造函数
     *
     * @param header
     * @param response
     */
    public ResponseMessage(MessageHeader header, T response) {
        super(header);
        this.response = response;
    }

    /**
     * 构造
     *
     * @param request
     * @param msgType
     * @param <T>
     * @return
     */
    public static <T> ResponseMessage<T> build(final Message<MessageHeader, T> request, byte msgType) {
        return build(request, msgType, null);
    }

    /**
     * 构造
     *
     * @param request
     * @param msgType
     * @param payload
     * @param <T>
     * @return
     */
    public static <T> ResponseMessage<T> build(final Message<MessageHeader, T> request, byte msgType, final T payload) {
        return new ResponseMessage<>(request.getHeader().response(msgType), payload);
    }

    @Override
    public T getPayLoad() {
        return response;
    }

    @Override
    public void setPayLoad(T payload) {
        this.response = payload;
    }

    @Override
    public boolean isRequest() {
        return false;
    }

    @Override
    public String toString() {
        return "ResponseMessage{" +
                "header=" + getHeader() +
                "response=" + response +
                '}';
    }
}
