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


import io.joyrpc.extension.URL;
import io.joyrpc.transport.TransportClient;

/**
 * 非共享连接通道管理器
 */
public class UnsharedChannelManager extends AbstractChannelManager implements ChannelManager {

    public static final String TRANSPORT_ID = "transportId";

    public UnsharedChannelManager(URL url) {
        super(url);
    }

    @Override
    public String getName(final TransportClient transport) {
        return transport == null ? null : transport.getUrl().add(TRANSPORT_ID, transport.getTransportId()).toString(false, false, TRANSPORT_ID);
    }

}
