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

import io.joyrpc.transaction.TransactionContext;

/**
 * seata事务上下文
 */
public class SeataTransactionContext implements TransactionContext {

    /**
     * seata的分布式事务id
     */
    protected String xid;

    public SeataTransactionContext(String xid) {
        this.xid = xid;
    }

    public String getXid() {
        return xid;
    }

}
