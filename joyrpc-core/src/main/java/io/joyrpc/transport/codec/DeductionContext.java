package io.joyrpc.transport.codec;

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

import io.joyrpc.transport.channel.ChannelHandlerChain;

/**
 * 协议推断上下文
 */
public interface DeductionContext extends CodecContext {

    /**
     * 绑定编解码和处理链
     *
     * @param codec 编解码
     * @param chain 处理链
     */
    void bind(final Codec codec, final ChannelHandlerChain chain);
}
