package io.joyrpc.transaction.hmily;

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
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.extension.condition.ConditionalOnProperty;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.transaction.TransactionOption;
import org.dromara.hmily.common.enums.HmilyActionEnum;
import org.dromara.hmily.common.enums.HmilyRoleEnum;
import org.dromara.hmily.common.utils.IdWorkerUtils;
import org.dromara.hmily.core.context.HmilyContextHolder;
import org.dromara.hmily.core.context.HmilyTransactionContext;
import org.dromara.hmily.core.mediator.RpcMediator;
import org.dromara.hmily.repository.spi.entity.HmilyInvocation;
import org.dromara.hmily.repository.spi.entity.HmilyParticipant;

/**
 * hmily分布式事务集成
 */
@Extension("hmily")
@ConditionalOnProperty(value = "extension.hmily.enable", matchIfMissing = true)
@ConditionalOnClass("org.dromara.hmily.core.context.HmilyTransactionContext")
public class HmilyTransmit implements Transmit {

    @Override
    public void onReceive(final RequestMessage<Invocation> request) {
        TransactionOption transactionOption = request.getOption().getTransactionOption();
        if (!(transactionOption instanceof HmilyTransactionOption)) {
            return;
        }
        Invocation invocation = request.getPayLoad();
        HmilyTransactionContext transactionContext = RpcMediator.getInstance().acquire(invocation::getAttachment);
        if (transactionContext != null) {
            HmilyContextHolder.set(transactionContext);
        }
    }

    @Override
    public void inject(final RequestMessage<Invocation> request) {
        Invocation invocation = request.getPayLoad();
        TransactionOption transactionOption = request.getOption().getTransactionOption();
        if (!(transactionOption instanceof HmilyTransactionOption)) {
            return;
        }
        HmilyTransactionContext context = HmilyContextHolder.get();
        if (context == null) {
            return;
        }
        Long participantId = context.getParticipantId();
        final HmilyParticipant hmilyParticipant = buildParticipant(context, invocation);
        if (hmilyParticipant != null) {
            context.setParticipantId(hmilyParticipant.getParticipantId());
        }
        if (context.getRole() == HmilyRoleEnum.PARTICIPANT.getCode()) {
            context.setParticipantRefId(participantId);
        }
        RpcMediator.getInstance().transmit(invocation::addAttachment, context);
    }

    protected HmilyParticipant buildParticipant(final HmilyTransactionContext context,
                                                final Invocation invocation) {
        if (HmilyActionEnum.TRYING.getCode() != context.getAction()) {
            return null;
        }
        HmilyParticipant hmilyParticipant = new HmilyParticipant();
        hmilyParticipant.setTransId(context.getTransId());
        hmilyParticipant.setParticipantId(IdWorkerUtils.getInstance().createUUID());
        hmilyParticipant.setTransType(context.getTransType());
        HmilyInvocation hmilyInvocation = new HmilyInvocation(invocation.getClazz(), invocation.getMethodName(), invocation.getArgClasses(), invocation.getArgs());
        hmilyParticipant.setConfirmHmilyInvocation(hmilyInvocation);
        hmilyParticipant.setCancelHmilyInvocation(hmilyInvocation);
        return hmilyParticipant;
    }
}
