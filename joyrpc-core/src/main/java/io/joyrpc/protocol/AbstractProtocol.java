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
import io.joyrpc.protocol.handler.RequestChannelHandler;
import io.joyrpc.protocol.handler.ResponseChannelHandler;
import io.joyrpc.protocol.message.MessageHeader;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.channel.ChannelHandlerChain;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.message.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.joyrpc.Plugin.MESSAGE_HANDLER_SELECTOR;
import static io.joyrpc.protocol.MsgType.BizResp;
import static io.joyrpc.protocol.MsgType.CallbackResp;

/**
 * 抽象的协议
 *
 * @date: 2019/3/27
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
    protected ChannelHandlerChain chain;
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
     * @param context
     * @param cause
     */
    protected void onException(final ChannelContext context, final Throwable cause) {
        Channel channel = context.getChannel();
        if (!(cause instanceof LafException)
                && cause.getCause() != null
                && cause.getCause() instanceof LafException) {
            //非rpc异常，可能为transport框架统一捕获后包装的异常，getCause()后，重新处理
            onException(context, cause.getCause());
            return;
        }
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
     * @param channel
     * @param header
     * @param cause
     */
    protected void onException(final Channel channel, final MessageHeader header, final RpcException cause) {
        MsgType msgType = MsgType.valueOf(header.getMsgType());
        if (msgType != null) {
            if (channel.isServer()) {
                //服务端
                switch (msgType) {
                    case BizReq:
                        header.setMsgType(BizResp.getType());
                        sendException(channel, header, cause);
                        break;
                    case BizResp:
                        sendException(channel, header, cause);
                        break;
                }
            } else {
                //客户端
                switch (msgType) {
                    case CallbackReq:
                        header.setMsgType(CallbackResp.getType());
                        sendException(channel, header, cause);
                        break;
                    case CallbackResp:
                        sendException(channel, header, cause);
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
     * @param channel
     * @param header
     * @param cause
     */
    protected void sendException(final Channel channel, final MessageHeader header, final RpcException cause) {
        ResponseMessage<ResponsePayload> response = new ResponseMessage<>(header, new ResponsePayload(cause));
        channel.send(response, (event -> {
            if (!event.isSuccess()) {
                String address = Channel.toString(channel.getRemoteAddress()) + "->" + Channel.toString(channel.getLocalAddress());
                logger.error("Error occurs while sending " + MsgType.valueOf(header.getMsgType()) + " message to " + address, cause.getMessage());
            }
        }));
    }

    @Override
    public ChannelHandlerChain buildChain() {
        if (chain == null) {
            chain = new ChannelHandlerChain()
                    .addLast(new RequestChannelHandler<>(MESSAGE_HANDLER_SELECTOR, this::onException))
                    .addLast(new ResponseChannelHandler());
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
     * @return
     */
    protected MessageConverter createInConverter() {
        return null;
    }

    /**
     * 构建出队消息转换器
     *
     * @return
     */
    protected MessageConverter createOutConverter() {
        return null;
    }

}
