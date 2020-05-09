package io.joyrpc.protocol.dubbo;

import io.joyrpc.exception.*;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.transport.message.Header;

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

    public static byte getStatus(Throwable err) {

        if (!(err instanceof LafException)) {
            return OK;
        } else if (err instanceof ProtocolException) {
            Header header = ((ProtocolException) err).getHeader();
            MsgType msgType = header == null ? null : MsgType.valueOf(header.getMsgType());
            if (msgType == null || !msgType.isRequest()) {
                return BAD_RESPONSE;
            }
            return BAD_REQUEST;
        } else if (err instanceof TransportException) {
            return CHANNEL_INACTIVE;
        } else if (err instanceof OverloadException) {
            return SERVER_THREADPOOL_EXHAUSTED_ERROR;
        }

        return SERVICE_ERROR;

    }


}
