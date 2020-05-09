package io.joyrpc.protocol.dubbo.serialization.hessian;

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

import io.joyrpc.com.caucho.hessian.io.AbstractHessianOutput;
import io.joyrpc.com.caucho.hessian.io.AutowiredObjectSerializer;
import io.joyrpc.protocol.dubbo.DubboStatus;
import io.joyrpc.protocol.dubbo.message.DubboResponsePayload;

import java.io.IOException;

import static io.joyrpc.protocol.dubbo.DubboStatus.OK;
import static io.joyrpc.protocol.dubbo.message.DubboResponsePayload.*;

/**
 * DubboResponsePayload序列化
 */
public class DubboResponsePayloadSerializer implements AutowiredObjectSerializer {

    @Override
    public Class<?> getType() {
        return DubboResponsePayload.class;
    }

    @Override
    public void writeObject(Object obj, AbstractHessianOutput out) throws IOException {
        if (!(obj instanceof DubboResponsePayload)) {
            throw new IOException("Write dubbo response payload data failed, because payload class is error,  payload class is "
                    + obj.getClass());
        }
        DubboResponsePayload payload = (DubboResponsePayload) obj;
        //心跳响应，直接写null
        if (payload.isHeartbeat()) {
            out.writeNull();
            return;
        }
        //序列化payload
        if (DubboStatus.getStatus(payload.getException()) == OK) {
            boolean attach = payload.isSupportResponseAttachment();
            Throwable th = payload.getException();
            if (th == null) {
                Object response = payload.getResponse();
                if (response == null) {
                    out.writeInt(attach ? RESPONSE_NULL_VALUE_WITH_ATTACHMENTS : RESPONSE_NULL_VALUE);
                } else {
                    out.writeInt(attach ? RESPONSE_VALUE_WITH_ATTACHMENTS : RESPONSE_VALUE);
                    out.writeObject(response);
                }
            } else {
                out.writeInt(attach ? RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS : RESPONSE_WITH_EXCEPTION);
                out.writeObject(th);
            }

            if (attach) {
                // returns current version of Response to consumer side.
                out.writeObject(payload.getAttachments());
            }
        } else {
            out.writeString(payload.getException().getMessage());
        }
    }
}
