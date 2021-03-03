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

import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.codec.Http2Codec;
import io.joyrpc.transport.http2.DefaultHttp2RequestMessage;
import io.joyrpc.transport.http2.Http2ResponseMessage;
import io.joyrpc.transport.netty4.buffer.NettyChannelBuffer;
import io.joyrpc.transport.netty4.transport.NettyServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.*;
import io.netty.handler.codec.http2.Http2Connection.PropertyKey;
import io.netty.handler.logging.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * http2 server端 编解码器
 */
public class Http2ServerCodecHandler extends Http2ConnectionHandler {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);
    protected final Http2ConnectionDecoder decoder;
    protected final Http2ConnectionEncoder encoder;
    /**
     * 消息头键
     */
    protected PropertyKey headerKey;
    /**
     * 编解码
     */
    protected Http2Codec codec;
    /**
     * 通道
     */
    protected Channel channel;

    public Http2ServerCodecHandler(Http2ConnectionDecoder decoder,
                                   Http2ConnectionEncoder encoder,
                                   Http2Settings initialSettings,
                                   Channel channel,
                                   Http2Codec codec) {
        super(decoder, encoder, initialSettings);
        this.decoder = decoder;
        this.encoder = encoder;
        this.channel = channel;
        this.codec = codec;
        this.headerKey = encoder().connection().newKey();
        decoder.frameListener(new FrameListener(encoder.connection(), headerKey, codec, channel));
    }


    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
        if (msg instanceof Http2ResponseMessage) {
            //response对象
            Http2ResponseMessage response = (Http2ResponseMessage) msg;
            //构建http2响应header
            Http2Headers headers = response.headers() == null ? null : new Http2NettyHeaders(response.headers().getAll());
            if (response.content() == null) {
                if (headers != null) {
                    encoder.writeHeaders(ctx, response.getStreamId(), headers, 0, response.isEnd(), promise);
                }
            } else {
                if (headers != null) {
                    encoder.writeHeaders(ctx, response.getStreamId(), headers, 0, false, promise);
                }
                encoder.writeData(ctx, response.getStreamId(), Unpooled.wrappedBuffer(response.content()), 0, response.isEnd(), ctx.voidPromise());
            }
        } else {
            super.write(ctx, msg, promise);
        }
    }

    /**
     * 创建http2服务端编解码处理器
     *
     * @param channel    通道
     * @param http2Codec http2编解码
     * @return http2服务端编解码处理器
     */
    public static Http2ServerCodecHandler create(final Channel channel, final Http2Codec http2Codec) {
        Http2FrameLogger frameLogger = new Http2FrameLogger(LogLevel.DEBUG, Http2ServerCodecHandler.class);
        int payload = channel.getPayloadSize();

        Http2HeadersDecoder headersDecoder = new DefaultHttp2HeadersDecoder(true, payload);
        Http2FrameReader frameReader = new Http2InboundFrameLogger(new DefaultHttp2FrameReader(headersDecoder), frameLogger);
        Http2FrameWriter frameWriter = new Http2OutboundFrameLogger(new DefaultHttp2FrameWriter(), frameLogger);

        Http2Connection connection = new DefaultHttp2Connection(true);
        Http2ConnectionEncoder encoder = new DefaultHttp2ConnectionEncoder(connection, frameWriter);
        Http2ConnectionDecoder decoder = new DefaultHttp2ConnectionDecoder(connection, encoder, frameReader);

        Http2Settings settings = new Http2Settings();
        settings.initialWindowSize(1048576);
        settings.maxConcurrentStreams(Integer.MAX_VALUE);
        settings.maxHeaderListSize(8192);
        return new Http2ServerCodecHandler(decoder, encoder, settings, channel, http2Codec);
    }


    /**
     * 框架监听器
     */
    protected static class FrameListener extends Http2FrameAdapter {
        /**
         * 连接
         */
        protected final Http2Connection connection;
        /**
         * 头部Key
         */
        protected final PropertyKey headerKey;
        /**
         * 编解码
         */
        protected final Http2Codec codec;
        /**
         * 通道
         */
        protected final Channel channel;

        public FrameListener(Http2Connection connection, PropertyKey headerKey, Http2Codec codec, Channel channel) {
            this.connection = connection;
            this.headerKey = headerKey;
            this.codec = codec;
            this.channel = channel;
        }

        @Override
        public int onDataRead(final ChannelHandlerContext ctx, final int streamId, final ByteBuf data,
                              final int padding, final boolean endOfStream) throws Http2Exception {
            int processed = data.readableBytes() + padding;
            if (endOfStream) {
                Http2Stream http2Stream = connection.stream(streamId);
                Http2Headers headers = http2Stream.getProperty(headerKey);
                dispatch(ctx, streamId, headers, data);
            }
            return processed;
        }

        @Override
        public void onHeadersRead(final ChannelHandlerContext ctx, final int streamId, final Http2Headers headers,
                                  final int padding, final boolean endStream) throws Http2Exception {
            if (streamId > 0) {
                // 正常的请求（streamId==1 的是settings请求）
                if (endStream) {
                    // 没有DATA帧的请求，可能是DATA
                    dispatch(ctx, streamId, headers, null);
                } else {
                    // 缓存起来
                    Http2Stream stream = connection.stream(streamId);
                    if (stream != null) {
                        stream.setProperty(headerKey, headers);
                    }
                }
            }
        }

        @Override
        public void onHeadersRead(final ChannelHandlerContext ctx, final int streamId, final Http2Headers headers,
                                  final int streamDependency, final short weight, final boolean exclusive,
                                  final int padding, final boolean endStream) throws Http2Exception {
            onHeadersRead(ctx, streamId, headers, padding, endStream);
        }

        @Override
        public void onRstStreamRead(final ChannelHandlerContext ctx, final int streamId, final long errorCode) {
            logger.error("onRstStreamRead streamId:" + streamId + " errorCode:" + errorCode);
        }

        @Override
        public void onPingRead(final ChannelHandlerContext ctx, final long data) throws Http2Exception {
            logger.warn("onPingRead data:" + data);
        }

        @Override
        public void onPingAckRead(final ChannelHandlerContext ctx, final long data) throws Http2Exception {
            logger.warn("onPingAckRead data:" + data);
        }

        /**
         * 派发请求
         *
         * @param ctx      上下文
         * @param streamId 流ID
         * @param headers  头
         * @param body     数据
         * @throws Http2Exception
         */
        protected void dispatch(final ChannelHandlerContext ctx,
                                final int streamId,
                                final Http2Headers headers,
                                final ByteBuf body) throws Http2Exception {
            try {
                //获取请求body
                byte[] content = body != null ? (byte[]) codec.decode(new Http2DecodeContext(channel), new NettyChannelBuffer(body)) : null;
                //server端收到消息，没有bizId，这里用streamId充当bizId
                ctx.fireChannelRead(new DefaultHttp2RequestMessage(streamId, streamId, headers, content));
            } catch (Exception e) {
                throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, e, "has error when codec");
            }

        }

    }

}
