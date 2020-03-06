package io.joyrpc.protocol;

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

import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.protocol.message.authentication.AuthenticationRequest;
import io.joyrpc.protocol.message.authentication.AuthenticationResponse;
import io.joyrpc.protocol.message.negotiation.NegotiationRequest;
import io.joyrpc.protocol.message.negotiation.NegotiationResponse;
import io.joyrpc.protocol.message.session.Sessionbeat;
import io.joyrpc.transport.session.DefaultSession;

/**
 * @date: 2019/1/7
 */
public enum MsgType {
    // 业务请求消息
    BizReq((byte) 1, true, Invocation.class),
    // 业务响应消息
    BizResp((byte) 2, false, ResponsePayload.class),
    // 回调请求消息
    CallbackReq((byte) 4, true, Invocation.class),
    // 回调响应消息
    CallbackResp((byte) 5, false, ResponsePayload.class),
    // 握手请求消息，保留和老版兼容
    @Deprecated
    ShakeHandReq((byte) 6, true),
    // 握手响应消息，保留和老版兼容
    @Deprecated
    ShakeHandResp((byte) 7, false),
    // 协商请求消息
    NegotiationReq((byte) 8, true, NegotiationRequest.class),
    // 协商响应消息
    NegotiationResp((byte) 9, false, NegotiationResponse.class),
    // 心跳请求消息
    HbReq((byte) 10, true),
    // 心跳响应消息
    HbResp((byte) 11, false),
    // 会话请求消息
    SessionReq((byte) 12, true, DefaultSession.class),
    // 会话响应消息
    SessionResp((byte) 13, false),
    // 会话续期请求消息
    SessionbeatReq((byte) 14, true, Sessionbeat.class),
    // Provider下线请求消息
    OfflineReq((byte) 15, true),
    // Provider下线响应消息
    OfflineResp((byte) 16, false),
    /**
     * 认证请求
     */
    AuthenticationReq((byte) 17, true, AuthenticationRequest.class),
    /**
     * 认证应答
     */
    AuthenticationResp((byte) 18, false, AuthenticationResponse.class);

    /**
     * 类型
     */
    private byte type;
    /**
     * 是否是请求
     */
    private boolean request;
    /**
     * 数据类
     */
    private Class payloadClz;

    MsgType(byte type, boolean request) {
        this.type = type;
        this.request = request;
    }

    MsgType(byte type, boolean request, Class clz) {
        this.type = type;
        this.request = request;
        this.payloadClz = clz;
    }

    /**
     * 获取消息类型
     *
     * @param b
     * @return
     */
    public static MsgType valueOf(final int b) {
        switch (b) {
            // 业务请求消息
            case 1:
                return BizReq;
            case 2:
                return BizResp;
            case 4:
                return CallbackReq;
            case 5:
                return CallbackResp;
            case 6:
                return ShakeHandReq;
            case 7:
                return ShakeHandResp;
            case 8:
                return NegotiationReq;
            case 9:
                return NegotiationResp;
            case 10:
                return HbReq;
            case 11:
                return HbResp;
            case 12:
                return SessionReq;
            case 13:
                return SessionResp;
            case 14:
                return SessionbeatReq;
            case 15:
                return OfflineReq;
            case 16:
                return OfflineResp;
            case 17:
                return AuthenticationReq;
            case 18:
                return AuthenticationResp;
            default:
                return null;
        }
    }

    public byte getType() {
        return type;
    }

    public Class getPayloadClz() {
        return payloadClz;
    }

    public boolean isRequest() {
        return request;
    }
}
