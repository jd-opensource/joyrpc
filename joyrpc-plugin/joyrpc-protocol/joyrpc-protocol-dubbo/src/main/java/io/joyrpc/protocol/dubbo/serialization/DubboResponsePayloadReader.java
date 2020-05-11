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

import io.joyrpc.codec.serialization.ObjectReader;
import io.joyrpc.protocol.dubbo.message.DubboResponsePayload;

import java.io.IOException;
import java.util.Map;

import static io.joyrpc.protocol.dubbo.message.DubboResponsePayload.*;

/**
 * DubboResponsePayload反序列化
 */
public class DubboResponsePayloadReader {
    /**
     * 读对象
     */
    protected ObjectReader reader;

    public DubboResponsePayloadReader(ObjectReader reader) {
        this.reader = reader;
    }

    /**
     * 读取应答载体
     *
     * @param payload 应答载体
     * @throws IOException
     */
    public void read(final DubboResponsePayload payload) throws IOException {
        int respFlag = reader.readInt();
        switch (respFlag) {
            case RESPONSE_NULL_VALUE:
                break;
            case RESPONSE_VALUE:
                payload.setResponse(reader.readObject());
                break;
            case RESPONSE_WITH_EXCEPTION:
                payload.setException((Throwable) reader.readObject());
                break;
            case RESPONSE_NULL_VALUE_WITH_ATTACHMENTS: {
                Map<String, Object> attrs = (Map<String, Object>) reader.readObject();
                if (attrs != null) {
                    payload.setAttachments(attrs);
                }
                break;
            }
            case RESPONSE_VALUE_WITH_ATTACHMENTS: {
                payload.setResponse(reader.readObject());
                Map<String, Object> attrs = (Map<String, Object>) reader.readObject();
                if (attrs != null) {
                    payload.setAttachments(attrs);
                }
                break;
            }
            case RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS: {
                payload.setException((Throwable) reader.readObject());
                Map<String, Object> attrs = (Map<String, Object>) reader.readObject();
                if (attrs != null) {
                    payload.setAttachments(attrs);
                }
                break;
            }
            default:
                throw new IOException("Unknown result flag, expect '0' '1' '2' '3' '4' '5', but received: " + respFlag);
        }
    }

    /**
     * 读取应答载体
     *
     * @return 应答载体
     * @throws IOException
     */
    public DubboResponsePayload read() throws IOException {
        DubboResponsePayload result = new DubboResponsePayload();
        read(result);
        return result;
    }
}
