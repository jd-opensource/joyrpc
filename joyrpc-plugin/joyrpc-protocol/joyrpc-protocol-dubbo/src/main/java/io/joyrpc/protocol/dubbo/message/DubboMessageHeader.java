package io.joyrpc.protocol.dubbo.message;

import io.joyrpc.constants.Head;
import io.joyrpc.protocol.message.MessageHeader;

import java.util.Map;

/**
 * dubbo消息头
 */
public class DubboMessageHeader extends MessageHeader {


    public static final Head HEAD_STATUS = new Head((byte) 20, Byte.class);

    /**
     * 应答状态
     */
    protected byte status;
    /**
     * 双向标识
     */
    protected boolean twoWay;
    /**
     * dubbo版本
     */
    protected String dubboVersion;

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }


    public boolean isTwoWay() {
        return twoWay;
    }

    public void setTwoWay(boolean twoWay) {
        this.twoWay = twoWay;
    }

    public String getDubboVersion() {
        return dubboVersion;
    }

    public void setDubboVersion(String dubboVersion) {
        this.dubboVersion = dubboVersion;
    }

    @Override
    public MessageHeader response(byte msgType, byte compression, Map<Byte, Object> attributes) {
        DubboMessageHeader result = new DubboMessageHeader();
        result.msgId = msgId;
        result.msgType = msgType;
        result.serialization = serialization;
        result.protocolType = protocolType;
        result.compression = compression;
        result.sessionId = sessionId;
        result.attributes = attributes;
        result.dubboVersion = dubboVersion;
        result.twoWay = twoWay;
        result.status = status;
        return result;
    }

    /**
     * 复制
     *
     * @param header 头
     */
    protected void copy(final DubboMessageHeader header) {
        super.copy(header);
        status = header.status;
        twoWay = header.twoWay;
        dubboVersion = header.dubboVersion;
    }

    @Override
    public DubboMessageHeader clone() {
        DubboMessageHeader result = new DubboMessageHeader();
        result.copy(this);
        return result;
    }
}
