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
import io.joyrpc.codec.serialization.Serializer;
import io.joyrpc.codec.serialization.UnsafeByteArrayInputStream;
import io.joyrpc.codec.serialization.UnsafeByteArrayOutputStream;
import io.joyrpc.exception.CodecException;
import io.joyrpc.exception.LafException;
import io.joyrpc.exception.RpcException;
import io.joyrpc.extension.MapParametic;
import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.AbstractHttpHandler;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.grpc.HeaderMapping;
import io.joyrpc.protocol.grpc.message.GrpcResponseMessage;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.MessageHeader;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.http2.*;
import io.joyrpc.util.GrpcType;
import io.joyrpc.util.Pair;
import io.joyrpc.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static io.joyrpc.Plugin.GRPC_FACTORY;
import static io.joyrpc.Plugin.SERIALIZATION_SELECTOR;
import static io.joyrpc.constants.Constants.GRPC_MESSAGE_KEY;
import static io.joyrpc.constants.Constants.GRPC_STATUS_KEY;
import static io.joyrpc.protocol.grpc.GrpcServerProtocol.GRPC_NUMBER;
import static io.joyrpc.protocol.grpc.HeaderMapping.ACCEPT_ENCODING;
import static io.joyrpc.util.ClassUtils.*;
import static io.joyrpc.util.GrpcType.F_RESULT;

/**
 * @date: 2019/5/6
 */
public class GrpcServerConvertHandler extends AbstractHttpHandler {

    private final static Logger logger = LoggerFactory.getLogger(GrpcClientConvertHandler.class);
    public static final Supplier<LafException> EXCEPTION_SUPPLIER = () -> new CodecException(":path interfaceClazz/methodName with alias header or interfaceClazz/alias/methodName");
    /**
     * 默认序列化
     */
    protected Serialization serialization = SERIALIZATION_SELECTOR.select((byte) Serialization.PROTOBUF_ID);

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Object received(final ChannelContext ctx, final Object message) {
        if (message instanceof Http2RequestMessage) {
            Http2RequestMessage http2Req = (Http2RequestMessage) message;
            try {
                return convert(http2Req, ctx.getChannel(), SystemClock.now());
            } catch (Throwable e) {
                logger.error(String.format("Error occurs while parsing grpc request from %s", Channel.toString(ctx.getChannel().getRemoteAddress())), e);
                MessageHeader header = new MessageHeader();
                header.addAttribute(HeaderMapping.STREAM_ID.getNum(), http2Req.getStreamId());
                header.setMsgId(http2Req.getBizMsgId());
                header.setMsgType(MsgType.BizReq.getType());
                throw new RpcException(header, e);
            }
        } else {
            return message;
        }
    }

    @Override
    public Object wrote(final ChannelContext ctx, final Object message) {
        if (message instanceof GrpcResponseMessage) {
            GrpcResponseMessage grpcResp = (GrpcResponseMessage) message;
            try {
                return convert(grpcResp);
            } catch (Exception e) {
                logger.error(String.format("Error occurs while wrote grpc response from %s", Channel.toString(ctx.getChannel().getRemoteAddress())), e);
                throw new RpcException(grpcResp.getHeader(), e);
            }
        } else {
            return message;
        }
    }

    /**
     * 构造请求消息
     *
     * @param message
     * @param channel
     * @return
     * @throws Exception
     */
    protected RequestMessage<Invocation> convert(final Http2RequestMessage message,
                                                 final Channel channel,
                                                 final long receiveTime) throws Exception {
        if (message.getStreamId() <= 0) {
            return null;
        }
        Http2Headers httpHeaders = message.headers();
        Map<CharSequence, Object> headerMap = httpHeaders.getAll();
        Parametric parametric = new MapParametic(headerMap);
        // 解析uri
        String path = parametric.getString(Http2Headers.PseudoHeaderName.PATH.value(), "/");
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        URL url = URL.valueOf(path, "http");
        //消息头
        MessageHeader header = new MessageHeader(serialization.getTypeId(), MsgType.BizReq.getType(), GRPC_NUMBER);
        header.setMsgId(message.getBizMsgId());
        header.setMsgType(MsgType.BizReq.getType());
        header.setTimeout(getTimeout(parametric, GrpcUtil.TIMEOUT));
        header.addAttribute(HeaderMapping.STREAM_ID.getNum(), message.getStreamId());
        header.addAttribute(ACCEPT_ENCODING.getNum(), parametric.getString(GrpcUtil.MESSAGE_ACCEPT_ENCODING));
        //获取压缩类型
        Compression compression = getCompression(parametric, GrpcUtil.MESSAGE_ENCODING);
        //构造invocation
        Invocation invocation = Invocation.build(url, headerMap, EXCEPTION_SUPPLIER);
        //构造消息输入流
        UnsafeByteArrayInputStream in = new UnsafeByteArrayInputStream(message.content());
        in.read(new byte[5], 0, 5);
        Object[] args = new Object[invocation.getMethod().getParameterCount()];
        //获取 grpcType
        GrpcType grpcType = getGrpcType(invocation.getClazz(), invocation.getMethodName(), (c, m) -> GRPC_FACTORY.get().generate(c, m));
        GrpcType.ClassWrapper reqWrapper = grpcType.getRequest();
        //如果方法没有参数，则返回null
        if (reqWrapper != null) {
            //获取反序列化插件
            Serializer serializer = getSerialization(parametric, GrpcUtil.CONTENT_ENCODING, serialization).getSerializer();
            //反序列化 wrapper
            Object wrapperObj = serializer.deserialize(compression == null ? in : compression.decompress(in), reqWrapper.getClazz());
            //isWrapper为true，为包装对象，遍历每个field，逐个取值赋值给args数组，否则，直接赋值args[0]
            if (reqWrapper.isWrapper()) {
                List<Field> wrapperFields = getFields(wrapperObj.getClass());
                int i = 0;
                for (Field field : wrapperFields) {
                    args[i++] = getValue(wrapperObj.getClass(), field, wrapperObj);
                }
            } else {
                args[0] = wrapperObj;
            }
        }
        invocation.setArgs(args);
        RequestMessage<Invocation> reqMessage = RequestMessage.build(header, invocation, channel, parametric, receiveTime);
        reqMessage.setResponseSupplier(() -> {
            MessageHeader respHeader = header.response(MsgType.BizResp.getType(), Compression.NONE, header.getAttributes());
            return new GrpcResponseMessage<>(respHeader, grpcType);
        });
        return reqMessage;
    }

    /**
     * 构建应答消息
     *
     * @param message
     * @return
     */
    protected Http2ResponseMessage convert(final GrpcResponseMessage message) throws IOException {
        int streamId = (Integer) message.getHeader().getAttributes().get(HeaderMapping.STREAM_ID.getNum());
        ResponsePayload responsePayload = (ResponsePayload) message.getPayLoad();
        if (!responsePayload.isError()) {
            //http2 header
            Http2Headers headers = new DefaultHttp2Headers();
            headers.status("200");
            headers.set(GrpcUtil.CONTENT_TYPE_KEY.name(), GrpcUtil.CONTENT_TYPE_GRPC);
            //获取payload
            Object payLoad = responsePayload.getResponse();
            //获取 grpcType
            GrpcType grpcType = message.getGrpcType();
            GrpcType.ClassWrapper respWrapper = grpcType.getResponse();
            //设置反正值
            Object respObj;
            if (respWrapper.isWrapper()) {
                respObj = newInstance(respWrapper.getClazz());
                setValue(respWrapper.getClazz(), F_RESULT, respObj, payLoad);
            } else {
                respObj = payLoad;
            }
            //设置content
            UnsafeByteArrayOutputStream outputStream = new UnsafeByteArrayOutputStream();
            //是否压缩
            outputStream.write(0);
            //长度(占位)
            outputStream.write(new byte[]{0, 0, 0, 0}, 0, 4);
            //序列化
            serialization.getSerializer().serialize(outputStream, respObj);
            //获取 content 字节数组
            byte[] content = outputStream.toByteArray();
            //压缩处理
            Pair<String, Compression> pair = null;
            if (content.length > 1024) {
                pair = getEncoding((String) message.getHeader().getAttribute(ACCEPT_ENCODING.getNum()));
            }
            if (pair != null) {
                UnsafeByteArrayOutputStream compressionOutputStream = new UnsafeByteArrayOutputStream();
                compressionOutputStream.write(new byte[]{1, 0, 0, 0, 0});
                content = compress(pair.getValue(), compressionOutputStream, content, 5, content.length - 5);
                headers.set(GrpcUtil.MESSAGE_ENCODING, pair.getKey());
            }
            //设置数据长度
            int length = content.length - 5;
            content[1] = (byte) (length >>> 24);
            content[2] = (byte) (length >>> 16);
            content[3] = (byte) (length >>> 8);
            content[4] = (byte) length;
            return new DefaultHttp2ResponseMessage(streamId, message.getHeader().getMsgId(),
                    headers, content, buildEndHttp2Headers());
        } else {
            return new DefaultHttp2ResponseMessage(streamId, message.getHeader().getMsgId(),
                    null, null, buildErrorEndHttp2Headers(responsePayload.getException()));
        }
    }

    /**
     * 设置结束头
     *
     * @return
     */
    public static Http2Headers buildEndHttp2Headers() {
        Http2Headers headers = new DefaultHttp2Headers();
        headers.status("200");
        headers.set(GrpcUtil.CONTENT_TYPE_KEY.name(), GrpcUtil.CONTENT_TYPE_GRPC);
        headers.set(GRPC_STATUS_KEY, Status.Code.OK.value());
        return headers;
    }

    /**
     * 设置异常结束头
     *
     * @param throwable
     * @return
     */
    public static Http2Headers buildErrorEndHttp2Headers(Throwable throwable) {
        String errorMsg = throwable.getClass().getName() + ":" + throwable.getMessage();
        Http2Headers headers = new DefaultHttp2Headers();
        headers.status("200");
        headers.set(GrpcUtil.CONTENT_TYPE_KEY.name(), GrpcUtil.CONTENT_TYPE_GRPC);
        headers.set(GRPC_STATUS_KEY, Status.Code.INTERNAL.value());
        headers.set(GRPC_MESSAGE_KEY, errorMsg);
        return headers;
    }

}
