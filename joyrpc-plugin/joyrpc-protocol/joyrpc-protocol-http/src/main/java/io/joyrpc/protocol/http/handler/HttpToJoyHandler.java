package io.joyrpc.protocol.http.handler;

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

import io.joyrpc.codec.compression.Compression;
import io.joyrpc.codec.serialization.GenericSerializer;
import io.joyrpc.codec.serialization.Serialization;
import io.joyrpc.constants.Constants;
import io.joyrpc.exception.CodecException;
import io.joyrpc.exception.LafException;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.AbstractHttpHandler;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.http.HeaderMapping;
import io.joyrpc.protocol.http.URLBinding;
import io.joyrpc.protocol.message.*;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.http.HttpHeaders;
import io.joyrpc.transport.http.HttpMethod;
import io.joyrpc.transport.http.HttpRequestMessage;
import io.joyrpc.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;
import java.util.*;
import java.util.function.Supplier;

import static io.joyrpc.Plugin.GENERIC_SERIALIZER;
import static io.joyrpc.codec.serialization.GenericSerializer.JSON;
import static io.joyrpc.protocol.http.HeaderMapping.ACCEPT_ENCODING;
import static io.joyrpc.protocol.http.HeaderMapping.KEEP_ALIVE;
import static io.joyrpc.protocol.http.Plugin.URL_BINDING;
import static io.joyrpc.transport.http.HttpHeaders.Names.CONTENT_LENGTH;

/**
 * HTTP转换成joy
 */
public class HttpToJoyHandler extends AbstractHttpHandler {
    public static final byte PROTOCOL_NUMBER = 9;

    private final static Logger logger = LoggerFactory.getLogger(HttpToJoyHandler.class);
    public static final Supplier<LafException> EXCEPTION_SUPPLIER = () -> new CodecException(
            "HTTP uri format: http://ip:port/interfaceClazz/methodName with alias header. " +
                    "or http://ip:port/interfaceClazz/alias/methodName");
    /**
     * 默认序列化器
     */
    protected GenericSerializer defSerializer;
    /**
     * 数据绑定
     */
    protected URLBinding binding;

    /**
     * 构造函数
     */
    public HttpToJoyHandler() {
        defSerializer = GENERIC_SERIALIZER.get(JSON);
        binding = URL_BINDING.get();
        binding = binding == null ? this::convert : binding;
    }

    @Override
    public Object received(final ChannelContext ctx, final Object msg) {
        if (!(msg instanceof HttpRequestMessage)) {
            return msg;
        }
        HttpRequestMessage message = (HttpRequestMessage) msg;
        long receiveTime = SystemClock.now();
        // 如果服务端开启容许keep-alive并且客户端是keep-alive方式调用
        boolean isKeepAlive = message.headers().isKeepAlive();
        HttpMethod method = message.getHttpMethod();
        String uri = message.getUri();
        try {
            switch (method) {
                case GET:
                case POST:
                case PUT:
                    break;
                default:
                    writeBack(ctx.getChannel(), false, "Only allow GET POST and PUT", isKeepAlive);
                    return null;
            }
            if ("/favicon.ico".equals(uri)) {
                writeBack(ctx.getChannel(), true, "", isKeepAlive);
                return null;
            }
            return convert(ctx.getChannel(), message, isKeepAlive, receiveTime);
        } catch (Throwable e) {
            // 解析请求body
            logger.error(String.format("Error occurs while parsing http request for uri %s from %s", uri, Channel.toString(ctx.getChannel().getRemoteAddress())), e);
            //write error msg back
            writeBack(ctx.getChannel(), false, e.getMessage(), isKeepAlive);
        }
        return null;

    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    /**
     * 把HTTP请求转换成joy请求
     *
     * @param channel     通道
     * @param msg         消息
     * @param isKeepAlive 保持连接
     * @return
     * @throws Exception
     */
    protected RequestMessage<Invocation> convert(final Channel channel,
                                                 final HttpRequestMessage msg,
                                                 final boolean isKeepAlive,
                                                 final long receiveTime) throws Exception {
        HttpHeaders httpHeaders = msg.headers();
        Map<CharSequence, Object> headerMap = httpHeaders.getAll();
        Parametric parametric = new MapParametric(headerMap);
        // 解析uri
        List<String> params = new LinkedList<>();
        //相对路径，确保以"/"开头
        String uri = msg.getUri();
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        URL url = URL.valueOf(uri, "http", params);
        Invocation invocation = Invocation.build(url, headerMap, EXCEPTION_SUPPLIER);
        invocation.setArgs(parseArgs(invocation, msg, parametric, url, params));

        // 构建joy请求
        MessageHeader header = createHeader();
        header.setLength(parametric.getPositive(CONTENT_LENGTH, (Integer) null));
        header.addAttribute(KEEP_ALIVE.getNum(), isKeepAlive);
        header.addAttribute(ACCEPT_ENCODING.getNum(), parametric.getString(HttpHeaders.Names.ACCEPT_ENCODING));
        header.setTimeout(getTimeout(parametric, Constants.TIMEOUT_OPTION.getName()));
        // 解析远程地址
        return RequestMessage.build(header, invocation, channel, parametric, receiveTime);
    }

    /**
     * 创建请求消息头
     *
     * @return 消息头
     */
    protected MessageHeader createHeader() {
        return new MessageHeader(MsgType.BizReq.getType(), (byte) Serialization.JSON_ID, PROTOCOL_NUMBER);
    }

    /**
     * 解析参数
     *
     * @param invocation 请求
     * @param message    消息
     * @param parametric 参数化
     * @param url        url
     * @param params     参数名
     * @return 参数值对象
     */
    protected Object[] parseArgs(final Invocation invocation,
                                 final HttpRequestMessage message,
                                 final Parametric parametric,
                                 final URL url,
                                 final List<String> params) throws IOException {
        Method method = invocation.getMethod();
        Parameter[] parameters = method.getParameters();
        if (parameters.length == 0) {
            return new Object[0];
        }

        Object[] args;
        //判断是否有参数名称
        boolean hasName = parameters[0].isNamePresent();
        HttpMethod reqMethod = message.getHttpMethod();
        if (reqMethod != HttpMethod.GET) {
            //获取压缩
            Compression compression = getCompression(parametric, HttpHeaders.Names.CONTENT_ENCODING);
            //解压缩
            byte[] content = decompress(compression, message.content());
            invocation.setArgs(new Object[]{invocation.getMethodName(), null, new Object[]{content}});
            //反序列化
            args = defSerializer.deserialize(invocation);
        } else if (params.size() < parameters.length) {
            throw new CodecException("The number of parameter is wrong.");
        } else {
            args = new Object[parameters.length];
            //和老版本兼容，历史原因问题，可能请求的参数和名称不一致，只有一致的情况下才按照名称获取
            boolean match = hasName && isMatch(params, parameters);
            String name;
            Parameter parameter;
            for (int i = 0; i < parameters.length; i++) {
                parameter = parameters[i];
                name = match ? parameter.getName() : params.get(i);
                args[i] = binding.convert(invocation.getClass(), method, parameter, i, url.getString(name));
            }
        }
        return args;
    }

    /**
     * 判断参数名称是否一样
     *
     * @param params
     * @param parameters
     * @return
     */
    protected boolean isMatch(final List<String> params, final Parameter[] parameters) {
        Set<String> set = new HashSet<>(params);
        for (Parameter parameter : parameters) {
            if (!set.contains(parameter.getName())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 把字符串转换成参数类型
     *
     * @param clazz     类型
     * @param method    方法
     * @param parameter 参数类型
     * @param index     参数索引
     * @param value     字符串
     * @return
     */
    protected Object convert(final Class<?> clazz, final Method method, final Parameter parameter, final int index, final String value) {
        Class<?> type = parameter.getType();
        if (String.class.equals(type)) {
            try {
                return URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return value;
            }
        } else if (boolean.class.equals(type) || Boolean.class.equals(type)) {
            return Boolean.parseBoolean(value);
        } else if (byte.class.equals(type) || Byte.class.equals(type)) {
            return Byte.decode(value);
        } else if (short.class.equals(type) || Short.class.equals(type)) {
            return Short.decode(value);
        } else if (char.class.equals(type) || Character.class.equals(type)) {
            return value.charAt(0);
        } else if (int.class.equals(type) || Integer.class.equals(type)) {
            return Integer.decode(value);
        } else if (long.class.equals(type) || Long.class.equals(type)) {
            return Long.decode(value);
        } else if (float.class.equals(type) || Float.class.equals(type)) {
            return Float.valueOf(value);
        } else if (double.class.equals(type) || Double.class.equals(type)) {
            return Double.valueOf(value);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * 回写数据
     *
     * @param channel     通道
     * @param isSuccess   成功标识
     * @param message     消息
     * @param isKeepAlive 保持连接
     * @return 消息长度
     */
    protected int writeBack(final Channel channel, final boolean isSuccess, final String message, final boolean isKeepAlive) {
        ResponseMessage<ResponsePayload> response = new ResponseMessage<>();
        MessageHeader header = response.getHeader();
        header.addAttribute(HeaderMapping.CONTENT_TYPE.getNum(), isSuccess ? "text/html; charset=UTF-8" : "text/json; charset=UTF-8");
        header.addAttribute(KEEP_ALIVE.getNum(), isKeepAlive);
        response.setPayLoad(isSuccess ? new ResponsePayload(message) : new ResponsePayload(new Exception(message)));
        channel.send(response);

        return message.length();
    }

}
