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

import io.joyrpc.codec.serialization.ObjectInputReader;
import io.joyrpc.codec.serialization.ObjectReader;
import io.joyrpc.codec.serialization.ObjectWriter;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Dubbo异常应答消息
 */
public class DubboResponseErrorPayload extends DubboResponsePayload {
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
     * java反序列化
     *
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        read(new ObjectInputReader(in));
    }

    /**
     * 读取调用
     *
     * @param reader 读取器
     * @throws IOException
     */
    public DubboResponseErrorPayload read(final ObjectReader reader) throws IOException {
        this.exceptionMessage = reader.readString();
        return this;
    }

    /**
     * 读取调用
     *
     * @param writer 写入器
     * @throws IOException
     */
    public void write(final ObjectWriter writer) throws IOException {
        writer.writeString(exceptionMessage);
    }
}
