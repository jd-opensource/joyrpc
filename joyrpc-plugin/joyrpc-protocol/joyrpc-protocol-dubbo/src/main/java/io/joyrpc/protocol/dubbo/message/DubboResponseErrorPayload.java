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

import java.io.IOException;

/**
 * Dubbo异常应答消息
 */
public class DubboResponseErrorPayload extends DubboResponsePayload implements Codec {
    /**
     * 异常信息
     */
    protected String exceptionMessage;

    public DubboResponseErrorPayload() {
    }

    public DubboResponseErrorPayload(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    /**
     * 读取调用
     *
     * @param reader 读取器
     * @throws IOException
     */
    public void decode(final ObjectReader reader) throws IOException {
        this.exceptionMessage = reader.readString();
    }

    /**
     * 读取调用
     *
     * @param writer 写入器
     * @throws IOException
     */
    public void encode(final ObjectWriter writer) throws IOException {
        writer.writeString(exceptionMessage);
    }
}
