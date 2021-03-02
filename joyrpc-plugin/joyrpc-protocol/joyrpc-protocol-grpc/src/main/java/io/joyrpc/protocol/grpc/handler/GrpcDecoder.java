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
import io.joyrpc.codec.UnsafeByteArrayInputStream;
import io.joyrpc.codec.compression.Compression;
import io.joyrpc.codec.serialization.Serialization;
import io.joyrpc.codec.serialization.Serializer;
import io.joyrpc.exception.LafException;
import io.joyrpc.exception.MethodOverloadException;
import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.http.AbstractHttpDecoder;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.util.IDLMethod;
import io.joyrpc.util.IDLMethodDesc;
import io.joyrpc.util.IDLType;

import java.io.IOException;
import java.util.function.Supplier;

import static io.joyrpc.constants.Constants.GRPC_TYPE_FUNCTION;
import static io.joyrpc.util.ClassUtils.getPublicMethod;

/**
 * Grpc调用的构建器
 */
public class GrpcDecoder extends AbstractHttpDecoder {
    /**
     * 方法描述
     */
    protected IDLMethodDesc methodDesc;
    /**
     * 消息ID
     */
    protected long messageId;
    /**
     * 默认的序列化
     */
    protected Serialization serialization;

    @Override
    public GrpcDecoder url(final URL url) {
        return (GrpcDecoder) super.url(url);
    }

    @Override
    public GrpcDecoder paths(final String[] paths) {
        return (GrpcDecoder) super.paths(paths);
    }

    @Override
    public GrpcDecoder header(final Parametric header) {
        return (GrpcDecoder) super.header(header);
    }

    public GrpcDecoder messageId(long messageId) {
        this.messageId = messageId;
        return this;
    }

    public GrpcDecoder serialization(Serialization serialization) {
        this.serialization = serialization;
        return this;
    }

    @Override
    public GrpcDecoder body(final byte[] body) {
        return (GrpcDecoder) super.body(body);
    }

    @Override
    public GrpcDecoder error(final Supplier<LafException> error) {
        return (GrpcDecoder) super.error(error);
    }

    @Override
    protected void parseMethod() throws NoSuchMethodException, MethodOverloadException {
        IDLMethod grpcMethod = getPublicMethod(intfClass, methodName, GRPC_TYPE_FUNCTION);
        method = grpcMethod.getMethod();
        methodDesc = grpcMethod.getType();
        genericMethod = genericClass.get(method);
    }

    @Override
    protected void build(final Invocation invocation) {
        invocation.setIdlMethodDesc(methodDesc);
    }

    @Override
    protected void parseArg(final Invocation invocation) throws Exception {
        //获取接口方法描述
        IDLMethodDesc methodDesc = invocation.getIdlMethodDesc();
        //构造消息输入流
        UnsafeByteArrayInputStream in = new UnsafeByteArrayInputStream(body);
        int compressed = in.read();
        if (in.skip(4) < 4) {
            throw new IOException(String.format("request data is not full. id=%d", messageId));
        }
        Object[] args;
        IDLType wrapper = methodDesc.getRequest();
        //如果方法没有参数，则返回null
        if (wrapper != null) {
            //获取反序列化插件
            Serializer serializer = getSerialization(GrpcUtil.CONTENT_ENCODING, serialization).getSerializer();
            //获取压缩类型
            Compression compression = compressed == 0 ? null : getCompression(GrpcUtil.MESSAGE_ENCODING);
            //反序列化
            Object target = serializer.deserialize(compression == null ? in : compression.decompress(in), wrapper.getClazz());
            //isWrapper为true，为包装对象，遍历每个field，逐个取值赋值给args数组，否则，直接赋值args[0]
            if (wrapper.isWrapper()) {
                args = wrapper.getConversion().getToParameter().apply(target);
            } else {
                args = new Object[]{target};
            }
        } else {
            args = new Object[0];
        }
        invocation.setArgs(args);
    }
}
