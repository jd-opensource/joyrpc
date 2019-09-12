package io.joyrpc.transport.transport;

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


import io.joyrpc.event.EventHandler;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.event.TransportEvent;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 传输通道
 */
public interface Transport {

    /**
     * 获取本地地址
     *
     * @return
     */
    InetSocketAddress getLocalAddress();

    /**
     * 获取URL
     *
     * @return
     */
    URL getUrl();

    /**
     * 添加一个eventHandler
     *
     * @param handler
     */
    default void addEventHandler(EventHandler<? extends TransportEvent> handler) {

    }

    /**
     * 添加一组eventHandler
     *
     * @param handlers
     */
    default void addEventHandler(EventHandler<? extends TransportEvent>... handlers) {
        if (handlers != null) {
            for (EventHandler<? extends TransportEvent> handler : handlers) {
                addEventHandler(handler);
            }
        }
    }

    /**
     * 移除一个eventHandler
     *
     * @param handler
     */
    default void removeEventHandler(EventHandler<? extends TransportEvent> handler) {

    }

    default int getTransportId() {
        return 0;
    }

    Supplier<Integer> ID_GENERATOR = new Supplier<Integer>() {

        protected AtomicInteger atomicInteger = new AtomicInteger(0);

        @Override
        public Integer get() {
            return atomicInteger.incrementAndGet();
        }
    };

}
