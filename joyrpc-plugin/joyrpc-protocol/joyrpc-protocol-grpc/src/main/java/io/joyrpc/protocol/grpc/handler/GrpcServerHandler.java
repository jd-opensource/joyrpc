package io.joyrpc.protocol.grpc.handler;

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

import io.grpc.internal.GrpcUtil;
import io.joyrpc.codec.UnsafeByteArrayOutputStream;
import io.joyrpc.codec.compression.Compression;
import io.joyrpc.codec.serialization.Serialization;
import io.joyrpc.constants.Constants;
import io.joyrpc.exception.CodecException;
import io.joyrpc.exception.LafException;
import io.joyrpc.exception.RpcException;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.Protocol;
import io.joyrpc.protocol.grpc.HeaderMapping;
import io.joyrpc.protocol.grpc.Headers;
import io.joyrpc.protocol.grpc.message.GrpcResponseMessage;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.MessageHeader;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.channel.ChannelOperator;
import io.joyrpc.transport.http2.DefaultHttp2ResponseMessage;
import io.joyrpc.transport.http2.Http2Headers;
import io.joyrpc.transport.http2.Http2RequestMessage;
import io.joyrpc.transport.http2.Http2ResponseMessage;
import io.joyrpc.util.GrpcType;
import io.joyrpc.util.GrpcType.ClassWrapper;
import io.joyrpc.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

import static io.joyrpc.Plugin.COMPRESSION;
import static io.joyrpc.Plugin.SERIALIZATION_SELECTOR;
import static io.joyrpc.protocol.grpc.HeaderMapping.ACCEPT_ENCODING;
import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;

/**
 * grpc服务端处理器
 */
public class GrpcServerHandler implements ChannelOperator {

    private final static Logger logger = LoggerFactory.getLogger(GrpcServerHandler.class);
    protected static final Supplier<LafException> EXCEPTION_SUPPLIER = () -> new CodecException(":path interfaceClazz/methodName with alias header or interfaceClazz/alias/methodName");
    /**
     * 默认序列化
     */
    protected Serialization serialization = SERIALIZATION_SELECTOR.select((byte) Serialization.PROTOBUF_ID);

    @Override
    public void received(final ChannelContext ctx, final Object message) throws Exception {
        if (message instanceof Http2RequestMessage) {
            Http2RequestMessage request = (Http2RequestMessage) message;
            try {
                ctx.fireChannelRead(input(request, ctx.getChannel(), SystemClock.now()));
            } catch (Throwable e) {
                logger.error(String.format("Error occurs while parsing grpc request from %s", Channel.toString(ctx.getChannel().getRemoteAddress())), e);
                MessageHeader header = new MessageHeader();
                header.addAttribute(HeaderMapping.STREAM_ID.getNum(), request.getStreamId());
                header.setMsgId(request.getMsgId());
                header.setMsgType(MsgType.BizReq.getType());
                throw new RpcException(header, e);
            }
        } else {
            ctx.fireChannelRead(message);
        }
    }

    @Override
    public void wrote(final ChannelContext ctx, final Object message) throws Exception {
        if (message instanceof GrpcResponseMessage) {
            GrpcResponseMessage<?> response = (GrpcResponseMessage<?>) message;
            try {
                ctx.wrote(output(response));
            } catch (Exception e) {
                logger.error(String.format("Error occurs while wrote grpc response from %s", Channel.toString(ctx.getChannel().getRemoteAddress())), e);
                throw new RpcException(response.getHeader(), e);
            }
        } else {
            ctx.wrote(message);
        }
    }

    /**
     * 构造请求消息
     *
     * @param message     消息
     * @param channel     通道
     * @param receiveTime 接收时间
     * @return
     * @throws Exception
     */
    protected RequestMessage<Invocation> input(final Http2RequestMessage message, final Channel channel, final long receiveTime) throws Exception {
        if (message.getStreamId() <= 0) {
            return null;
        }
        Http2Headers httpHeaders = message.headers();
        Map<CharSequence, Object> headerMap = httpHeaders.getAll();
        Parametric parametric = new MapParametric(headerMap);
        // 解析uri
        String path = parametric.getString(Http2Headers.PseudoHeaderName.PATH.value(), "/");
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        URL url = URL.valueOf(path, "http");
        //消息头
        MessageHeader header = new MessageHeader(serialization.getTypeId(), MsgType.BizReq.getType(), (byte) Protocol.GRPC);
        header.setMsgId(message.getMsgId());
        header.setMsgType(MsgType.BizReq.getType());
        header.setTimeout(parametric.getTimeout(GrpcUtil.TIMEOUT, Constants.TIMEOUT_OPTION.getValue()));
        header.addAttribute(HeaderMapping.STREAM_ID.getNum(), message.getStreamId());
        header.addAttribute(ACCEPT_ENCODING.getNum(), parametric.getString(GrpcUtil.MESSAGE_ACCEPT_ENCODING));
        //构造invocation
        Invocation invocation = new GrpcDecoder()
                .url(url)
                .header(parametric)
                .messageId(header.getMsgId())
                .body(message.content())
                .serialization(serialization)
                .error(EXCEPTION_SUPPLIER)
                .build();
        RequestMessage<Invocation> reqMessage = RequestMessage.build(header, invocation, channel, parametric, receiveTime);
        reqMessage.setResponseSupplier(() -> {
            MessageHeader respHeader = header.response(MsgType.BizResp.getType(), Compression.NONE, header.getAttributes());
            return new GrpcResponseMessage<>(respHeader, invocation.getGrpcType());
        });
        return reqMessage;
    }

    /**
     * 构建应答消息
     *
     * @param message 消息
     * @return 应答消息
     */
    protected Http2ResponseMessage output(final GrpcResponseMessage<?> message) throws IOException {
        MessageHeader header = message.getHeader();
        int streamId = (Integer) header.getAttributes().get(HeaderMapping.STREAM_ID.getNum());
        ResponsePayload responsePayload = (ResponsePayload) message.getPayLoad();
        if (responsePayload.isError()) {
            return new DefaultHttp2ResponseMessage(streamId, header.getMsgId(),
                    null, null, Headers.build(responsePayload.getException()));
        }
        //http2 header
        Http2Headers headers = Headers.build(false);
        GrpcType grpcType = message.getGrpcType();
        Object respObj = wrapPayload(responsePayload, grpcType);
        //设置content
        UnsafeByteArrayOutputStream baos = new UnsafeByteArrayOutputStream();
        //是否压缩
        baos.write(0);
        //长度(占位)
        baos.write(new byte[]{0, 0, 0, 0}, 0, 4);
        //序列化
        serialization.getSerializer().serialize(baos, respObj);
        //获取 content 字节数组
        byte[] content = baos.toByteArray();
        //压缩处理
        Compression compression = null;
        if (content.length > 1024) {
            compression = COMPRESSION.get(split((String) header.getAttribute(ACCEPT_ENCODING.getNum()), SEMICOLON_COMMA_WHITESPACE));
        }
        if (compression != null) {
            //复用缓冲区
            baos.reset();
            baos.write(new byte[]{1, 0, 0, 0, 0});
            content = compression.compress(baos, content, 5, content.length - 5);
            headers.set(GrpcUtil.MESSAGE_ENCODING, compression.getTypeName());
        }
        //设置数据长度
        int length = content.length - 5;
        content[1] = (byte) (length >>> 24);
        content[2] = (byte) (length >>> 16);
        content[3] = (byte) (length >>> 8);
        content[4] = (byte) length;
        return new DefaultHttp2ResponseMessage(streamId, header.getMsgId(),
                headers, content, Headers.build(true));
    }

    /**
     * 包装载体，进行类型转换
     *
     * @param payload  载体
     * @param grpcType 类型
     * @return 包装载体
     */
    protected Object wrapPayload(final ResponsePayload payload, final GrpcType grpcType) {
        //获取 grpcType
        ClassWrapper wrapper = grpcType.getResponse();
        //设置反正值
        Object result;
        if (wrapper.isWrapper()) {
            //加快构建性能
            result = wrapper.getConversion().getToWrapper().apply(new Object[]{payload.getResponse()});
        } else {
            result = payload.getResponse();
        }
        return result;
    }
}
