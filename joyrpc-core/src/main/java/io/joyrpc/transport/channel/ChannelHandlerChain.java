package io.joyrpc.transport.channel;

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


import java.util.LinkedList;
import java.util.List;

/**
 * 处理链
 */
public class ChannelHandlerChain {

    /**
     * 处理链
     */
    protected LinkedList<ChannelHandler> handlers = new LinkedList<>();

    public ChannelHandlerChain() {
    }

    public ChannelHandlerChain(ChannelHandler... handlers) {
        if (handlers != null) {
            for (ChannelHandler handler : handlers) {
                this.handlers.addLast(handler);
            }
        }
    }

    /**
     * 添加到头部
     *
     * @param handler 处理器
     * @return 处理链
     */
    public ChannelHandlerChain addFirst(final ChannelHandler handler) {
        if (handler != null) {
            handlers.addFirst(handler);
        }
        return this;
    }

    /**
     * 添加到尾部
     *
     * @param handler 处理器
     * @return 处理链
     */
    public ChannelHandlerChain addLast(final ChannelHandler handler) {
        if (handler != null) {
            handlers.addLast(handler);
        }
        return this;
    }

    /**
     * 异常处理器
     *
     * @param handler 处理器
     * @return 处理链
     */
    public ChannelHandlerChain remove(final ChannelHandler handler) {
        if (handler != null) {
            handlers.remove(handler);
        }
        return this;
    }

    /**
     * 获取处理链
     *
     * @return 处理器列表
     */
    public List<ChannelHandler> getHandlers() {
        return handlers;
    }
}
