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

import io.joyrpc.transport.buffer.ChannelBuffer;

/**
 * 协议推断
 */
public interface ProtocolDeduction {

    /**
     * 协议推断处理器名称
     */
    String PROTOCOL_DEDUCTION_HANDLER = "protocolDeduction";

    /**
     * 推断协议，不会改变连接通道缓冲区读取位置
     *
     * @param context 上下文
     * @param buffer  缓冲区
     */
    void deduce(DeductionContext context, ChannelBuffer buffer);
}
