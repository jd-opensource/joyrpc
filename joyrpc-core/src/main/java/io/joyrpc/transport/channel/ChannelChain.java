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
public class ChannelChain {

    /**
     * 处理器
     */
    protected LinkedList<ChannelHandler> handlers = new LinkedList<>();

    public ChannelChain() {
    }

    public ChannelChain(ChannelHandler... handlers) {
        if (handlers != null) {
            for (ChannelHandler handler : handlers) {
                addLast(handler);
            }
        }
    }

    /**
     * 添加到头部
     *
     * @param handler 处理器
     * @return 处理链
     */
    public ChannelChain addFirst(final ChannelHandler handler) {
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
    public ChannelChain addLast(final ChannelHandler handler) {
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
    public ChannelChain remove(final ChannelHandler handler) {
        if (handler != null) {
            handlers.remove(handler);
        }
        return this;
    }

    public List<ChannelHandler> getHandlers() {
        return handlers;
    }
}
