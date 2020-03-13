package io.joyrpc.transport.netty4.http2;

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
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.codec.DecodeContext;
import io.joyrpc.transport.codec.Http2Codec;
import io.joyrpc.transport.http2.DefaultHttp2ResponseMessage;
import io.joyrpc.transport.http2.Http2RequestMessage;
import io.joyrpc.transport.netty4.buffer.NettyChannelBuffer;
import io.joyrpc.transport.netty4.transport.NettyServerTransport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.util.CharsetUtil.UTF_8;

/**
 * http2 client端 编解码器
 */
public class Http2ClientCodecHandler extends Http2ConnectionHandler {

    private static final Logger logger = LoggerFactory.getLogger(NettyServerTransport.class);

    /**
     * 流Key
     */
    protected Http2Connection.PropertyKey streamKey;
    /**
     * 头部Key
     */
    protected Http2Connection.PropertyKey headerKey;
    /**
     * 编解码
     */
    protected Http2Codec codec;
    /**
     * 通道
     */
    protected Channel channel;

    /**
     * 构造函数
     *
     * @param decoder
     * @param encoder
     * @param initialSettings
     * @param channel
     * @param codec
     */
    public Http2ClientCodecHandler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder,
                                   Http2Settings initialSettings, Channel channel, Http2Codec codec) {
        super(decoder, encoder, initialSettings);
        this.channel = channel;
        this.codec = codec;
        this.streamKey = encoder.connection().newKey();
        this.headerKey = encoder.connection().newKey();
        // Set the frame listener on the decoder.
        this.decoder().frameListener(new FrameListener());
        this.encoder().connection().addListener(new Http2ConnectionAdapter() {
            @Override
            public void onGoAwayReceived(int lastStreamId, long errorCode, ByteBuf debugData) {
                byte[] debugDataBytes = ByteBufUtil.getBytes(debugData);
                goingAway();
                if (errorCode == Http2Error.ENHANCE_YOUR_CALM.code()) {
                    String data = new String(debugDataBytes, UTF_8);
                    logger.warn("Received GOAWAY with ENHANCE_YOUR_CALM. Debug data: {}", data);
                }
            }
        });
    }

    protected void goingAway() {
        final int lastKnownStream = connection().local().lastStreamKnownByPeer();
        try {
            connection().forEachActiveStream(stream -> {
                        if (stream.id() > lastKnownStream) {
                            stream.close();
                        }
                        return true;
                    }
            );
        } catch (Http2Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg == null || !(msg instanceof Http2RequestMessage)) {
            super.write(ctx, msg, promise);
            return;
        }
        int streamId = connection().local().incrementAndGetNextStreamId();
        //request 对象
        Http2RequestMessage request = (Http2RequestMessage) msg;
        request.setStreamId(streamId);
        //write header
        if (request.headers() != null && !request.headers().isEmpty()) {
            //构建http2响应header
            Http2Headers http2Headers = new DefaultHttp2Headers(false);
            request.headers().getAll().forEach((k, v) -> http2Headers.add(k, v.toString()));
            //write header
            encoder().writeHeaders(ctx, streamId, http2Headers, 0, false, promise).addListener(
                    f -> {
                        Http2Stream http2Stream = connection().stream(streamId);
                        http2Stream.setProperty(streamKey, request.getBizMsgId());
                    }
            );
        }
        //write data
        if (request.content() != null) {
            ByteBuf byteBuf = ctx.alloc().buffer();
            try {
                codec.encode(new Http2EncodeContext(channel).attribute(Http2Codec.HEADER, request.headers()),
                        new NettyChannelBuffer(byteBuf), request.content());
            } catch (CodecException e) {
                byteBuf.release();
                throw e;
            }
            encoder().writeData(ctx, streamId, byteBuf, 0, true, ctx.voidPromise());
        }
    }

    /**
     * 处理请求
     *
     * @param ctx
     * @param streamId
     * @param bizMsgId
     * @param http2Headers
     * @param body
     * @throws Http2Exception
     */
    protected void handleRequest(ChannelHandlerContext ctx, int streamId, int bizMsgId,
                                 Http2Headers http2Headers, ByteBuf body) throws Http2Exception {
        try {
            //获取server端响应header
            io.joyrpc.transport.http2.Http2Headers respHeaders = new io.joyrpc.transport.http2.DefaultHttp2Headers();
            if (http2Headers != null) {
                http2Headers.forEach(t -> respHeaders.set(t.getKey().toString(), t.getValue().toString()));
            }
            //获取server端响应body
            byte[] content = null;
            if (body != null) {
                DecodeContext deCtx = new Http2DecodeContext(channel);
                content = (byte[]) codec.decode(deCtx, new NettyChannelBuffer(body));
            }
            //触发下一个channelread
            ctx.fireChannelRead(new DefaultHttp2ResponseMessage(streamId, bizMsgId, respHeaders, content));
        } catch (Exception e) {
            throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, e, "has error when codec");
        }
    }

    protected void handleEndHeader(ChannelHandlerContext ctx, int streamId, int bizMsgId,
                                   Http2Headers http2Headers) throws Http2Exception {
        try {
            //获取server端响应header
            io.joyrpc.transport.http2.Http2Headers respEndHeaders = new io.joyrpc.transport.http2.DefaultHttp2Headers();
            http2Headers.forEach(t -> respEndHeaders.set(t.getKey().toString(), t.getValue().toString()));
            //触发下一个channelread
            ctx.fireChannelRead(new DefaultHttp2ResponseMessage(streamId, bizMsgId, null, null, respEndHeaders));
        } catch (Exception e) {
            throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, e, "has error when codec");
        }
    }

    protected class FrameListener extends Http2FrameAdapter {
        @Override
        public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data,
                              int padding, boolean endOfStream) throws Http2Exception {
            Http2Stream http2Stream = connection().stream(streamId);
            //根据streamKey,获取缓存的bizId
            int bizMsgId = http2Stream.getProperty(streamKey);
            Http2Headers headers = http2Stream.getProperty(headerKey);
            handleRequest(ctx, streamId, bizMsgId, headers, data);
            return padding;
        }

        @Override
        public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers,
                                  int padding, boolean endStream) throws Http2Exception {
            if (streamId > 0) {
                onHeadersRead(ctx, streamId, headers, endStream);
            } else {
                super.onHeadersRead(ctx, streamId, headers, padding, endStream);
            }

        }

        @Override
        public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers,
                                  int streamDependency, short weight, boolean exclusive,
                                  int padding, boolean endStream) throws Http2Exception {
            if (streamId > 0) {
                onHeadersRead(ctx, streamId, headers, endStream);
            } else {
                super.onHeadersRead(ctx, streamId, headers, padding, endStream);
            }
        }

        private void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, boolean endStream) throws Http2Exception {
            if (!endStream) {
                // 缓存起来
                connection().stream(streamId).setProperty(headerKey, headers);
            } else {
                Http2Stream http2Stream = connection().stream(streamId);
                //根据streamKey,获取缓存的bizId
                int bizMsgId = http2Stream.getProperty(streamKey);
                handleEndHeader(ctx, streamId, bizMsgId, headers);
            }
        }

        @Override
        public void onPingRead(ChannelHandlerContext ctx, long data) throws Http2Exception {
            super.onPingRead(ctx, data);
        }
    }

}
