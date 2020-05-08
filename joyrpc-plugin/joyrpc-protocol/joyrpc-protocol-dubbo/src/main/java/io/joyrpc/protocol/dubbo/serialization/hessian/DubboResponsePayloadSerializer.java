package io.joyrpc.protocol.dubbo.serialization.hessian;

import io.joyrpc.com.caucho.hessian.io.AbstractHessianOutput;
import io.joyrpc.com.caucho.hessian.io.AutowiredObjectSerializer;
import io.joyrpc.protocol.dubbo.message.DubboResponsePayload;

import java.io.IOException;

import static io.joyrpc.protocol.dubbo.message.DubboResponsePayload.*;

public class DubboResponsePayloadSerializer implements AutowiredObjectSerializer {

    @Override
    public Class<?> getType() {
        return DubboResponsePayload.class;
    }

    @Override
    public void writeObject(Object obj, AbstractHessianOutput out) throws IOException {
        if (!(obj instanceof DubboResponsePayload)) {
            throw new IOException("Write dubbo response payload data failed, because payload class is error,  payload class"
                    + obj.getClass());
        }
        DubboResponsePayload payload = (DubboResponsePayload) obj;
        //心跳响应，直接写null
        if (payload.isHeartbeat()) {
            out.writeNull();
            return;
        }
        //序列化payload
        if (payload.getStatus() == OK) {
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
