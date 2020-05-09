package io.joyrpc.protocol.dubbo.message;

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
    public MessageHeader response(final byte msgType, final byte compression, final Map<Byte, Object> attributes) {
        DubboMessageHeader result = clone();
        result.msgType = msgType;
        result.compression = compression;
        result.attributes = attributes;
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
