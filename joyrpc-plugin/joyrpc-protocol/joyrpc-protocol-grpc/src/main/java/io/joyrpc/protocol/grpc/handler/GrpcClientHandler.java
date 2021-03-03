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

import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.internal.GrpcUtil;
import io.joyrpc.codec.UnsafeByteArrayInputStream;
import io.joyrpc.codec.UnsafeByteArrayOutputStream;
import io.joyrpc.codec.compression.Compression;
import io.joyrpc.codec.serialization.Serialization;
import io.joyrpc.config.InterfaceOption;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.exception.RpcException;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.Protocol;
import io.joyrpc.protocol.grpc.HeaderMapping;
import io.joyrpc.protocol.grpc.exception.GrpcBizException;
import io.joyrpc.protocol.message.*;
import io.joyrpc.transport.channel.*;
import io.joyrpc.transport.http.HttpMethod;
import io.joyrpc.transport.http2.DefaultHttp2Headers;
import io.joyrpc.transport.http2.DefaultHttp2RequestMessage;
import io.joyrpc.transport.http2.Http2Headers;
import io.joyrpc.transport.http2.Http2ResponseMessage;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.session.Session;
import io.joyrpc.util.IDLMethodDesc;
import io.joyrpc.util.IDLType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.joyrpc.Plugin.*;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.transport.http.HttpHeaders.Values.GZIP;
import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;

/**
 * grpc client端消息转换handler
 */
public class GrpcClientHandler implements ChannelOperator {

    private final static Logger logger = LoggerFactory.getLogger(GrpcClientHandler.class);

    protected final Serialization serialization = SERIALIZATION_SELECTOR.select((byte) Serialization.PROTOBUF_ID);

    protected final Map<Integer, Http2ResponseMessage> http2ResponseNoEnds = new ConcurrentHashMap<>();

    @Override
    public void received(final ChannelContext ctx, final Object message) throws Exception {
        if (message instanceof Http2ResponseMessage) {
            Http2ResponseMessage response = (Http2ResponseMessage) message;
            try {
                ctx.fireChannelRead(input(ctx.getChannel(), response));
            } catch (Throwable e) {
                logger.error(String.format("Error occurs while parsing grpc request from %s", Channel.toString(ctx.getChannel().getRemoteAddress())), e);
                MessageHeader header = new MessageHeader((byte) Serialization.PROTOBUF_ID, MsgType.BizReq.getType(), (byte) Protocol.GRPC);
                header.setMsgId(response.getMsgId());
                header.addAttribute(HeaderMapping.STREAM_ID.getNum(), response.getStreamId());
                throw new RpcException(header, e);
            }
        } else {
            ctx.fireChannelRead(message);
        }
    }

    @Override
    public void wrote(final ChannelContext ctx, final Object message) throws Exception {
        if (message instanceof RequestMessage) {
            RequestMessage<?> request = (RequestMessage<?>) message;
            try {
                ctx.wrote(output(ctx.getChannel(), request));
            } catch (Exception e) {
                logger.error(String.format("Error occurs while write grpc request from %s", Channel.toString(ctx.getChannel().getRemoteAddress())), e);
                throw new RpcException(request.getHeader(), e);
            }
        } else {
            ctx.wrote(message);
        }
    }

    /**
     * 转换grpc应答
     *
     * @param channel 通道
     * @param message 消息
     * @return 应答消息
     */
    protected Object input(final Channel channel, final Http2ResponseMessage message) throws IOException {
        if (message.getStreamId() <= 0) {
            return null;
        }
        Http2ResponseMessage http2Msg = adjoin(message);
        if (http2Msg == null) {
            return null;
        }
        MessageHeader header = new MessageHeader(serialization.getTypeId(), MsgType.BizResp.getType(), (byte) Protocol.GRPC);
        header.setMsgId(http2Msg.getMsgId());
        header.addAttribute(HeaderMapping.STREAM_ID.getNum(), http2Msg.getStreamId());
        ResponsePayload payload;
        Object grpcStatusVal = http2Msg.headers().get(GRPC_STATUS_KEY);
        int grpcStatus = grpcStatusVal == null ? Code.UNKNOWN.value() : Integer.parseInt(grpcStatusVal.toString());
        if (grpcStatus == Code.OK.value()) {
            RequestFuture<Long, Message> future = channel.getFutureManager().get(http2Msg.getMsgId());
            if (future != null) {
                payload = decodePayload(http2Msg, (IDLType) future.getAttr());
            } else {
                payload = new ResponsePayload(new GrpcBizException(String.format("request is timeout. id=%d", http2Msg.getMsgId())));
            }
        } else {
            Status status = Status.fromCodeValue(grpcStatus);
            String errMsg = String.format("%s [%d]: %s", status.getCode().name(), grpcStatus, http2Msg.headers().get(GRPC_MESSAGE_KEY));
            payload = new ResponsePayload(new GrpcBizException(errMsg));
        }
        return new ResponseMessage<>(header, payload);
    }

    /**
     * 解析应答
     *
     * @param message 消息
     * @param wrapper 返回类型
     * @return 应答
     * @throws IOException
     */
    protected ResponsePayload decodePayload(final Http2ResponseMessage message, final IDLType wrapper) throws IOException {
        Http2Headers headers = message.headers();
        InputStream in = new UnsafeByteArrayInputStream(message.content());
        //读压缩位标识
        int isCompression = in.read();
        //读长度共4位
        if (in.skip(4) < 4) {
            throw new IOException(String.format("request data is not full. id=%d", message.getMsgId()));
        }
        //解压处理
        if (isCompression > 0) {
            Compression compression = COMPRESSION.get(split((String) headers.get(GrpcUtil.MESSAGE_ACCEPT_ENCODING), SEMICOLON_COMMA_WHITESPACE));
            if (compression != null) {
                in = compression.decompress(in);
            }
        }
        //反序列化
        Object response = serialization.getSerializer().deserialize(in, wrapper.getClazz());
        if (wrapper.isWrapper()) {
            //性能优化
            Object[] parameters = wrapper.getConversion().getToParameter().apply(response);
            response = parameters[0];
        }
        return new ResponsePayload(response);
    }

    /**
     * 判断是否是结束消息
     *
     * @param message 消息
     * @return 原始消息
     */
    protected Http2ResponseMessage adjoin(final Http2ResponseMessage message) {
        Http2ResponseMessage http2Msg = null;
        if (!message.isEnd()) {
            http2ResponseNoEnds.put(message.getStreamId(), message);
            return null;
        } else {
            http2Msg = http2ResponseNoEnds.remove(message.getStreamId());
        }
        return http2Msg;
    }

    /**
     * 转换
     *
     * @param channel
     * @param message
     * @return
     */
    protected Object output(final Channel channel, final RequestMessage<?> message) throws IOException {
        if (!(message.getPayLoad() instanceof Invocation)) {
            return message;
        }
        Invocation invocation = (Invocation) message.getPayLoad();
        Session session = message.getSession();
        Http2Headers headers = buildHeaders(invocation, session, channel);
        //做grpc入参与返回值的类型转换，获取GrpcType
        InterfaceOption.MethodOption option = message.getOption();
        IDLMethodDesc methodDesc = option.getArgType().getIDLMethodDesc();
        IDLType type = methodDesc.getResponse();
        //包装payload
        Object payLoad = wrapPayload(invocation, methodDesc);
        //将返回值类型放到 future 中
        FutureManager<Long, Message> futureManager = channel.getFutureManager();
        RequestFuture<Long, Message> future = futureManager.get(message.getMsgId());
        future.setAttr(type != null ? type : new IDLType(invocation.getMethod().getReturnType(), false));

        byte compressType = message.getHeader().getCompression();
        //设置content
        UnsafeByteArrayOutputStream baos = new UnsafeByteArrayOutputStream();
        //是否压缩
        baos.write(0);
        //长度(占位)
        baos.write(new byte[]{0, 0, 0, 0}, 0, 4);
        //序列化
        if (payLoad != null) {
            serialization.getSerializer().serialize(baos, payLoad);
        }
        //获取 content 字节数组
        byte[] content = baos.toByteArray();
        //压缩处理
        if (content.length > 1024 && compressType > 0) {
            Compression compression = COMPRESSION_SELECTOR.select(compressType);
            if (compression != null) {
                //复用内存缓冲区
                baos.reset();
                baos.write(new byte[]{1, 0, 0, 0, 0});
                content = compression.compress(baos, content, 5, content.length - 5);
                headers.set(GrpcUtil.MESSAGE_ENCODING, compression.getTypeName());
            }
        }
        //设置长度
        int length = content.length - 5;
        content[1] = (byte) (length >>> 24);
        content[2] = (byte) (length >>> 16);
        content[3] = (byte) (length >>> 8);
        content[4] = (byte) length;
        //streamId会在后续的处理器中设置
        //Stream IDs on the client MUST start at 1 and increment by 2 sequentially, such as 1, 3, 5, 7, etc.
        //Stream IDs on the server MUST start at 2 and increment by 2 sequentially, such as 2, 4, 6, 8, etc.
        return new DefaultHttp2RequestMessage(0, message.getMsgId(), headers, content);
    }

    /**
     * 构建头部
     *
     * @param invocation 调用请求
     * @param session    会话
     * @param channel    channel
     * @return 头部
     */
    protected Http2Headers buildHeaders(final Invocation invocation, final Session session, final Channel channel) {
        //设置headers
        final InetSocketAddress remoteAddress = channel.getRemoteAddress();
        String authority = remoteAddress.getHostName() + ":" + remoteAddress.getPort();
        String path = "/" + invocation.getClassName() + "/" + invocation.getMethodName();
        //创建http2header对象
        Http2Headers headers = new DefaultHttp2Headers().authority(authority).path(path).method(HttpMethod.POST).scheme("http");
        //隐藏参数
        Map<String, Object> attachments = invocation.getAttachments();
        if (attachments != null) {
            attachments.forEach((key, value) -> {
                if (value != null) {
                    Class<?> clazz = value.getClass();
                    if (CharSequence.class.isAssignableFrom(clazz)
                            || clazz.isPrimitive() || clazz.isEnum()
                            || Boolean.class == clazz
                            || Number.class.isAssignableFrom(clazz)) {
                        headers.set(key, value.toString());
                    } else {
                        //其它的转换成JSON
                        headers.set(key, JSON.get().toJSONString(value));
                    }
                }
            });
        }
        headers.set(GrpcUtil.CONTENT_TYPE_KEY.name(), GrpcUtil.CONTENT_TYPE_GRPC);
        headers.set(GrpcUtil.TE_HEADER.name(), GrpcUtil.TE_TRAILERS);
        headers.set(GrpcUtil.USER_AGENT_KEY.name(), GlobalContext.getString(PROTOCOL_VERSION_KEY));
        headers.set(ALIAS_OPTION.getName(), invocation.getAlias());
        String acceptEncoding = GZIP;
        if (session != null && session.getCompressions() != null) {
            acceptEncoding = String.join(",", session.getCompressions());
        }
        headers.set(GrpcUtil.MESSAGE_ACCEPT_ENCODING, acceptEncoding);

        return headers;
    }

    /**
     * 包装载体，进行类型转换
     *
     * @param invocation 调用请求
     * @param methodDesc 类型
     * @return 包装的对象
     */
    protected Object wrapPayload(final Invocation invocation, final IDLMethodDesc methodDesc) {
        Object payLoad;
        Object[] args = invocation.getArgs();
        IDLType wrapper = methodDesc.getRequest();
        if (wrapper == null) {
            //不需要转换
            payLoad = (args == null || args.length == 0) ? null : args[0];
        } else if (wrapper.isWrapper()) {
            //加快构建性能
            payLoad = wrapper.getConversion().getToWrapper().apply(args);

        } else {
            payLoad = args[0];
        }
        return payLoad;
    }

}
