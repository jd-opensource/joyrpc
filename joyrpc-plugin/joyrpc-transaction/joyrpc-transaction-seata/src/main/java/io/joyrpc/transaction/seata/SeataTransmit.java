package io.joyrpc.transaction.seata;

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

import io.joyrpc.invoker.injection.Transmit;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.extension.condition.ConditionalOnProperty;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.transaction.TransactionContext;
import io.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Seata分布式事务集成
 */
@Extension(value = "seata")
@ConditionalOnProperty(value = "extension.seata.enable", matchIfMissing = true)
@ConditionalOnClass("io.seata.core.context.RootContext")
public class SeataTransmit implements Transmit {

    private final static Logger logger = LoggerFactory.getLogger(SeataTransmit.class);

    @Override
    public void onServerReceive(final RequestMessage<Invocation> request) {
        String rpcXid = request.getPayLoad().getAttachment(RootContext.KEY_XID); // Acquire the XID from RPC invoke
        if (rpcXid != null) {
            // Provider：Bind the XID propagated by RPC to current runtime
            request.setTransactionContext(new SeataTransactionContext(rpcXid));
            RootContext.bind(rpcXid);
        }
    }

    @Override
    public void onServerReturn(final RequestMessage<Invocation> request) {
        // Provider：Clean up XID after invoke
        TransactionContext context = request.getTransactionContext();
        if (context instanceof SeataTransactionContext) {
            String xid = ((SeataTransactionContext) context).getXid();
            String unbindXid = RootContext.unbind();
            if (!xid.equalsIgnoreCase(unbindXid)) {
                logger.warn("xid in change during RPC from " + xid + " to " + unbindXid);
                if (unbindXid != null) { // if there is new transaction begin, can't do clean up
                    RootContext.bind(unbindXid);
                    logger.warn("bind [" + unbindXid + "] back to RootContext");
                }
            }
        }
    }

    @Override
    public void inject(RequestMessage<Invocation> request) {
        // Get XID of current transaction
        String xid = RootContext.getXID();
        if (xid != null) {
            // Consumer：Put XID into the attachment of RPC
            request.getPayLoad().addAttachment(RootContext.KEY_XID, xid);
        }
    }
}
