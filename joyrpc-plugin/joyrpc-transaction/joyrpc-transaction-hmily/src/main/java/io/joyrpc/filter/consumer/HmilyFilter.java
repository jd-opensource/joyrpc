package io.joyrpc.filter.consumer;

import io.joyrpc.Invoker;
import io.joyrpc.Result;
import io.joyrpc.config.InterfaceOption;
import io.joyrpc.filter.ConsumerFilter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import org.dromara.hmily.annotation.Hmily;
import org.dromara.hmily.common.enums.HmilyActionEnum;
import org.dromara.hmily.common.enums.HmilyRoleEnum;
import org.dromara.hmily.common.exception.HmilyRuntimeException;
import org.dromara.hmily.common.utils.IdWorkerUtils;
import org.dromara.hmily.core.context.HmilyContextHolder;
import org.dromara.hmily.core.context.HmilyTransactionContext;
import org.dromara.hmily.core.holder.HmilyTransactionHolder;
import org.dromara.hmily.core.mediator.RpcMediator;
import org.dromara.hmily.repository.spi.entity.HmilyInvocation;
import org.dromara.hmily.repository.spi.entity.HmilyParticipant;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public class HmilyFilter  implements ConsumerFilter {

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        final HmilyTransactionContext context = HmilyContextHolder.get();
        if (context == null) {
            return invoker.invoke(request);
        }
        Invocation invocation = request.getPayLoad();
        Method method = invocation.getMethod();
        Hmily hmily = method.getAnnotation(Hmily.class);
        if (hmily == null) {
            return invoker.invoke(request);
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
        final Result result = invoker.invoke(invocation);
        //if result has not exception
        if (!result.hasException()) {
            if (context.getRole() == HmilyRoleEnum.PARTICIPANT.getCode()) {
                HmilyTransactionHolder.getInstance().registerParticipantByNested(participantId, hmilyParticipant);
            } else {
                HmilyTransactionHolder.getInstance().registerStarterParticipant(hmilyParticipant);
            }
        } else {
            throw new HmilyRuntimeException("rpc invoke exception{}", result.getException());
        }
        return result;
    }

    protected HmilyParticipant buildParticipant(final HmilyTransactionContext context,
                                                final Invocation invocation) throws HmilyRuntimeException {
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

    @Override
    public boolean test(final InterfaceOption option) {
      return true;
    }
}
