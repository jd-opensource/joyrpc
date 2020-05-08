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
    /**
     * 双向标识
     */
    protected boolean twoWay;

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

    /**
     * 复制
     *
     * @param header 头
     */
    protected void copy(final DubboMessageHeader header) {
        super.copy(header);
        status = header.status;
        twoWay = header.twoWay;
    }

    @Override
    public DubboMessageHeader clone() {
        DubboMessageHeader result = new DubboMessageHeader();
        result.copy(this);
        return result;
    }
}
