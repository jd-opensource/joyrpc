package io.joyrpc.protocol.dubbo;

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

import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.protocol.ClientProtocol;
import io.joyrpc.transport.Client;
import io.joyrpc.transport.message.Message;

/**
 * Dubbo客户端协议
 */
@Extension("dubbo")
@ConditionalOnClass("org.apache.dubbo.rpc.Protocol")
public class DubboClientProtocol extends DubboAbstractProtocol implements ClientProtocol {

    @Override
    public Message negotiate(URL clusterUrl, Client client) {
        return null;
    }

    @Override
    public Message sessionbeat(URL clusterUrl, Client client) {
        return null;
    }

    @Override
    public Message heartbeat(URL clusterUrl, Client client) {
        return null;
    }
}
