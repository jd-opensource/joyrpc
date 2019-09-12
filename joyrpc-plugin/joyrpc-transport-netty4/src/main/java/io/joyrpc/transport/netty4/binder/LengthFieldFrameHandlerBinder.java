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
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.codec.LengthFieldFrameCodec;
import io.joyrpc.transport.codec.LengthFieldFrameCodec.LengthFieldFrame;
import io.joyrpc.transport.netty4.handler.LengthFieldFrameDecodeHandler;
import io.netty.channel.ChannelHandler;

import java.util.function.BiFunction;

/**
 * @date: 2019/3/26
 */
@Extension("lengthFieldFrame")
public class LengthFieldFrameHandlerBinder extends DefaultHandlerBinder {

    /**
     * 函数
     */
    public static final BiFunction<Codec, Channel, ChannelHandler> FUNCTION = (c, l) -> {
        LengthFieldFrameCodec frameCodec = (LengthFieldFrameCodec) c;
        LengthFieldFrame frame = frameCodec.getLengthFieldFrame();
        if (frame.getMaxFrameLength() <= 0) {
            frame.setMaxFrameLength(l.getAttribute(Channel.PAYLOAD));
        }
        return new LengthFieldFrameDecodeHandler(
                frame.getMaxFrameLength(),
                frame.getLengthFieldOffset(),
                frame.getLengthFieldLength(),
                frame.getLengthAdjustment(),
                frame.getInitialBytesToStrip(),
                frameCodec, l);
    };

    @Override
    public HandlerMeta<Codec>[] decoders() {
        return new CodecMeta[]{new CodecMeta(DECODER, FUNCTION)};
    }
}
