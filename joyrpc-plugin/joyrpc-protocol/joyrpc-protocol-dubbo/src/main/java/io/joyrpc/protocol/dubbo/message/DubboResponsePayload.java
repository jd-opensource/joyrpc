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

import io.joyrpc.protocol.message.ResponsePayload;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static io.joyrpc.protocol.dubbo.DubboVersion.LOWEST_VERSION_FOR_RESPONSE_ATTACHMENT;
import static io.joyrpc.protocol.dubbo.DubboVersion.getIntVersion;

/**
 * Dubbo应答消息
 */
public class DubboResponsePayload extends ResponsePayload {

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

    public static final byte RESPONSE_WITH_EXCEPTION = 0;
    public static final byte RESPONSE_VALUE = 1;
    public static final byte RESPONSE_NULL_VALUE = 2;
    public static final byte RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS = 3;
    public static final byte RESPONSE_VALUE_WITH_ATTACHMENTS = 4;
    public static final byte RESPONSE_NULL_VALUE_WITH_ATTACHMENTS = 5;

    /**
     * 异常转换消息状态
     */
    protected static final Function<Throwable, Byte> CONVERT_STATUS_FUNC = err -> {
        if (err == null) {
            return OK;
        }
        return BAD_REQUEST;
    };

    private transient String dubboVersion;

    private transient byte status = OK;

    private transient boolean heartbeat = false;

    private Map<String, Object> attachments = new HashMap<>();

    public DubboResponsePayload() {
    }

    public DubboResponsePayload(boolean heartbeat) {
        this.heartbeat = heartbeat;
    }

    public DubboResponsePayload(Object response, Throwable exception, String dubboVersion) {
        super(response, exception);
        this.dubboVersion = dubboVersion;
        this.status = CONVERT_STATUS_FUNC.apply(exception);
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

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
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

}
