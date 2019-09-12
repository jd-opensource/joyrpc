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
 * @date: 2019/1/10
 */
public class ChannelHandlerChain {

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

    public ChannelHandlerChain addFirst(ChannelHandler handler) {
        if (handler != null) {
            handlers.addFirst(handler);
        }
        return this;
    }

    public ChannelHandlerChain addLast(ChannelHandler handler) {
        if (handler != null) {
            handlers.addLast(handler);
        }
        return this;
    }

    public ChannelHandlerChain remove(ChannelHandler handler) {
        if (handler != null) {
            handlers.remove(handler);
        }
        return this;
    }

    public List<ChannelHandler> getHandlers() {
        return handlers;
    }
}
