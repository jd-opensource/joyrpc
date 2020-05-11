package io.joyrpc.protocol.dubbo.serialization;

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

import io.joyrpc.codec.serialization.ObjectWriter;
import io.joyrpc.protocol.dubbo.DubboStatus;
import io.joyrpc.protocol.dubbo.message.DubboResponsePayload;

import java.io.IOException;

import static io.joyrpc.protocol.dubbo.DubboStatus.OK;
import static io.joyrpc.protocol.dubbo.message.DubboResponsePayload.*;

/**
 * DubboResponsePayload序列化
 */
public class DubboResponsePayloadWriter {
    /**
     * 写对象
     */
    protected ObjectWriter writer;

    public DubboResponsePayloadWriter(ObjectWriter reader) {
        this.writer = writer;
    }

    /**
     * 写应答载体
     *
     * @param payload 应答载体
     * @throws IOException
     */
    public void write(final DubboResponsePayload payload) throws IOException {
        //心跳响应，直接写null
        if (payload.isHeartbeat()) {
            writer.writeNull();
            return;
        }
        //序列化payload
        if (DubboStatus.getStatus(payload.getException()) == OK) {
            boolean attach = payload.isSupportResponseAttachment();
            Throwable th = payload.getException();
            if (th == null) {
                Object response = payload.getResponse();
                if (response == null) {
                    writer.writeInt(attach ? RESPONSE_NULL_VALUE_WITH_ATTACHMENTS : RESPONSE_NULL_VALUE);
                } else {
                    writer.writeInt(attach ? RESPONSE_VALUE_WITH_ATTACHMENTS : RESPONSE_VALUE);
                    writer.writeObject(response);
                }
            } else {
                writer.writeInt(attach ? RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS : RESPONSE_WITH_EXCEPTION);
                writer.writeObject(th);
            }

            if (attach) {
                // returns current version of Response to consumer side.
                writer.writeObject(payload.getAttachments());
            }
        } else {
            writer.writeString(payload.getException().getMessage());
        }
    }

}
