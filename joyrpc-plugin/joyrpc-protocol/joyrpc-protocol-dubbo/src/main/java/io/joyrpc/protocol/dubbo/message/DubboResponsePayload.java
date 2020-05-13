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

import io.joyrpc.codec.serialization.Codec;
import io.joyrpc.codec.serialization.ObjectReader;
import io.joyrpc.codec.serialization.ObjectWriter;
import io.joyrpc.protocol.dubbo.DubboStatus;
import io.joyrpc.protocol.message.ResponsePayload;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.joyrpc.protocol.dubbo.DubboStatus.OK;
import static io.joyrpc.protocol.dubbo.DubboVersion.LOWEST_VERSION_FOR_RESPONSE_ATTACHMENT;
import static io.joyrpc.protocol.dubbo.DubboVersion.getIntVersion;

/**
 * Dubbo应答消息
 */
public class DubboResponsePayload extends ResponsePayload implements Codec {

    public static final byte RESPONSE_WITH_EXCEPTION = 0;
    public static final byte RESPONSE_VALUE = 1;
    public static final byte RESPONSE_NULL_VALUE = 2;
    public static final byte RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS = 3;
    public static final byte RESPONSE_VALUE_WITH_ATTACHMENTS = 4;
    public static final byte RESPONSE_NULL_VALUE_WITH_ATTACHMENTS = 5;
    protected static final Map<Object, Object> EMPTY_ATTACHMENTS = new HashMap<>(0);

    /**
     * dubbo版本
     */
    protected transient String dubboVersion;
    /**
     * 心跳标识
     */
    protected transient boolean heartbeat = false;
    /**
     * 扩展属性
     */
    protected Map<String, Object> attachments;

    public DubboResponsePayload() {
    }

    public DubboResponsePayload(boolean heartbeat) {
        this.heartbeat = heartbeat;
    }

    public DubboResponsePayload(Object response, Throwable exception, String dubboVersion) {
        super(response, exception);
        this.dubboVersion = dubboVersion;
    }

    public DubboResponsePayload(String dubboVersion) {
        this.dubboVersion = dubboVersion;
    }

    public Map<String, Object> getAttachments() {
        return attachments;
    }

    public void setAttachments(Map<String, Object> attachments) {
        this.attachments = attachments;
    }

    public String getDubboVersion() {
        return dubboVersion;
    }

    public void setDubboVersion(String dubboVersion) {
        this.dubboVersion = dubboVersion;
    }

    public boolean isHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(boolean heartbeat) {
        this.heartbeat = heartbeat;
    }

    public boolean isSupportResponseAttachment() {
        if (dubboVersion == null || dubboVersion.isEmpty()) {
            return false;
        }
        // for previous dubbo version(2.0.10/020010~2.6.2/020602), this version is the jar's version, so they need to
        // be ignore
        int iVersion = getIntVersion(dubboVersion);
        if (iVersion >= 2001000 && iVersion < 2060300) {
            return false;
        }

        // 2.8.x is reserved for dubbox
        if (iVersion >= 2080000 && iVersion < 2090000) {
            return false;
        }

        return iVersion >= LOWEST_VERSION_FOR_RESPONSE_ATTACHMENT;
    }

    /**
     * 读取应答载体
     *
     * @param reader 读取器
     * @throws IOException
     */
    public void decode(final ObjectReader reader) throws IOException {
        int respFlag = reader.readInt();
        switch (respFlag) {
            case RESPONSE_NULL_VALUE:
                break;
            case RESPONSE_VALUE:
                response = reader.readObject();
                break;
            case RESPONSE_WITH_EXCEPTION:
                exception = (Throwable) reader.readObject();
                break;
            case RESPONSE_NULL_VALUE_WITH_ATTACHMENTS:
                attachments = (Map<String, Object>) reader.readObject();
                break;
            case RESPONSE_VALUE_WITH_ATTACHMENTS:
                response = reader.readObject();
                attachments = (Map<String, Object>) reader.readObject();
                break;
            case RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS:
                exception = (Throwable) reader.readObject();
                attachments = (Map<String, Object>) reader.readObject();
                break;
            default:
                throw new IOException("Unknown result flag, expect '0' '1' '2' '3' '4' '5', but received: " + respFlag);
        }
    }

    /**
     * 写应答载体
     *
     * @param writer 写入器
     * @throws IOException
     */
    public void encode(final ObjectWriter writer) throws IOException {
        //心跳响应，直接写null
        if (isHeartbeat()) {
            writer.writeObject(null);
            return;
        }
        //序列化payload
        if (DubboStatus.getStatus(exception) == OK) {
            boolean attach = isSupportResponseAttachment();
            Throwable th = exception;
            if (th == null) {
                if (response == null) {
                    writer.writeByte(attach ? RESPONSE_NULL_VALUE_WITH_ATTACHMENTS : RESPONSE_NULL_VALUE);
                } else {
                    writer.writeByte(attach ? RESPONSE_VALUE_WITH_ATTACHMENTS : RESPONSE_VALUE);
                    writer.writeObject(response);
                }
            } else {
                writer.writeByte(attach ? RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS : RESPONSE_WITH_EXCEPTION);
                writer.writeObject(th);
            }
            if (attach) {
                // returns current version of Response to consumer side.
                writer.writeObject(attachments == null ? EMPTY_ATTACHMENTS : attachments);
            }
        } else {
            writer.writeString(exception.getMessage());
        }
    }

}
