package io.joyrpc.transport.netty4.binder;

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

import io.joyrpc.extension.Extension;
import io.joyrpc.transport.channel.ChainChannelHandler;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelHandlerChain;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.codec.Http2Codec;
import io.joyrpc.transport.netty4.http2.Http2ClientCodecHandler;
import io.joyrpc.transport.netty4.http2.Http2ServerCodecHandler;
import io.joyrpc.transport.netty4.http2.SimpleHttp2BizHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.*;
import io.netty.handler.logging.LogLevel;

import java.util.function.BiFunction;

import static io.netty.handler.codec.http2.DefaultHttp2LocalFlowController.DEFAULT_WINDOW_UPDATE_RATIO;

/**
 * @date: 2019/4/10
 */
@Extension("http2")
public class Http2HandlerBinder implements HandlerBinder {

    /**
     * 函数
     */
    public static final BiFunction<ChannelHandlerChain, Channel, ChannelHandler> FUNCTION = (c, l) ->
            new SimpleHttp2BizHandler(new ChainChannelHandler(c, l.getAttribute(Channel.BIZ_THREAD_POOL)), l);

    @Override
    public HandlerMeta<ChannelHandlerChain>[] handlers() {
        return new HandlerChainMeta[]{new HandlerChainMeta(HANDLER, FUNCTION)};
    }

    @Override
    public HandlerMeta<Codec>[] decoders() {
        return new CodecMeta[0];
    }

    @Override
    public HandlerMeta<Codec>[] encoders() {
        return new CodecMeta[0];
    }

    @Override
    public void bind(ChannelPipeline pipeline, Codec codec, ChannelHandlerChain chain, Channel channel) {
        if (codec != null && codec instanceof Http2Codec) {
            Http2Codec http2Codec = (Http2Codec) codec;
            if (channel.isServer()) {
                Http2FrameLogger frameLogger = new Http2FrameLogger(LogLevel.DEBUG, Http2ServerCodecHandler.class);
                int payload = channel.getAttribute(Channel.PAYLOAD);

                Http2HeadersDecoder headersDecoder = new DefaultHttp2HeadersDecoder(true, payload);
                Http2FrameReader frameReader = new Http2InboundFrameLogger(new DefaultHttp2FrameReader(headersDecoder), frameLogger);
                Http2FrameWriter frameWriter = new Http2OutboundFrameLogger(new DefaultHttp2FrameWriter(), frameLogger);

                final Http2Connection connection = new DefaultHttp2Connection(true);

                Http2ConnectionEncoder encoder = new DefaultHttp2ConnectionEncoder(connection, frameWriter);
                Http2ConnectionDecoder decoder = new DefaultHttp2ConnectionDecoder(connection, encoder, frameReader);

                Http2Settings settings = new Http2Settings();
                settings.initialWindowSize(1048576);
                settings.maxConcurrentStreams(Integer.MAX_VALUE);
                settings.maxHeaderListSize(8192);
                Http2ServerCodecHandler serverCodec = new Http2ServerCodecHandler(decoder, encoder, settings, channel, http2Codec);
                pipeline.addLast(CODEC, serverCodec);
            } else {
                Http2FrameLogger frameLogger = new Http2FrameLogger(LogLevel.DEBUG, DefaultHttp2ConnectionDecoder.class);

                Http2HeadersDecoder headersDecoder = new DefaultHttp2HeadersDecoder(true);
                Http2FrameReader frameReader = new DefaultHttp2FrameReader(headersDecoder);
                Http2FrameWriter frameWriter = new DefaultHttp2FrameWriter();

                Http2Connection connection = new DefaultHttp2Connection(false);

                WeightedFairQueueByteDistributor dist = new WeightedFairQueueByteDistributor(connection);
                dist.allocationQuantum(16 * 1024); // Make benchmarks fast again.
                DefaultHttp2RemoteFlowController controller = new DefaultHttp2RemoteFlowController(connection, dist);
                connection.remote().flowController(controller);

                frameReader = new Http2InboundFrameLogger(frameReader, frameLogger);
                frameWriter = new Http2OutboundFrameLogger(frameWriter, frameLogger);

                StreamBufferingEncoder encoder = new StreamBufferingEncoder(new DefaultHttp2ConnectionEncoder(connection, frameWriter));

                connection.local().flowController(
                        new DefaultHttp2LocalFlowController(connection, DEFAULT_WINDOW_UPDATE_RATIO, true));

                Http2ConnectionDecoder decoder = new DefaultHttp2ConnectionDecoder(connection, encoder, frameReader);

                Http2Settings settings = new Http2Settings();
                settings.pushEnabled(false);
                settings.initialWindowSize(1048576);
                settings.maxConcurrentStreams(0);
                settings.maxHeaderListSize(8192);
                Http2ClientCodecHandler clientCodec = new Http2ClientCodecHandler(decoder, encoder, settings, channel, http2Codec);
                pipeline.addLast(CODEC, clientCodec);
            }
        }
        if (chain != null) {
            for (HandlerMeta<ChannelHandlerChain> meta : handlers()) {
                pipeline.addLast(meta.name, meta.function.apply(chain, channel));
            }
        }
    }
}
