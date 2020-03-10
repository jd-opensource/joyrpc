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

import io.joyrpc.codec.CodecType;
import io.joyrpc.exception.HandlerException;
import io.joyrpc.extension.ExtensionPoint;
import io.joyrpc.protocol.message.negotiation.AbstractNegotiation;
import io.joyrpc.protocol.message.negotiation.NegotiationResponse;
import io.joyrpc.transport.MessageHandler;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.message.Message;
import io.joyrpc.util.Shutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static io.joyrpc.Plugin.*;

/**
 * @date: 2019/3/18
 */
public abstract class AbstractNegotiationHandler<T extends Message> implements MessageHandler<T> {

    protected final static Logger logger = LoggerFactory.getLogger(AbstractNegotiationHandler.class);

    @Override
    public void handle(final ChannelContext context, final T message) throws HandlerException {
        if (message.isRequest()) {
            NegotiationResponse negotiation;
            //协商
            Object payLoad = message.getPayLoad();
            AbstractNegotiation request = payLoad instanceof AbstractNegotiation ? (AbstractNegotiation) payLoad : null;
            if (request == null) {
                if (payLoad instanceof Throwable) {
                    negotiation = new NegotiationResponse(NegotiationResponse.NOT_SUPPORT, ((Throwable) payLoad).getMessage());
                } else {
                    negotiation = new NegotiationResponse(NegotiationResponse.NOT_SUPPORT, "Unkown Error.");
                }
            } else if (Shutdown.isShutdown()) {
                negotiation = new NegotiationResponse(NegotiationResponse.NOT_SUPPORT, "Server is shutdown.");
            } else {
                //先由服务端推荐默认设置，创建新的协商应答返回
                negotiation = negotiate(recommendRequest(request));
                //协商成功，创建并保存session
                if (negotiation.isSuccess()) {
                    //复制一份，可用于修改应答的压缩等协议
                    NegotiationResponse clone = negotiation.clone();
                    //重置为请求的扩展信息
                    clone.setAttributes(request.getAttributes());
                    //构建并保存session
                    session(context, message.getSessionId(), recommendResponse(clone));
                }
            }
            //响应
            Message response = createResponseMessage(message, negotiation);
            context.getChannel().send(response, (event) -> {
                if (!event.isSuccess()) {
                    logger.error("Negotiation response error, messge : {}", message.toString());
                }
            });
            //结束调用链
            context.end();
        }
    }

    /**
     * 构建应答消息
     *
     * @param request
     * @param negotiation
     * @return
     */
    protected abstract T createResponseMessage(final T request, final NegotiationResponse negotiation);

    /**
     * 进行协商
     *
     * @param negotiation
     * @return
     */
    protected NegotiationResponse negotiate(final AbstractNegotiation negotiation) {
        Result serialization = negotiate(SERIALIZATION, negotiation.getSerialization(), negotiation.getSerializations(), false);
        Result checksum = negotiate(CHECKSUM, negotiation.getChecksum(), negotiation.getChecksums(), true);
        Result compression = negotiate(COMPRESSION, negotiation.getCompression(), negotiation.getCompressions(), true);
        return serialization.isSuccess() && checksum.isSuccess() && compression.isSuccess() ?
                new NegotiationResponse(NegotiationResponse.SUCCESS,
                        serialization.getPrefer(), compression.getPrefer(), checksum.getPrefer(),
                        serialization.getCandidates(), compression.getCandidates(), checksum.getCandidates()) :
                new NegotiationResponse(NegotiationResponse.NOT_SUPPORT, "Negotiation Fail.");
    }

    /**
     * 推荐请求的默认协议
     *
     * @param negotiation
     * @return
     */
    protected AbstractNegotiation recommendRequest(final AbstractNegotiation negotiation) {
        return negotiation;
    }

    /**
     * 推荐应答的协议
     *
     * @param negotiation
     * @return
     */
    protected AbstractNegotiation recommendResponse(final AbstractNegotiation negotiation) {
        return negotiation;
    }

    /**
     * 构造并保存session
     *
     * @param context
     * @param sessionId
     * @param negotiation
     */
    protected void session(final ChannelContext context, final int sessionId, final AbstractNegotiation negotiation) {
    }

    /**
     * 协商编解码
     *
     * @param extension  插件
     * @param prefer     首选
     * @param candidates 候选
     * @param nullable   是否可以为空
     * @return
     */
    protected Result negotiate(final ExtensionPoint<? extends CodecType, String> extension, final String prefer,
                               final List<String> candidates, final boolean nullable) {
        Result result = new Result();
        String best = include(extension, prefer) && candidates.contains(prefer) ? prefer : null;
        if (candidates != null) {
            for (String v : candidates) {
                if (include(extension, v)) {
                    result.addCandidate(v);
                    //为空表示不使用
                    if (!nullable && best == null) {
                        best = v;
                    }
                }
            }
        }
        result.setPrefer(best);
        if (best != null && !best.isEmpty()) {
            result.setSuccess(true);
        } else if (nullable && (prefer == null || prefer.isEmpty())) {
            // 如果没有优选并且可以为空
            result.setSuccess(true);
        }
        return result;
    }

    /**
     * 是否包含该插件
     *
     * @param extension
     * @param name
     * @return
     */
    protected boolean include(final ExtensionPoint<? extends CodecType, String> extension, final String name) {
        return name == null || name.isEmpty() ? false : extension.get(name) != null;
    }

    /**
     * 协商结果
     */
    protected static class Result {
        /**
         * 首选
         */
        protected String prefer;
        /**
         * 候选者
         */
        protected List<String> candidates = new ArrayList<>(15);
        /**
         * 协商成功标识
         */
        protected boolean success;

        public String getPrefer() {
            return prefer;
        }

        public void setPrefer(String prefer) {
            this.prefer = prefer;
        }

        public List<String> getCandidates() {
            return candidates;
        }

        public void setCandidates(List<String> candidates) {
            this.candidates = candidates;
        }

        public void addCandidate(String name) {
            if (name != null) {
                candidates.add(name);
            }
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }
    }
}
