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

import io.joyrpc.com.caucho.hessian.io.AbstractHessianInput;
import io.joyrpc.com.caucho.hessian.io.AutowiredObjectDeserializer;
import io.joyrpc.protocol.dubbo.message.DubboResponsePayload;

import java.io.IOException;
import java.util.Map;

import static io.joyrpc.protocol.dubbo.message.DubboResponsePayload.*;

/**
 * DubboResponsePayload反序列化
 */
public class DubboResponsePayloadDeserializer implements AutowiredObjectDeserializer {

    @Override
    public Class<?> getType() {
        return DubboResponsePayload.class;
    }

    @Override
    public boolean isReadResolve() {
        return false;
    }

    @Override
    public Object readObject(AbstractHessianInput in) throws IOException {
        DubboResponsePayload payload = new DubboResponsePayload();
        int respFlag = in.readInt();
        switch (respFlag) {
            case RESPONSE_NULL_VALUE:
                break;
            case RESPONSE_VALUE:
                payload.setResponse(in.readObject());
                break;
            case RESPONSE_WITH_EXCEPTION:
                payload.setException((Throwable) in.readObject());
                break;
            case RESPONSE_NULL_VALUE_WITH_ATTACHMENTS: {
                Map<String, Object> attrs = (Map<String, Object>) in.readObject();
                if (attrs != null) {
                    payload.setAttachments(attrs);
                }
                break;
            }
            case RESPONSE_VALUE_WITH_ATTACHMENTS: {
                payload.setResponse(in.readObject());
                Map<String, Object> attrs = (Map<String, Object>) in.readObject();
                if (attrs != null) {
                    payload.setAttachments(attrs);
                }
                break;
            }
            case RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS: {
                payload.setException((Throwable) in.readObject());
                Map<String, Object> attrs = (Map<String, Object>) in.readObject();
                if (attrs != null) {
                    payload.setAttachments(attrs);
                }
                break;
            }
            default:
                throw new IOException("Unknown result flag, expect '0' '1' '2' '3' '4' '5', but received: " + respFlag);
        }
        return payload;
    }

    @Override
    public Object readList(AbstractHessianInput in, int length) throws IOException {
        return null;
    }

    @Override
    public Object readLengthList(AbstractHessianInput in, int length) throws IOException {
        return null;
    }

    @Override
    public Object readMap(AbstractHessianInput in) throws IOException {
        return null;
    }

    @Override
    public Object[] createFields(int len) {
        return new Object[0];
    }

    @Override
    public Object createField(String name) {
        return null;
    }

    @Override
    public Object readObject(AbstractHessianInput in, Object[] fields) throws IOException {
        return null;
    }

    @Override
    public Object readObject(AbstractHessianInput in, String[] fieldNames) throws IOException {
        return null;
    }
}
