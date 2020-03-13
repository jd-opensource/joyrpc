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
import io.joyrpc.transport.codec.Http2Codec;
import io.joyrpc.transport.http2.DefaultHttp2RequestMessage;
import io.joyrpc.transport.http2.Http2ResponseMessage;
import io.joyrpc.transport.netty4.buffer.NettyChannelBuffer;
import io.joyrpc.transport.netty4.transport.NettyServerTransport;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * http2 server端 编解码器
 *
 * @date: 2019/4/10
 */
public class Http2ServerCodecHandler extends Http2ConnectionHandler {

    private static final Logger logger = LoggerFactory.getLogger(NettyServerTransport.class);

    protected Http2Connection.PropertyKey headerKey;

    protected Http2Codec codec;

    protected Channel channel;

    public Http2ServerCodecHandler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder,
                                   Http2Settings initialSettings, Channel channel, Http2Codec codec) {
        super(decoder, encoder, initialSettings);
        this.channel = channel;
        this.codec = codec;
        this.headerKey = encoder().connection().newKey();
        this.decoder().frameListener(new FrameListener());
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
        if (msg == null || !(msg instanceof Http2ResponseMessage)) {
            super.write(ctx, msg, promise);
            return;
        }
        //response对象
        Http2ResponseMessage response = (Http2ResponseMessage) msg;
        //应答头
        if (response.headers() != null && !response.headers().isEmpty()) {
            //构建http2响应header
            Http2Headers http2Headers = new DefaultHttp2Headers();
            response.headers().getAll().forEach((k, v) -> http2Headers.add(k, v.toString()));
            //write
            encoder().writeHeaders(ctx, response.getStreamId(), http2Headers, 0, false, promise);
        }
        //是否有结束头
        boolean withEndHeaders = response.endHeaders() != null;
        //写应答内容
        if (response.content() != null) {
            ByteBuf byteBuf = ctx.alloc().buffer();
            try {
                codec.encode(new Http2EncodeContext(channel).attribute(Http2Codec.HEADER, response.headers()),
                        new NettyChannelBuffer(byteBuf), response.content());
            } catch (CodecException e) {
                byteBuf.release();
                throw e;
            }
            encoder().writeData(ctx, response.getStreamId(), byteBuf, 0, !withEndHeaders, ctx.voidPromise());
        }
        //write end header
        if (withEndHeaders) {
            Http2Headers endHeaders = new DefaultHttp2Headers();
            response.endHeaders().getAll().forEach((k, v) -> endHeaders.add(k, v.toString()));
            //write
            encoder().writeHeaders(ctx, response.getStreamId(), endHeaders, 0, true, promise);
        }
    }

    protected void handleRequest(ChannelHandlerContext ctx, int streamId, Http2Headers http2Headers, ByteBuf body) throws Http2Exception {
        try {
            //获取请求header
            io.joyrpc.transport.http2.Http2Headers reqHeaders = new io.joyrpc.transport.http2.DefaultHttp2Headers();
            if (http2Headers != null) {
                http2Headers.forEach(t -> {
                    try {
                        reqHeaders.set(t.getKey().toString(), URLDecoder.decode(t.getValue().toString(), "UTF-8"));
                    } catch (UnsupportedEncodingException ignored) {
                    }
                });
            }
            //获取请求body
            byte[] content = body != null ? (byte[]) codec.decode(new Http2DecodeContext(channel), new NettyChannelBuffer(body)) : null;
            //触发下一个channelread
            //server端收到消息，没有bizId，这里用streamId充当bizId
            ctx.fireChannelRead(new DefaultHttp2RequestMessage(streamId, streamId, reqHeaders, content));
        } catch (Exception e) {
            //TODO 异常处理
            throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, e, "has error when codec");
        }

    }

    /**
     * 监听器
     */
    protected class FrameListener extends Http2FrameAdapter {

        protected boolean firstSettings = true;

        @Override
        public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) {
            if (firstSettings) {
                firstSettings = false;
            }
        }

        @Override
        public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) throws Http2Exception {
            int processed = data.readableBytes() + padding;
            if (endOfStream) {
                Http2Stream http2Stream = connection().stream(streamId);
                // read cached http2 header from stream
                Http2Headers headers = http2Stream.getProperty(headerKey);
                handleRequest(ctx, streamId, headers, data);
            }
            return processed;
        }

        @Override
        public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endStream) throws Http2Exception {
            if (streamId > 0) {
                // 正常的请求（streamId==1 的是settings请求）
                if (endStream) {
                    // 没有DATA帧的请求，可能是DATA
                    handleRequest(ctx, streamId, headers, null);
                } else {
                    // 缓存起来
                    Http2Stream stream = connection().stream(streamId);
                    if (stream != null) {
                        stream.setProperty(headerKey, headers);
                    }
                }
            }
        }

        @Override
        public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers,
                                  int streamDependency, short weight, boolean exclusive, int padding, boolean endStream)
                throws Http2Exception {

            if (streamId > 0) {
                // 正常的请求（streamId==1 的是settings请求）
                if (endStream) {
                    // 没有DATA帧的请求，可能是DATA
                    handleRequest(ctx, streamId, headers, null);
                } else {
                    // 缓存起来
                    Http2Stream stream = connection().stream(streamId);
                    if (stream != null) {
                        stream.setProperty(headerKey, headers);
                    }
                }
            }
        }

        @Override
        public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) {
            logger.error("onRstStreamRead streamId:" + streamId + " errorCode:" + errorCode);
        }

        @Override
        public void onPingRead(ChannelHandlerContext ctx, long data) throws Http2Exception {
            logger.warn("onPingRead data:" + data);
        }

        @Override
        public void onPingAckRead(ChannelHandlerContext ctx, long data) throws Http2Exception {
            logger.warn("onPingAckRead data:" + data);
        }

    }
}
