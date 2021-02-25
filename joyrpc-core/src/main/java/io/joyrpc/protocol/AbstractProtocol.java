package io.joyrpc.protocol;

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

import io.joyrpc.exception.LafException;
import io.joyrpc.exception.RpcException;
import io.joyrpc.protocol.handler.RequestReceiver;
import io.joyrpc.protocol.handler.ResponseReceiver;
import io.joyrpc.protocol.message.MessageHeader;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelChain;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.message.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.joyrpc.Plugin.MESSAGE_HANDLER_SELECTOR;
import static io.joyrpc.protocol.MsgType.BizResp;
import static io.joyrpc.protocol.MsgType.CallbackResp;

/**
 * 抽象的协议
 */
public abstract class AbstractProtocol implements Protocol {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractProtocol.class);

    public static final byte PROTOCOL_NUMBER = 12;

    public static final byte[] MAGIC_CODE = new byte[]{(byte) 0xAD, (byte) 0xD0};

    /**
     * 入队消息转换器
     */
    protected MessageConverter inConverter;
    /**
     * 出队消息转换器
     */
    protected MessageConverter outConverter;
    /**
     * 处理链
     */
    protected ChannelChain chain;
    /**
     * 编解码
     */
    protected Codec codec;

    /**
     * 构造函数
     */
    public AbstractProtocol() {
        this.inConverter = createInConverter();
        this.outConverter = createOutConverter();
        this.codec = createCodec();
    }

    /**
     * 创建编解码
     *
     * @return
     */
    protected abstract Codec createCodec();

    @Override
    public Codec getCodec() {
        return codec;
    }

    @Override
    public byte[] getMagicCode() {
        return MAGIC_CODE;
    }

    /**
     * 异常处理
     *
     * @param context   上下文
     * @param throwable 异常
     */
    protected void onException(final ChannelContext context, final Throwable throwable) {
        Channel channel = context.getChannel();
        //非rpc异常，可能为transport框架统一捕获后包装的异常，getCause()后，重新处理
        Throwable cause = !(throwable instanceof LafException) && throwable.getCause() instanceof LafException ? throwable.getCause() : throwable;
        Header header = cause instanceof RpcException ? ((RpcException) cause).getHeader() : null;
        if (header instanceof MessageHeader) {
            onException(channel, (MessageHeader) header, (RpcException) cause);
        } else {
            logger.error(String.format("Error %s occurs at %s ", cause.getClass().getName(), Channel.toString(channel)), cause);
        }
    }

    /**
     * 异常处理
     *
     * @param channel 连接通道
     * @param header  消息头
     * @param cause   异常
     */
    protected void onException(final Channel channel, final MessageHeader header, final RpcException cause) {
        MsgType msgType = MsgType.valueOf(header.getMsgType());
        if (msgType != null) {
            if (channel.isServer()) {
                //服务端
                switch (msgType) {
                    case BizReq:
                        ackException(channel, header.msgType(BizResp.getType()), cause);
                        break;
                    case BizResp:
                        ackException(channel, header, cause);
                        break;
                }
            } else {
                //客户端
                switch (msgType) {
                    case CallbackReq:
                        ackException(channel, header.msgType(CallbackResp.getType()), cause);
                        break;
                    case CallbackResp:
                        ackException(channel, header, cause);
                        break;
                }
            }
        }
        //发生异常，打印日志，方便本端问题排查
        logger.error(String.format("Error %s occurs at %s ", cause.getClass().getName(), Channel.toString(channel)), cause);
    }

    /**
     * 响应异常
     *
     * @param channel 连接通道
     * @param header  消息头
     * @param cause   异常
     */
    protected void ackException(final Channel channel, final MessageHeader header, final RpcException cause) {
        ackException(channel, header, cause, new ResponseMessage<>(header, new ResponsePayload(cause)));
    }

    /**
     * 发送应答
     *
     * @param channel  连接通道
     * @param header   消息头
     * @param cause    异常原因
     * @param response 应答
     */
    protected void ackException(final Channel channel, final MessageHeader header, final RpcException cause, final Object response) {
        channel.send(response).whenComplete((v, error) -> {
            if (error != null) {
                String address = Channel.toString(channel.getRemoteAddress()) + "->" + Channel.toString(channel.getLocalAddress());
                logger.error("Error occurs while sending " + MsgType.valueOf(header.getMsgType()) + " message to " + address, cause.getMessage());
            }
        });
    }

    @Override
    public ChannelChain buildChain() {
        if (chain == null) {
            chain = new ChannelChain()
                    .addLast(new RequestReceiver<>(MESSAGE_HANDLER_SELECTOR, this::onException))
                    .addLast(new ResponseReceiver());
        }
        return chain;
    }

    @Override
    public MessageConverter inMessage() {
        return inConverter;
    }

    @Override
    public MessageConverter outMessage() {
        return outConverter;
    }

    /**
     * 构建入队消息转换器
     *
     * @return 消息转换器
     */
    protected MessageConverter createInConverter() {
        return null;
    }

    /**
     * 构建出队消息转换器
     *
     * @return 消息转换器
     */
    protected MessageConverter createOutConverter() {
        return null;
    }

}
