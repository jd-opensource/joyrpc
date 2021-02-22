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


import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 处理链
 */
public class ChannelChain {
    /**
     * 处理器
     */
    protected Deque<ChannelHandler> handlers = new ConcurrentLinkedDeque<>();

    protected volatile ChannelReader[] readers;

    protected volatile ChannelWriter[] writers;

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
    public synchronized ChannelChain addFirst(final ChannelHandler handler) {
        if (handler != null) {
            handlers.addFirst(handler);
            readers = null;
            writers = null;
        }
        return this;
    }

    /**
     * 添加到尾部
     *
     * @param handler 处理器
     * @return 处理链
     */
    public synchronized ChannelChain addLast(final ChannelHandler handler) {
        if (handler != null) {
            handlers.addLast(handler);
            readers = null;
            writers = null;
        }
        return this;
    }

    /**
     * 异常处理器
     *
     * @param handler 处理器
     * @return 处理链
     */
    public synchronized ChannelChain remove(final ChannelHandler handler) {
        if (handler != null) {
            if (handlers.remove(handler)) {
                readers = null;
                writers = null;
            }
        }
        return this;
    }

    public ChannelReader[] getReaders() {
        if (readers == null) {
            //有并发访问
            synchronized (this) {
                if (readers == null) {
                    LinkedList<ChannelReader> list = new LinkedList<>();
                    for (ChannelHandler handler : handlers) {
                        if (handler instanceof ChannelReader) {
                            list.add((ChannelReader) handler);
                        }
                    }
                    readers = list.toArray(new ChannelReader[0]);
                }
            }
        }
        return readers;
    }

    public ChannelWriter[] getWriters() {
        if (writers == null) {
            //有并发访问
            synchronized (this) {
                if (writers == null) {
                    LinkedList<ChannelWriter> list = new LinkedList<>();
                    for (ChannelHandler handler : handlers) {
                        if (handler instanceof ChannelWriter) {
                            list.add((ChannelWriter) handler);
                        }
                    }
                    writers = list.toArray(new ChannelWriter[0]);
                }
            }
        }
        return writers;
    }
}
