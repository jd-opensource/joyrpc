package io.joyrpc.transport.resteasy.codec;

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
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.codec.DecodeContext;
import io.joyrpc.transport.codec.EncodeContext;
import org.jboss.resteasy.plugins.server.netty.RequestDispatcher;

/**
 * Resteasy编解码
 */
public class ResteasyCodec implements Codec {

    protected final String root;

    protected final RequestDispatcher dispatcher;

    public ResteasyCodec(String root, RequestDispatcher dispatcher) {
        this.root = root;
        this.dispatcher = dispatcher;
    }

    @Override
    public String binder() {
        return "resteasy";
    }

    @Override
    public Object decode(DecodeContext context, ChannelBuffer buffer) throws CodecException {
        return null;
    }

    @Override
    public void encode(EncodeContext context, ChannelBuffer buffer, Object message) throws CodecException {

    }

    public String getRoot() {
        return root;
    }

    public RequestDispatcher getDispatcher() {
        return dispatcher;
    }

}
