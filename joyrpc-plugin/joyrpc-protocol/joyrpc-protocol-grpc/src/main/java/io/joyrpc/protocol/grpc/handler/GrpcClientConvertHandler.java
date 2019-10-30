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
import io.joyrpc.exception.CodecException;
import io.joyrpc.exception.MethodOverloadException;
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
import io.joyrpc.transport.session.Session;
import io.joyrpc.util.GrpcType;
import io.joyrpc.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.joyrpc.Plugin.*;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.protocol.grpc.GrpcServerProtocol.GRPC_NUMBER;
import static io.joyrpc.transport.http.HttpHeaders.Values.GZIP;
import static io.joyrpc.util.ClassUtils.*;
import static io.joyrpc.util.ClassUtils.setValue;
import static io.joyrpc.util.GrpcType.F_RESULT;

/**
 * grpc client端消息转换handler
 */
public class GrpcClientConvertHandler extends AbstractHttpHandler {

    private final static Logger logger = LoggerFactory.getLogger(GrpcClientConvertHandler.class);

    protected static final String USER_AGENT = "joyrpc";

    protected Serialization serialization = SERIALIZATION_SELECTOR.select((byte) Serialization.PROTOBUF_ID);

    protected Map<Integer, Http2ResponseMessage> http2ResponseNoEnds = new ConcurrentHashMap<>();

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Object received(ChannelContext ctx, Object message) {
        if (message instanceof Http2ResponseMessage) {
            Http2ResponseMessage http2RespMsg = (Http2ResponseMessage) message;
            try {
                return convert(ctx.getChannel(), http2RespMsg);
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
    public Object wrote(ChannelContext ctx, Object message) {
        if (message instanceof RequestMessage) {
            try {
                return convert(ctx.getChannel(), (RequestMessage) message);
            } catch (Exception e) {
                logger.error(String.format("Error occurs while write grpc request from %s", Channel.toString(ctx.getChannel().getRemoteAddress())), e);
                throw new RpcException(((RequestMessage) message).getHeader(), e);
            }
        } else {
            return message;
        }
    }

    /**
     * 转换
     *
     * @param channel
     * @param http2ResponseMsg
     * @return
     */
    protected ResponseMessage convert(Channel channel, Http2ResponseMessage http2ResponseMsg) throws IOException {
        if (http2ResponseMsg.getStreamId() <= 0) {
            return null;
        }
        Http2ResponseMessage http2Msg = null;
        if (http2ResponseMsg.endHeaders() == null) {
            http2ResponseNoEnds.put(http2ResponseMsg.getStreamId(), http2ResponseMsg);
            return null;
        } else {
            http2Msg = http2ResponseNoEnds.remove(http2ResponseMsg.getStreamId());
            if (http2Msg != null) {
                http2Msg.setEndHeaders(http2ResponseMsg.endHeaders());
            } else {
                http2Msg = http2ResponseMsg;
            }
        }
        MessageHeader header = new MessageHeader(serialization.getTypeId(), MsgType.BizResp.getType(), GRPC_NUMBER);
        header.setMsgId(http2Msg.getBizMsgId());
        header.addAttribute(HeaderMapping.STREAM_ID.getNum(), http2Msg.getStreamId());
        ResponsePayload palyLoad;
        Object grpcStatusVal = http2Msg.endHeaders().get(GRPC_STATUS_KEY);
        int grpcStatus = grpcStatusVal == null ? Status.Code.UNKNOWN.value() : Integer.valueOf(grpcStatusVal.toString());
        if (grpcStatus == Status.Code.OK.value()) {
            EnhanceCompletableFuture future = channel.getFutureManager().get(http2Msg.getBizMsgId());
            if (future != null) {
                InputStream in = new UnsafeByteArrayInputStream(http2Msg.content());
                //读压缩位标识
                int isCompression = in.read();
                //读4位
                in.read(new byte[4], 0, 4);
                //解压处理
                if (isCompression > 0) {
                    Pair<String, Compression> pair = getEncoding((String) http2Msg.headers().get(GrpcUtil.MESSAGE_ACCEPT_ENCODING));
                    if (pair != null) {
                        in = pair.getValue().decompress(in);
                    }
                }
                //反序列化
                ReturnType returnType = (ReturnType) future.getAttr();
                Object response = serialization.getSerializer().deserialize(in, returnType.getReturnType());
                if (returnType.isWrapper()) {
                    response = getValue(returnType.getReturnType(), F_RESULT, response);
                }
                palyLoad = new ResponsePayload(response);
            } else {
                palyLoad = new ResponsePayload(new GrpcBizException(String.format("request is timeout. id=%d", http2Msg.getBizMsgId())));
            }
        } else {
            Status status = Status.fromCodeValue(grpcStatus);
            String errMsg = String.format("%s [%d]: %s", status.getCode().name(), grpcStatus, http2Msg.headers().get(GRPC_MESSAGE_KEY));
            palyLoad = new ResponsePayload(new GrpcBizException(errMsg));
        }
        return new ResponseMessage<>(header, palyLoad);
    }

    /**
     * 转换
     *
     * @param channel
     * @param message
     * @return
     */
    protected Http2RequestMessage convert(final Channel channel, final RequestMessage message) throws IOException, NoSuchMethodException, MethodOverloadException, ClassNotFoundException {
        if (!(message.getPayLoad() instanceof Invocation)) {
            throw new CodecException("no such kind of message.");
        }
        Invocation invocation = (Invocation) message.getPayLoad();
        //设置headers
        final InetSocketAddress remoteAddress = channel.getRemoteAddress();
        String authority = remoteAddress.getHostName() + ":" + remoteAddress.getPort();
        String path = "/" + invocation.getClassName() + "/" + invocation.getMethodName();
        //创建http2header对象
        Http2Headers headers = new DefaultHttp2Headers().authority(authority).path(path).method(HttpMethod.POST).scheme("http");
        headers.set(GrpcUtil.CONTENT_TYPE_KEY.name(), GrpcUtil.CONTENT_TYPE_GRPC);
        headers.set(GrpcUtil.TE_HEADER.name(), GrpcUtil.TE_TRAILERS);
        headers.set(GrpcUtil.USER_AGENT_KEY.name(), USER_AGENT);
        headers.set(ALIAS_OPTION.getName(), invocation.getAlias());
        String acceptEncoding = GZIP;
        Session session = message.getSession();
        if (session != null && session.getCompressions() != null) {
            acceptEncoding = String.join(",", session.getCompressions());
        }
        headers.set(GrpcUtil.MESSAGE_ACCEPT_ENCODING, acceptEncoding);
        //设置content
        UnsafeByteArrayOutputStream outputStream = new UnsafeByteArrayOutputStream();
        //是否压缩
        outputStream.write(0);
        //长度(占位)
        outputStream.write(new byte[]{0, 0, 0, 0}, 0, 4);
        //设置payload
        Object payLoad;
        //做grpc入参与返回值的类型转换，获取GrpcType
        GrpcType grpcType = getGrpcType(invocation.getClazz(), invocation.getMethodName(), (c, m) -> GRPC_FACTORY.get().generate(c, m));
        GrpcType.ClassWrapper reqWrapper = grpcType.getRequest();
        //根据reqWrapper，转换payLoad
        if (reqWrapper == null) {
            payLoad = (invocation.getArgs() == null || invocation.getArgs().length == 0) ? null : invocation.getArgs()[0];
        } else if (reqWrapper.isWrapper()) {
            payLoad = newInstance(reqWrapper.getClazz());
            List<Field> wrapperFields = getFields(payLoad.getClass());
            int i = 0;
            for (Field field : wrapperFields) {
                setValue(reqWrapper.getClazz(), field, payLoad, invocation.getArgs()[i++]);
            }
        } else {
            payLoad = invocation.getArgs()[0];
        }
        //将返回值类型放到 future 中
        GrpcType.ClassWrapper respWrapper = grpcType.getResponse();
        if (respWrapper != null) {
            channel.getFutureManager().get(message.getMsgId()).setAttr(new ReturnType(respWrapper.getClazz(), reqWrapper.isWrapper()));
        } else {
            Method reqMethod = invocation.getMethod();
            if (reqMethod == null) {
                reqMethod = getPublicMethod(invocation.getClassName(), invocation.getMethodName());
            }
            channel.getFutureManager().get(message.getMsgId()).setAttr(new ReturnType(reqMethod.getReturnType(), false));
        }
        //序列化
        if (payLoad != null) {
            serialization.getSerializer().serialize(outputStream, payLoad);
        }
        //获取 content 字节数组
        byte[] content = outputStream.toByteArray();
        //压缩处理
        if (content.length > 1024 && message.getHeader().getCompression() > 0) {
            Compression compression = COMPRESSION_SELECTOR.select(message.getHeader().getCompression());
            if (compression != null) {
                UnsafeByteArrayOutputStream compressionOutputStream = new UnsafeByteArrayOutputStream();
                compressionOutputStream.write(new byte[]{1, 0, 0, 0, 0});
                content = compress(compression, compressionOutputStream, content, 5, content.length - 5);
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
     * 返回值类型存储类，存储在响应Future中
     */
    protected class ReturnType {
        /**
         * 返回值类型
         */
        private Class returnType;
        /**
         * 是否为转换类
         */
        private boolean wrapper;

        public ReturnType(Class returnType, boolean wrapper) {
            this.returnType = returnType;
            this.wrapper = wrapper;
        }

        public Class getReturnType() {
            return returnType;
        }

        public void setReturnType(Class returnType) {
            this.returnType = returnType;
        }

        public boolean isWrapper() {
            return wrapper;
        }

        public void setWrapper(boolean wrapper) {
            this.wrapper = wrapper;
        }
    }

}
