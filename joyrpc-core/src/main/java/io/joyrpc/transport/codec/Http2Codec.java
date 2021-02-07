package io.joyrpc.transport.codec;

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

import io.joyrpc.exception.CodecException;
import io.joyrpc.transport.buffer.ChannelBuffer;

/**
 * http2编解码
 */
public class Http2Codec implements Codec {

    public static final Http2Codec INSTANCE = new Http2Codec();

    public static String HEADER = "http2Header";

    @Override
    public Object decode(final DecodeContext context, final ChannelBuffer buffer) throws CodecException {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

    @Override
    public void encode(final EncodeContext context, final ChannelBuffer buffer, final Object message) throws CodecException {
        if (message instanceof byte[]) {
            buffer.writeBytes((byte[]) message);
        }
    }

    @Override
    public String binder() {
        return "http2";
    }
}
