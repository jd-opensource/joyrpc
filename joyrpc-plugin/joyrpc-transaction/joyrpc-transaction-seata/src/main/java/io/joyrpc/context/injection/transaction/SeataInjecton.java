package io.joyrpc.context.injection.transaction;

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

import io.joyrpc.context.injection.Transmit;
import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.transport.session.Session;
import io.seata.core.context.RootContext;

/**
 * Seata分布式事务集成
 */
@Extension(value = "seata")
public class SeataInjecton implements Transmit {

    @Override
    public void restoreOnReceive(final RequestMessage<Invocation> request, final Session.RpcSession session) {
        String rpcXid = request.getPayLoad().getAttachment(RootContext.KEY_XID); // Acquire the XID from RPC invoke
        if (rpcXid != null) {
            // Provider：Bind the XID propagated by RPC to current runtime
            RootContext.bind(rpcXid);
        }
    }

    @Override
    public void inject(RequestMessage<Invocation> request) {
        // Get XID of current transaction
        String xid = RootContext.getXID();
        boolean bind = false;
        if (xid != null) {
            // Consumer：Put XID into the attachment of RPC
            request.getPayLoad().addAttachment(RootContext.KEY_XID, xid);
        }
    }
}
