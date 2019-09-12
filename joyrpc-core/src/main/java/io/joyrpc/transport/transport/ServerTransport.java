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


import io.joyrpc.transport.Endpoint;
import io.joyrpc.transport.channel.ServerChannel;
import io.joyrpc.transport.codec.ProtocolAdapter;

import java.util.List;
import java.util.function.Consumer;

/**
 * 服务通道
 */
public interface ServerTransport extends Transport, Endpoint {

    /**
     * 获取连接此ServerTransport的所有ChannelTransport
     *
     * @return
     */
    List<ChannelTransport> getChannelTransports();

    /**
     * 遍历Transport
     *
     * @param consumer
     */
    default void forEach(final Consumer<ChannelTransport> consumer) {
        if (consumer != null) {
            List<ChannelTransport> transports = getChannelTransports();
            if (transports != null) {
                transports.forEach(consumer);
            }
        }
    }

    /**
     * 获取服务通道
     *
     * @return
     */
    ServerChannel getServerChannel();

    /**
     * 设置适配器
     *
     * @param adapter
     */
    void setAdapter(ProtocolAdapter adapter);

}
