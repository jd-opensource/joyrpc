package io.joyrpc.protocol.handler;

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

import io.joyrpc.Invoker;
import io.joyrpc.Result;
import io.joyrpc.context.RequestContext;
import io.joyrpc.exception.HandlerException;
import io.joyrpc.exception.RpcException;
import io.joyrpc.invoker.InvokerManager;
import io.joyrpc.protocol.MessageHandler;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.message.*;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.message.Header;
import io.joyrpc.util.Futures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.constants.Constants.HEAD_CALLBACK_INSID;

/**
 * 回调消息处理器
 */
public class CallbackReqHandler implements MessageHandler {

    private final static Logger logger = LoggerFactory.getLogger(CallbackReqHandler.class);

    @Override
    public void handle(final ChannelContext context, final Message message) throws HandlerException {
        // handle the callback Request
        RequestMessage<Invocation> request = (RequestMessage<Invocation>) message;
        String callbackInsId = (String) request.getHeader().getAttribute(HEAD_CALLBACK_INSID);
        Invoker invoker = InvokerManager.getConsumerCallback().getInvoker(callbackInsId);
        Channel channel = context.getChannel();
        Header header = message.getHeader();
        InvokerManager.getCallbackThreadPool().execute(() -> {
            try {
                //TODO 参数恢复
                CompletableFuture<Result> future = invoker != null ? invoker.invoke(request) :
                        Futures.completeExceptionally(new RpcException("Can't find callback invoker, callback id:" + callbackInsId));
                future.whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("Error occurs while invoking callback in channel " + Channel.toString(channel)
                                + ", error message is :" + throwable.getMessage(), throwable);
                        sendResponse(channel, header, new ResponsePayload(new RpcException(header, throwable)));
                    }
                    boolean isAsync = Optional.ofNullable(result.getContext()).orElse(RequestContext.getContext()).isAsync();
                    if (isAsync) {
                        ((CompletableFuture<Object>) result.getValue()).whenComplete((obj, th) -> {
                            sendResponse(channel, header, new ResponsePayload(obj, th));
                        });
                    } else {
                        sendResponse(channel, header, new ResponsePayload(result.getValue(), result.getException()));
                    }
                });

            } catch (Exception e) {
                logger.error("Error occurs while invoking callback in channel " + Channel.toString(channel)
                        + ", error message is :" + e.getMessage(), e);
                sendResponse(channel, header, new ResponsePayload(new RpcException(header, e)));
            }
        });
    }

    /**
     * 发送应答消息
     *
     * @param channel 通道
     * @param header  请求 头部
     * @param payload 包体
     */
    protected void sendResponse(final Channel channel, final Header header, final ResponsePayload payload) {
        ResponseMessage<ResponsePayload> response = new ResponseMessage<>((MessageHeader) header.clone(), MsgType.CallbackResp.getType());
        //write the callback response to the serverside
        response.setPayLoad(payload);

        channel.send(response, (result) -> {
            if (!result.isSuccess()) {
                Throwable throwable = result.getThrowable();
                logger.error(String.format("Error occurs while sending callback ResponsePayload %d", response.getHeader().getMsgId()), throwable);
            }
        });
    }

    @Override
    public Integer type() {
        return (int) MsgType.CallbackReq.getType();
    }
}
