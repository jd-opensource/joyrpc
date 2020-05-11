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
import io.joyrpc.protocol.dubbo.message.DubboResponseErrorPayload;

import java.io.IOException;

/**
 * DubboResponseErrorPayload反序列化
 */
public class DubboResponseErrorPayloadReader {
    /**
     * 读对象
     */
    protected ObjectReader reader;

    public DubboResponseErrorPayloadReader(ObjectReader reader) {
        this.reader = reader;
    }

    /**
     * 读取应答异常载体
     *
     * @param payload 应答异常载体
     * @throws IOException
     */
    public void read(final DubboResponseErrorPayload payload) throws IOException {
        payload.setExceptionMessage(reader.readString());
    }

    /**
     * 读取应答异常载体
     *
     * @return 应答异常载体
     * @throws IOException
     */
    public DubboResponseErrorPayload read() throws IOException {
        DubboResponseErrorPayload result = new DubboResponseErrorPayload();
        read(result);
        return result;
    }
}
