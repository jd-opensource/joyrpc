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

import io.joyrpc.codec.UnsafeByteArrayInputStream;
import io.joyrpc.codec.compression.Compression;
import io.joyrpc.codec.serialization.GenericSerializer;
import io.joyrpc.codec.serialization.Serialization;
import io.joyrpc.codec.serialization.generic.StandardGenericSerializer;
import io.joyrpc.constants.Constants;
import io.joyrpc.exception.CodecException;
import io.joyrpc.exception.LafException;
import io.joyrpc.exception.MethodOverloadException;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.AbstractHttpDecoder;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.Protocol;
import io.joyrpc.protocol.http.ContentTypeHandler;
import io.joyrpc.protocol.http.message.JsonRpcRequest;
import io.joyrpc.protocol.http.message.JsonRpcResponse;
import io.joyrpc.protocol.http.message.JsonRpcResponseMessage;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.MessageHeader;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.http.HttpHeaders.Names;
import io.joyrpc.transport.http.HttpRequestMessage;
import io.joyrpc.util.SystemClock;

import java.lang.reflect.Array;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static io.joyrpc.Plugin.JSON;
import static io.joyrpc.constants.Constants.ALIAS_OPTION;
import static io.joyrpc.protocol.http.HeaderMapping.ACCEPT_ENCODING;
import static io.joyrpc.protocol.http.HeaderMapping.KEEP_ALIVE;
import static io.joyrpc.transport.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.joyrpc.util.ClassUtils.forName;
import static io.joyrpc.util.ClassUtils.getGenericClass;

/**
 * JsonRpc调用处理，处理Content-Type为"application/json-rpc"的调用
 */
@Extension("application/json-rpc")
public class JsonRpcController implements ContentTypeHandler {

    public static final String VERSION = "2.0";

    protected GenericSerializer genericSerializer = new StandardGenericSerializer();

    @Override
    public Object execute(final ChannelContext ctx, final HttpRequestMessage message, final URL url, final List<String> params) throws Exception {
        try {
            Map<CharSequence, Object> headerMap = message.headers().getAll();
            Parametric parametric = new MapParametric(headerMap);
            JsonRpcDecoder decoder = new JsonRpcDecoder().url(url)
                    .header(parametric)
                    .body(message.content())
                    .serializer(genericSerializer);
            Invocation invocation = decoder.build();
            // 构建joy请求
            MessageHeader header = new MessageHeader(MsgType.BizReq.getType(), (byte) Serialization.JSON_ID, (byte) Protocol.JSON_RPC);
            header.setLength(parametric.getPositive(CONTENT_LENGTH, (Integer) null));
            header.addAttribute(KEEP_ALIVE.getNum(), message.headers().isKeepAlive());
            header.addAttribute(ACCEPT_ENCODING.getNum(), parametric.getString(Names.ACCEPT_ENCODING));
            header.setTimeout(parametric.getTimeout(Constants.TIMEOUT_OPTION));
            // 解析远程地址
            RequestMessage result = RequestMessage.build(header, invocation, ctx.getChannel(), parametric, SystemClock.now());
            result.setResponseSupplier(() -> new JsonRpcResponseMessage(new JsonRpcResponse(VERSION, decoder.request.getId())));
            return result;
        } catch (SerializerException e) {
            throw new CodecException("Parse error", e, "-32700");
        } catch (ClassNotFoundException e) {
            throw new CodecException("Invalid Request", e, "-32600");
        } catch (NoSuchMethodException e) {
            throw new CodecException("Method not found", e, "-32601");
        } catch (MethodOverloadException e) {
            throw new CodecException("Invalid Request", e, "-32600");
        } catch (Throwable e) {
            throw new CodecException("Internal error", e, "-32603");
        }
    }

    /**
     * 标准的Http调用的构建器
     */
    protected static class JsonRpcDecoder extends AbstractHttpDecoder {

        protected GenericSerializer serializer;

        /**
         * 版本
         */
        protected transient JsonRpcRequest request;

        @Override
        public JsonRpcDecoder url(final URL url) {
            return (JsonRpcDecoder) super.url(url);
        }

        @Override
        public JsonRpcDecoder paths(final String[] paths) {
            return (JsonRpcDecoder) super.paths(paths);
        }

        @Override
        public JsonRpcDecoder header(final Parametric header) {
            return (JsonRpcDecoder) super.header(header);
        }

        @Override
        public JsonRpcDecoder body(final byte[] body) {
            return (JsonRpcDecoder) super.body(body);
        }

        public JsonRpcDecoder serializer(GenericSerializer serializer) {
            this.serializer = serializer;
            return this;
        }

        @Override
        public JsonRpcDecoder error(final Supplier<LafException> error) {
            return (JsonRpcDecoder) super.error(error);
        }

        @Override
        protected void parse() throws Exception {
            className = url.getPath();
            alias = header.getString(ALIAS_OPTION);
            intfClass = forName(className);
            genericClass = getGenericClass(intfClass);
            //获取压缩
            Compression compression = getCompression(Names.CONTENT_ENCODING);
            //解压缩
            byte[] content = compression == null ? body : compression.decompress(body);
            //反序列化
            request = JSON.get().parseObject(new UnsafeByteArrayInputStream(content), JsonRpcRequest.class);
            methodName = request.getMethod();
            if (methodName == null || request.getId() == null || !VERSION.equals(request.getJsonrpc())) {
                throw new CodecException("Invalid Request", "-32600");
            }
            parseMethod();
        }

        @Override
        protected void parseArg(final Invocation invocation) throws Exception {
            Object params = request.getParams();
            Parameter[] parameters = method.getParameters();
            if (parameters.length == 0) {
                return;
            } else if (params == null) {
                //参数不存在
                throw new CodecException("Invalid Request", "-32600");
            }
            Object[] args = new Object[parameters.length];
            // 判断是数组还是Map
            if (params instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) params;
                int i = 0;
                for (Parameter parameter : parameters) {
                    args[i] = map.get(parameter.getName());
                    i++;
                }
                invocation.setArgs(new Object[]{null, null, args});
                invocation.setArgs(serializer.deserialize(invocation));
            } else if (params instanceof List) {
                //集合
                List<?> objects = (List<?>) params;
                int len = objects.size();
                if (len != parameters.length) {
                    throw new CodecException("Invalid Request", "-32600");
                }
                int i = 0;
                for (Object obj : objects) {
                    args[i++] = obj;
                }
                invocation.setArgs(new Object[]{null, null, args});
                invocation.setArgs(serializer.deserialize(invocation));
            } else if (params.getClass().isArray()) {
                int len = Array.getLength(params);
                if (len != parameters.length) {
                    throw new CodecException("Invalid Request", "-32600");
                }
                //数组
                for (int i = 0; i < len; i++) {
                    args[i] = Array.get(params, i);
                }
                invocation.setArgs(new Object[]{null, null, args});
                invocation.setArgs(serializer.deserialize(invocation));
            } else {
                throw new CodecException("Invalid Request", "-32600");
            }
        }
    }

}
