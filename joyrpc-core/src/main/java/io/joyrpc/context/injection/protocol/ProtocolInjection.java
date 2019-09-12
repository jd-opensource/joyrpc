package io.joyrpc.context.injection.protocol;

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

import io.joyrpc.cluster.Node;
import io.joyrpc.context.injection.NodeReqInjection;
import io.joyrpc.context.injection.ReqInjection;
import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.Protocol;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.transport.Client;

/**
 * 协议注入
 */
@Extension("protocol")
public class ProtocolInjection implements NodeReqInjection {

    @Override
    public boolean test() {
        return true;
    }

    @Override
    public void inject(final RequestMessage<Invocation> request, final Node node, final Client client) {
        //兼容老的协议，需要透传一些额外的信息
        Protocol protocol = client.getProtocol();
        if (protocol instanceof ReqInjection) {
            ((ReqInjection) protocol).inject(request);
        }
    }
}
