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

import io.joyrpc.cluster.event.ReconnectEvent;
import io.joyrpc.exception.ChannelSendException;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.codec.Http2Codec;
import io.joyrpc.transport.http2.DefaultHttp2ResponseMessage;
import io.joyrpc.transport.http2.Http2RequestMessage;
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

import java.util.concurrent.atomic.AtomicBoolean;

import static io.netty.handler.codec.http2.DefaultHttp2LocalFlowController.DEFAULT_WINDOW_UPDATE_RATIO;

/**
 * http2 client端 编解码器
 */
public class Http2ClientCodecHandler extends Http2ConnectionHandler {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);
    /**
     * 解码器
     */
    protected final Http2ConnectionDecoder decoder;
    /**
     * 编码器
     */
    protected final Http2ConnectionEncoder encoder;
    /**
     * 连接
     */
    protected final Http2Connection connection;
    /**
     * 消息ID键
     */
    protected final PropertyKey msgIdKey;
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
    /**
     * 是否消耗完毕
     */
    protected final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * 构造函数
     *
     * @param decoder         HTTP2解码器
     * @param encoder         HTTP2编码器
     * @param initialSettings 初始化设置
     * @param channel         连接通道
     * @param codec           编解码
     */
    public Http2ClientCodecHandler(final Http2ConnectionDecoder decoder,
                                   final Http2ConnectionEncoder encoder,
                                   final Http2Settings initialSettings,
                                   final Channel channel,
                                   final Http2Codec codec) {
        super(decoder, encoder, initialSettings);
        this.decoder = decoder;
        this.encoder = encoder;
        this.channel = channel;
        this.codec = codec;
        this.connection = encoder.connection();
        this.msgIdKey = connection.newKey();
        this.headerKey = connection.newKey();
        // Set the frame listener on the decoder.
        decoder.frameListener(new FrameListener(connection, msgIdKey, headerKey, codec, channel));
        connection.addListener(new MyHttp2ConnectionAdapter(channel, this::reconnect));
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
        if (msg instanceof Http2RequestMessage) {
            Http2RequestMessage request = (Http2RequestMessage) msg;
            byte[] content = request.content();
            //构建http2响应header
            Http2Headers headers = request.headers() == null ? null : new Http2NettyHeaders(request.headers().getAll());
            if (content == null && headers == null) {
                return;
            }
            int streamId = request.getStreamId();
            if (streamId <= 0) {
                streamId = connection.local().incrementAndGetNextStreamId();
                if (streamId <= 0) {
                    exhausted();
                }
                request.setStreamId(streamId);
            }
            //保存消息ID
            Http2Stream stream = connection.stream(streamId);
            stream.setProperty(msgIdKey, request.getMsgId());
            if (content == null) {
                encoder.writeHeaders(ctx, streamId, headers, 0, request.isEnd(), promise);
            } else {
                if (headers != null) {
                    encoder.writeHeaders(ctx, streamId, headers, 0, false, promise);
                }
                encoder.writeData(ctx, streamId, Unpooled.wrappedBuffer(content), 0, request.isEnd(), ctx.voidPromise());
            }
        } else {
            super.write(ctx, msg, promise);
        }
    }

    /**
     * 流ID溢出
     */
    protected void exhausted() {
        reconnect();
        throw new ChannelSendException("Error occurs while sending message, caused by stream id<0");
    }

    /**
     * 重连
     */
    protected void reconnect() {
        if (closed.compareAndSet(false, true)) {
            //超出最大范围，需要断开连接重连，发送重连事件
            channel.getPublisher().offer(new ReconnectEvent(channel));
        }
    }

    /**
     * 构建HTTP2客户端编解码处理器
     *
     * @param channel    连接通道
     * @param http2Codec 编解码
     * @return HTTP2客户端编解码处理器
     */
    public static Http2ClientCodecHandler create(final Channel channel, final Http2Codec http2Codec) {
        Http2Connection connection = new DefaultHttp2Connection(false);
        WeightedFairQueueByteDistributor dist = new WeightedFairQueueByteDistributor(connection);
        dist.allocationQuantum(16 * 1024); // Make benchmarks fast again.
        connection.remote().flowController(new DefaultHttp2RemoteFlowController(connection, dist));
        connection.local().flowController(new DefaultHttp2LocalFlowController(connection, DEFAULT_WINDOW_UPDATE_RATIO, true));

        Http2HeadersDecoder headersDecoder = new DefaultHttp2HeadersDecoder(true);
        Http2FrameLogger frameLogger = new Http2FrameLogger(LogLevel.INFO, DefaultHttp2ConnectionDecoder.class);
        Http2FrameReader frameReader = new DefaultHttp2FrameReader(headersDecoder);
        Http2FrameWriter frameWriter = new DefaultHttp2FrameWriter();
        frameReader = new Http2InboundFrameLogger(frameReader, frameLogger);
        frameWriter = new Http2OutboundFrameLogger(frameWriter, frameLogger);

        StreamBufferingEncoder encoder = new StreamBufferingEncoder(new DefaultHttp2ConnectionEncoder(connection, frameWriter));
        Http2ConnectionDecoder decoder = new DefaultHttp2ConnectionDecoder(connection, encoder, frameReader);

        Http2Settings settings = new Http2Settings();
        settings.pushEnabled(false);
        settings.initialWindowSize(1048576);
        settings.maxConcurrentStreams(0);
        settings.maxHeaderListSize(8192);
        return new Http2ClientCodecHandler(decoder, encoder, settings, channel, http2Codec);
    }

    /**
     * 框架监听器
     */
    protected static class FrameListener extends Http2FrameAdapter {

        protected final Http2Connection connection;
        /**
         * 消息ID键
         */
        protected final PropertyKey msgIdKey;
        /**
         * 头部Key
         */
        protected final PropertyKey headerKey;
        /**
         * 编解码
         */
        protected Http2Codec codec;
        /**
         * 通道
         */
        protected Channel channel;

        public FrameListener(final Http2Connection connection, final PropertyKey msgIdKey, final PropertyKey headerKey,
                             final Http2Codec codec, final Channel channel) {
            this.connection = connection;
            this.msgIdKey = msgIdKey;
            this.headerKey = headerKey;
            this.codec = codec;
            this.channel = channel;
        }

        @Override
        public int onDataRead(final ChannelHandlerContext ctx,
                              final int streamId,
                              final ByteBuf data,
                              final int padding,
                              final boolean endOfStream) throws Http2Exception {
            try {
                Http2Stream stream = connection.stream(streamId);
                Long bizMsgId = stream.getProperty(msgIdKey);
                bizMsgId = bizMsgId == null ? 0 : bizMsgId;
                Http2Headers headers = stream.getProperty(headerKey);
                //获取server端响应body
                byte[] content = data == null ? null : (byte[]) codec.decode(new Http2DecodeContext(channel), new NettyChannelBuffer(data));
                ctx.fireChannelRead(new DefaultHttp2ResponseMessage(streamId, bizMsgId, headers, content));
            } catch (Exception e) {
                throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, e, "has error when codec");
            }
            return data.readableBytes() + padding;
        }

        @Override
        public void onHeadersRead(final ChannelHandlerContext ctx,
                                  final int streamId,
                                  final Http2Headers headers,
                                  final int padding,
                                  final boolean endStream) throws Http2Exception {
            if (streamId > 0) {
                onHeadersRead(ctx, streamId, headers, endStream);
            } else {
                super.onHeadersRead(ctx, streamId, headers, padding, endStream);
            }
        }

        @Override
        public void onHeadersRead(final ChannelHandlerContext ctx,
                                  final int streamId,
                                  final Http2Headers headers,
                                  final int streamDependency,
                                  final short weight,
                                  final boolean exclusive,
                                  final int padding,
                                  final boolean endStream) throws Http2Exception {
            if (streamId > 0) {
                onHeadersRead(ctx, streamId, headers, endStream);
            } else {
                super.onHeadersRead(ctx, streamId, headers, padding, endStream);
            }
        }

        /**
         * 读取到数据头
         *
         * @param ctx       上下文
         * @param streamId  流式ID
         * @param headers   头
         * @param endStream 流结束标识
         * @throws Http2Exception
         */
        protected void onHeadersRead(final ChannelHandlerContext ctx,
                                     final int streamId,
                                     final Http2Headers headers,
                                     final boolean endStream) throws Http2Exception {
            Http2Stream stream = connection.stream(streamId);
            if (!endStream) {
                // 缓存起来
                stream.setProperty(headerKey, headers);
            } else {
                //根据streamKey,获取缓存的bizId
                Long bizMsgId = stream.getProperty(msgIdKey);
                bizMsgId = bizMsgId == null ? 0 : bizMsgId;
                try {
                    ctx.fireChannelRead(new DefaultHttp2ResponseMessage(streamId, bizMsgId, headers, null, true));
                } catch (Exception e) {
                    throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, e, "has error when codec");
                }
            }
        }
    }

    /**
     * 连接监听器
     */
    protected static class MyHttp2ConnectionAdapter extends Http2ConnectionAdapter {
        /**
         * 连接通道
         */
        protected Channel channel;
        /**
         * 关闭程序
         */
        protected Runnable goaway;

        public MyHttp2ConnectionAdapter(Channel channel, Runnable goaway) {
            this.channel = channel;
            this.goaway = goaway;
        }

        @Override
        public void onGoAwayReceived(final int lastStreamId, final long errorCode, final ByteBuf debugData) {
            logger.info(String.format("Received GOAWAY message from %s,reconnect", Channel.toString(channel.getRemoteAddress())));
            goaway.run();
        }
    }
}
