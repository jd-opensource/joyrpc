package io.joyrpc.protocol.http.controller;

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
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.AbstractHttpDecoder;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.Protocol;
import io.joyrpc.protocol.http.HttpController;
import io.joyrpc.protocol.http.URLBinding;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.MessageHeader;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.http.HttpHeaders;
import io.joyrpc.transport.http.HttpMethod;
import io.joyrpc.transport.http.HttpRequestMessage;
import io.joyrpc.util.SystemClock;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static io.joyrpc.Plugin.GENERIC_SERIALIZER;
import static io.joyrpc.codec.serialization.GenericSerializer.JSON;
import static io.joyrpc.protocol.http.HeaderMapping.ACCEPT_ENCODING;
import static io.joyrpc.protocol.http.HeaderMapping.KEEP_ALIVE;
import static io.joyrpc.protocol.http.Plugin.URL_BINDING;
import static io.joyrpc.transport.http.HttpHeaders.Names.CONTENT_LENGTH;

/**
 * 默认Http控制器
 */
@Extension("default")
public class DefaultHttpController implements HttpController {

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

    public DefaultHttpController() {
        defSerializer = GENERIC_SERIALIZER.get(JSON);
        binding = URL_BINDING.get();
    }

    @Override
    public Object execute(ChannelContext ctx, HttpRequestMessage message, URL url, List<String> params) throws Exception {
        Map<CharSequence, Object> headerMap = message.headers().getAll();
        Parametric parametric = new MapParametric(headerMap);
        Invocation invocation = new HttpDecoder().url(url)
                .header(parametric)
                .body(message.content())
                .params(params)
                .httpMethod(message.getHttpMethod())
                .serializer(defSerializer)
                .binding(binding)
                .error(EXCEPTION_SUPPLIER)
                .build();

        // 构建joy请求
        MessageHeader header = createHeader();
        header.setLength(parametric.getPositive(CONTENT_LENGTH, (Integer) null));
        header.addAttribute(KEEP_ALIVE.getNum(), message.headers().isKeepAlive());
        header.addAttribute(ACCEPT_ENCODING.getNum(), parametric.getString(HttpHeaders.Names.ACCEPT_ENCODING));
        header.setTimeout(parametric.getTimeout(Constants.TIMEOUT_OPTION));
        // 解析远程地址
        return RequestMessage.build(header, invocation, ctx.getChannel(), parametric, SystemClock.now());
    }

    /**
     * 创建请求消息头
     *
     * @return 消息头
     */
    protected MessageHeader createHeader() {
        return new MessageHeader(MsgType.BizReq.getType(), (byte) Serialization.JSON_ID, (byte) Protocol.HTTP);
    }

    /**
     * 标准的Http调用的构建器
     */
    protected static class HttpDecoder extends AbstractHttpDecoder {
        /**
         * HttpMethod
         */
        protected HttpMethod httpMethod;
        /**
         * 参数名称
         */
        protected List<String> params;
        /**
         * 默认的序列化
         */
        protected GenericSerializer serializer;
        /**
         * 数据绑定
         */
        protected URLBinding binding;

        @Override
        public HttpDecoder url(final URL url) {
            return (HttpDecoder) super.url(url);
        }

        @Override
        public HttpDecoder paths(final String[] paths) {
            return (HttpDecoder) super.paths(paths);
        }

        @Override
        public HttpDecoder header(final Parametric header) {
            return (HttpDecoder) super.header(header);
        }

        public HttpDecoder httpMethod(final HttpMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        @Override
        public HttpDecoder body(final byte[] body) {
            return (HttpDecoder) super.body(body);
        }

        public HttpDecoder params(final List<String> params) {
            this.params = params;
            return this;
        }

        public HttpDecoder serializer(final GenericSerializer serializer) {
            this.serializer = serializer;
            return this;
        }

        public HttpDecoder binding(final URLBinding binding) {
            this.binding = binding;
            return this;
        }

        @Override
        public HttpDecoder error(final Supplier<LafException> error) {
            return (HttpDecoder) super.error(error);
        }

        @Override
        protected void parseArg(final Invocation invocation) throws Exception {
            Method method = invocation.getMethod();
            Parameter[] parameters = method.getParameters();
            if (parameters.length == 0) {
                invocation.setArgs(new Object[0]);
            } else {
                Object[] args;
                //判断是否有参数名称
                boolean hasName = parameters[0].isNamePresent();
                if (httpMethod != HttpMethod.GET) {
                    //获取压缩
                    Compression compression = getCompression(HttpHeaders.Names.CONTENT_ENCODING);
                    //解压缩
                    byte[] content = compression == null ? body : compression.decompress(body);
                    //构造泛化调用参数
                    invocation.setArgs(new Object[]{invocation.getMethodName(), null, new Object[]{content}});
                    //反序列化
                    args = serializer.deserialize(invocation);
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
                invocation.setArgs(args);
            }
        }

        /**
         * 判断参数名称是否一样
         *
         * @param params     参数
         * @param parameters 参数
         * @return 匹配标识
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

    }
}
