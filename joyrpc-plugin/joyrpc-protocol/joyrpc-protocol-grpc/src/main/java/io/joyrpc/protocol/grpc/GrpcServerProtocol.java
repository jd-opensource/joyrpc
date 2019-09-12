package io.joyrpc.protocol.grpc;

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

import io.joyrpc.exception.RpcException;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.protocol.AbstractProtocol;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.ServerProtocol;
import io.joyrpc.protocol.grpc.handler.GrpcServerConvertHandler;
import io.joyrpc.protocol.handler.RequestChannelHandler;
import io.joyrpc.protocol.handler.ResponseChannelHandler;
import io.joyrpc.protocol.message.MessageHeader;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelHandlerChain;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.codec.Http2Codec;
import io.joyrpc.transport.http2.DefaultHttp2ResponseMessage;
import io.joyrpc.transport.http2.Http2Headers;
import io.joyrpc.transport.http2.Http2ResponseMessage;

import static io.joyrpc.Plugin.MESSAGE_HANDLER_SELECTOR;
import static io.joyrpc.protocol.Protocol.GRPC_ORDER;

/**
 * @date: 2019/4/11
 */
@Extension(value = "grpc", order = GRPC_ORDER)
@ConditionalOnClass("io.grpc.Codec")
public class GrpcServerProtocol extends AbstractProtocol implements ServerProtocol {

    /**
     * HTTP/2协议头的魔术位
     */
    public static final byte[] GRPC_MAGICCODEBYTE = new byte[]{(byte) 0x50, (byte) 0x52};

    public static final String GRPC_NAME = "grpc";

    public static final byte GRPC_NUMBER = 10;

    @Override
    public ChannelHandlerChain buildChain() {
        if (chain == null) {
            chain = new ChannelHandlerChain()
                    .addLast(new GrpcServerConvertHandler())
                    .addLast(new RequestChannelHandler<>(MESSAGE_HANDLER_SELECTOR, this::onException))
                    .addLast(new ResponseChannelHandler());
        }

        return chain;
    }

    @Override
    protected Codec createCodec() {
        return Http2Codec.INSTANCE;
    }

    @Override
    public byte[] getMagicCode() {
        return GRPC_MAGICCODEBYTE;
    }

    @Override
    protected void onException(Channel channel, MessageHeader header, RpcException cause) {
        int streamId = (Integer) header.getAttributes().get(HeaderMapping.STREAM_ID.getNum());
        int msgId = header.getMsgId();
        Http2Headers endHeaders = GrpcServerConvertHandler.buildErrorEndHttp2Headers(cause);
        Http2ResponseMessage http2ResponseMessage = new DefaultHttp2ResponseMessage(streamId, msgId, null, null, endHeaders);
        channel.send(http2ResponseMessage, (event -> {
            if (!event.isSuccess()) {
                String address = Channel.toString(channel.getRemoteAddress()) + "->" + Channel.toString(channel.getLocalAddress());
                logger.error("Error occurs while sending " + MsgType.valueOf(header.getMsgType()) + " message to " + address, cause.getMessage());
            }
        }));
    }

}
