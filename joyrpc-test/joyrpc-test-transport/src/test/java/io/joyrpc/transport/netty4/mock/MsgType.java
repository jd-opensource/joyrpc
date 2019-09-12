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

/**
 * @date: 2019/1/7
 */
public enum MsgType {
    // 业务请求消息
    BizReq((byte) 0),
    // 心跳请求消息
    HbReq((byte) 1),
    // 业务响应消息
    BizResp((byte) 2),
    // 业务响应消息
    HbResp((byte) 3),
    // 握手请求消息
    ShakeHandReq((byte) 4),
    // 握手响应消息
    ShakeHandResp((byte) 5),
    // 回调请求消息
    CallbackReq((byte) 6),
    // 回调响应消息
    CallbackResp((byte) 7);

    private byte type;

    MsgType(byte type) {
        this.type = type;
    }

    public static MsgType valueOf(byte b) {
        switch (b) {
            case (byte) 0:
                return BizReq;
            case (byte) 1:
                return HbReq;
            case (byte) 2:
                return BizResp;
            case (byte) 3:
                return HbResp;
            case (byte) 4:
                return ShakeHandReq;
            case (byte) 5:
                return ShakeHandResp;
            case (byte) 6:
                return CallbackReq;
            case (byte) 7:
                return CallbackResp;
            default:
                return BizReq;
        }
    }

    public byte getType() {
        return type;
    }

}
