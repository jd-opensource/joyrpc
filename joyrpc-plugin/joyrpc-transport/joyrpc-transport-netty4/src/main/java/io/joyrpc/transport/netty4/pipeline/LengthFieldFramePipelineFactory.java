package io.joyrpc.transport.netty4.pipeline;

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
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.codec.LengthFieldFrameCodec;
import io.joyrpc.transport.codec.LengthFieldFrameCodec.LengthFieldFrame;
import io.joyrpc.transport.netty4.handler.LengthFieldMessageDecoder;
import io.joyrpc.transport.netty4.handler.MessageEncoder;
import io.netty.channel.ChannelPipeline;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * 基于长度字段的管道工厂
 */
@Extension("lengthFieldFrame")
public class LengthFieldFramePipelineFactory extends DefaultPipelineFactory {

    @Override
    public void build(final ChannelPipeline pipeline, final Codec codec, final Channel channel, final EventExecutorGroup group) {
        LengthFieldFrameCodec frameCodec = (LengthFieldFrameCodec) codec;
        LengthFieldFrame frame = frameCodec.getLengthFieldFrame();
        if (frame.getMaxFrameLength() <= 0) {
            frame.setMaxFrameLength(channel.getAttribute(Channel.PAYLOAD));
        }
        pipeline.addLast(DECODER, new LengthFieldMessageDecoder(
                frame.getMaxFrameLength(), frame.getLengthFieldOffset(),
                frame.getLengthFieldLength(), frame.getLengthAdjustment(),
                frame.getInitialBytesToStrip(), frameCodec, channel));
        pipeline.addLast(ENCODER, new MessageEncoder(codec, channel));
    }

}
