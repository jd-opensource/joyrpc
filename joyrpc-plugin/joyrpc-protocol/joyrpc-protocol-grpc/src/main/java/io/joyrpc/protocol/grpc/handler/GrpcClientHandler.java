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
import io.grpc.internal.GrpcUtil;
import io.joyrpc.codec.compression.Compression;
import io.joyrpc.codec.serialization.Serialization;
import io.joyrpc.codec.serialization.UnsafeByteArrayInputStream;
import io.joyrpc.codec.serialization.UnsafeByteArrayOutputStream;
import io.joyrpc.config.InterfaceOption;
import io.joyrpc.exception.RpcException;
import io.joyrpc.protocol.AbstractHttpHandler;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.grpc.HeaderMapping;
import io.joyrpc.protocol.grpc.exception.GrpcBizException;
import io.joyrpc.protocol.message.*;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.channel.EnhanceCompletableFuture;
import io.joyrpc.transport.http.HttpMethod;
import io.joyrpc.transport.http2.*;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.session.Session;
import io.joyrpc.util.GrpcType;
import io.joyrpc.util.GrpcType.ClassWrapper;
import io.joyrpc.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.joyrpc.Plugin.*;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.protocol.grpc.GrpcServerProtocol.GRPC_NUMBER;
import static io.joyrpc.transport.http.HttpHeaders.Values.GZIP;
import static io.joyrpc.util.ClassUtils.*;
import static io.joyrpc.util.GrpcType.F_RESULT;

/**
 * grpc client端消息转换handler
 */
public class GrpcClientHandler extends AbstractHttpHandler {

    private final static Logger logger = LoggerFactory.getLogger(GrpcClientHandler.class);

    protected static final String USER_AGENT = "joyrpc";

    protected Serialization serialization = SERIALIZATION_SELECTOR.select((byte) Serialization.PROTOBUF_ID);

    protected Map<Integer, Http2ResponseMessage> http2ResponseNoEnds = new ConcurrentHashMap<>();

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public Object received(final ChannelContext ctx, final Object message) {
        if (message instanceof Http2ResponseMessage) {
            Http2ResponseMessage http2RespMsg = (Http2ResponseMessage) message;
            try {
                return input(ctx.getChannel(), http2RespMsg);
            } catch (Throwable e) {
                logger.error(String.format("Error occurs while parsing grpc request from %s", Channel.toString(ctx.getChannel().getRemoteAddress())), e);
                MessageHeader header = new MessageHeader((byte) Serialization.PROTOBUF_ID, MsgType.BizReq.getType(), GRPC_NUMBER);
                header.setMsgId(((Http2Message) message).getBizMsgId());
                header.addAttribute(HeaderMapping.STREAM_ID.getNum(), ((Http2Message) message).getStreamId());
                throw new RpcException(header, e);
            }
        } else {
            return message;
        }
    }

    @Override
    public Object wrote(final ChannelContext ctx, final Object message) {
        if (message instanceof RequestMessage) {
            try {
                return output(ctx.getChannel(), (RequestMessage<?>) message);
            } catch (Exception e) {
                logger.error(String.format("Error occurs while write grpc request from %s", Channel.toString(ctx.getChannel().getRemoteAddress())), e);
                throw new RpcException(((RequestMessage) message).getHeader(), e);
            }
        } else {
            return message;
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
        MessageHeader header = new MessageHeader(serialization.getTypeId(), MsgType.BizResp.getType(), GRPC_NUMBER);
        header.setMsgId(http2Msg.getBizMsgId());
        header.addAttribute(HeaderMapping.STREAM_ID.getNum(), http2Msg.getStreamId());
        ResponsePayload payload;
        Object grpcStatusVal = http2Msg.endHeaders().get(GRPC_STATUS_KEY);
        int grpcStatus = grpcStatusVal == null ? Status.Code.UNKNOWN.value() : Integer.parseInt(grpcStatusVal.toString());
        if (grpcStatus == Status.Code.OK.value()) {
            EnhanceCompletableFuture<Integer, Message> future = channel.getFutureManager().get(http2Msg.getBizMsgId());
            if (future != null) {
                payload = decodePayload(http2Msg, (ReturnType) future.getAttr());
            } else {
                payload = new ResponsePayload(new GrpcBizException(String.format("request is timeout. id=%d", http2Msg.getBizMsgId())));
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
     * @param message    消息
     * @param returnType 返回类型
     * @return 应答
     * @throws IOException
     */
    protected ResponsePayload decodePayload(final Http2ResponseMessage message, final ReturnType returnType) throws IOException {
        Http2Headers headers = message.headers();
        InputStream in = new UnsafeByteArrayInputStream(message.content());
        //读压缩位标识
        int isCompression = in.read();
        //读长度共4位
        if (in.skip(4) < 4) {
            throw new IOException(String.format("request data is not full. id=%d", message.getBizMsgId()));
        }
        //解压处理
        if (isCompression > 0) {
            Pair<String, Compression> pair = getEncoding((String) headers.get(GrpcUtil.MESSAGE_ACCEPT_ENCODING));
            if (pair != null) {
                in = pair.getValue().decompress(in);
            }
        }
        //反序列化
        Object response = serialization.getSerializer().deserialize(in, returnType.getReturnType());
        if (returnType.isWrapper()) {
            response = getValue(returnType.getReturnType(), F_RESULT, response);
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
        if (message.endHeaders() == null) {
            http2ResponseNoEnds.put(message.getStreamId(), message);
            return null;
        } else {
            http2Msg = http2ResponseNoEnds.remove(message.getStreamId());
            if (http2Msg != null) {
                http2Msg.setEndHeaders(message.endHeaders());
            } else {
                http2Msg = message;
            }
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
        GrpcType grpcType = option.getArgType().getGrpcType();
        //包装payload
        Object payLoad = wrapPayload(invocation, grpcType);
        //将返回值类型放到 future 中
        EnhanceCompletableFuture<Integer, Message> future = channel.getFutureManager().get(message.getMsgId());
        storeReturnType(invocation, grpcType, future);

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
                content = compress(compression, baos, content, 5, content.length - 5);
                headers.set(GrpcUtil.MESSAGE_ENCODING, compression.getTypeName());
            }
        }
        //设置长度
        int length = content.length - 5;
        content[1] = (byte) (length >>> 24);
        content[2] = (byte) (length >>> 16);
        content[3] = (byte) (length >>> 8);
        content[4] = (byte) length;
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
        headers.set(GrpcUtil.USER_AGENT_KEY.name(), USER_AGENT);
        headers.set(ALIAS_OPTION.getName(), invocation.getAlias());
        String acceptEncoding = GZIP;
        if (session != null && session.getCompressions() != null) {
            acceptEncoding = String.join(",", session.getCompressions());
        }
        headers.set(GrpcUtil.MESSAGE_ACCEPT_ENCODING, acceptEncoding);

        return headers;
    }

    /**
     * 存储返回类型
     *
     * @param invocation 调用请求
     * @param grpcType   grpc类型
     * @param future     future
     */
    protected void storeReturnType(final Invocation invocation, final GrpcType grpcType,
                                   final EnhanceCompletableFuture<Integer, Message> future) {
        ClassWrapper respWrapper = grpcType.getResponse();
        if (respWrapper != null) {
            future.setAttr(new ReturnType(respWrapper.getClazz(), respWrapper.isWrapper()));
        } else {
            //调用的时候已经赋值了方法
            future.setAttr(new ReturnType(invocation.getMethod().getReturnType(), false));
        }
    }

    /**
     * 包装payload
     *
     * @param invocation 调用请求
     * @param grpcType   类型
     * @return 包装的对象
     */
    protected Object wrapPayload(final Invocation invocation, final GrpcType grpcType) {
        Object payLoad;
        Object[] args = invocation.getArgs();
        ClassWrapper wrapper = grpcType.getRequest();
        //根据reqWrapper，转换payLoad
        if (wrapper == null) {
            payLoad = (args == null || args.length == 0) ? null : args[0];
        } else if (wrapper.isWrapper()) {
            payLoad = newInstance(wrapper.getClazz());
            List<Field> wrapperFields = getFields(payLoad.getClass());
            int i = 0;
            for (Field field : wrapperFields) {
                setValue(wrapper.getClazz(), field, payLoad, args[i++]);
            }
        } else {
            payLoad = args[0];
        }
        return payLoad;
    }

    /**
     * 返回值类型存储类，存储在响应Future中
     */
    protected static class ReturnType {
        /**
         * 返回值类型
         */
        protected Class<?> returnType;
        /**
         * 是否为转换类
         */
        protected boolean wrapper;

        public ReturnType(Class<?> returnType, boolean wrapper) {
            this.returnType = returnType;
            this.wrapper = wrapper;
        }

        public Class<?> getReturnType() {
            return returnType;
        }

        public boolean isWrapper() {
            return wrapper;
        }
    }

}
