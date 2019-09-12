package io.joyrpc.transport.buffer;

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

import java.io.OutputStream;

/**
 * @date: 2019/3/25
 */
public class ChannelBufferOutputStream extends OutputStream {

    protected ChannelBuffer buffer;

    public ChannelBufferOutputStream(ChannelBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void write(final int b) {
        buffer.writeByte(b);
    }

    @Override
    public void write(final byte[] bytes) {
        buffer.writeBytes(bytes);
    }

    @Override
    public void write(final byte[] bytes, final int offset, final int length) {
        if (length <= 0) {
            return;
        }
        buffer.writeBytes(bytes, offset, length);
    }

}
