package io.joyrpc.protocol.telnet;

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

import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.protocol.ServerProtocol;
import io.joyrpc.transport.buffer.ChannelBuffer;
import io.joyrpc.transport.channel.ChannelHandlerChain;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.codec.TelnetCodec;

import static io.joyrpc.constants.Constants.TELNET_PROMPT_OPTION;
import static io.joyrpc.protocol.Protocol.TELNET_ORDER;

/**
 * @date: 2019/1/22
 */
@Extension(value = "telnet", order = TELNET_ORDER)
public class TelnetServerProtocol implements ServerProtocol {

    /**
     * 构造函数
     */
    public TelnetServerProtocol() {
        Parametric parametric = new MapParametric(GlobalContext.getContext());
        this.codec = new TelnetCodec(parametric.getString(TELNET_PROMPT_OPTION));
    }

    /**
     * 逻辑处理链
     */
    protected ChannelHandlerChain chain = new ChannelHandlerChain().addFirst(new TelnetChannelHandler());
    /**
     * Telnet处理器
     */
    protected Codec codec;

    @Override
    public boolean match(final ChannelBuffer channelBuffer) {
        return true;
    }

    @Override
    public ChannelHandlerChain buildChain() {
        return chain;
    }

    @Override
    public Codec getCodec() {
        return codec;
    }

    @Override
    public byte[] getMagicCode() {
        return new byte[0];
    }
}
