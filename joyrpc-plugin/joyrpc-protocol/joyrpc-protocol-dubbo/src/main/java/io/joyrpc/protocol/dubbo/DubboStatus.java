package io.joyrpc.protocol.dubbo;

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

import io.joyrpc.exception.*;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.message.Header;

import java.util.concurrent.TimeoutException;

/**
 * 状态
 */
public class DubboStatus {

    public static final byte OK = 20;
    public static final byte CLIENT_TIMEOUT = 30;
    public static final byte SERVER_TIMEOUT = 31;
    public static final byte CHANNEL_INACTIVE = 35;
    public static final byte BAD_REQUEST = 40;
    public static final byte BAD_RESPONSE = 50;
    public static final byte SERVICE_NOT_FOUND = 60;
    public static final byte SERVICE_ERROR = 70;
    public static final byte SERVER_ERROR = 80;
    public static final byte CLIENT_ERROR = 90;
    public static final byte SERVER_THREADPOOL_EXHAUSTED_ERROR = 100;

    /**
     * 获取状态
     *
     * @param err 异常
     * @return 状态
     */
    public static byte getStatus(final Throwable err) {
        if (err == null) {
            return OK;
        } else if (err instanceof ProtocolException) {
            Header header = ((ProtocolException) err).getHeader();
            MsgType msgType = header == null ? null : MsgType.valueOf(header.getMsgType());
            return msgType == null || !msgType.isRequest() ? BAD_RESPONSE : BAD_REQUEST;
        } else if (err instanceof TransportException) {
            return CHANNEL_INACTIVE;
        } else if (err instanceof OverloadException) {
            return SERVER_THREADPOOL_EXHAUSTED_ERROR;
        }
        return SERVICE_ERROR;
    }

    public static Throwable getThrowable(byte status, String errMsg) {
        switch (status) {
            case OK:
                return null;
            case SERVER_TIMEOUT:
            case CLIENT_TIMEOUT:
                return new TimeoutException(errMsg);
            case BAD_REQUEST:
            case BAD_RESPONSE:
                return new CodecException(errMsg);
            case SERVER_THREADPOOL_EXHAUSTED_ERROR:
                return new OverloadException(errMsg);
            case CHANNEL_INACTIVE:
                return new ConnectionException(errMsg);
            case SERVICE_NOT_FOUND:
            case SERVICE_ERROR:
            case SERVER_ERROR:
            case CLIENT_ERROR:
                return new LafException(errMsg);
            default:
                return null;
        }
    }


}
