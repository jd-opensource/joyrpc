package io.joyrpc.protocol.dubbo.message;

import io.joyrpc.protocol.message.MessageHeader;

/**
 * dubbo消息头
 */
public class DubboMessageHeader extends MessageHeader {

    /**
     * 应答状态
     */
    protected byte status;

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    @Override
    public DubboMessageHeader clone() {
        DubboMessageHeader result = new DubboMessageHeader();
        result.msgId = msgId;
        result.serialization = serialization;
        result.msgType = msgType;
        result.protocolType = protocolType;
        result.timeout = timeout;
        result.compression = compression;
        result.length = length;
        result.headerLength = headerLength;
        result.sessionId = sessionId;
        result.attributes = attributes;
        result.status = status;
        return result;
    }
}
