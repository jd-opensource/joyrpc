package io.joyrpc.filter.provider;

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

import io.joyrpc.Invoker;
import io.joyrpc.Result;
import io.joyrpc.codec.serialization.GenericSerializer;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.context.RequestContext;
import io.joyrpc.exception.CodecException;
import io.joyrpc.exception.GenericException;
import io.joyrpc.exception.LafException;
import io.joyrpc.exception.RpcException;
import io.joyrpc.extension.Extension;
import io.joyrpc.filter.AbstractProviderFilter;
import io.joyrpc.filter.ProviderFilter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.network.Ipv4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.Plugin.GENERIC_SERIALIZER;
import static io.joyrpc.codec.serialization.GenericSerializer.STANDARD;
import static io.joyrpc.constants.Constants.GENERIC_OPTION;

/**
 * @description: 服务端的泛化调用过滤器<br>
 * 如果是generic请求，那么可能传递的参数值和参数类型不匹配 需要转换<br>
 */
@Extension(value = "generic", order = ProviderFilter.GENERIC_ORDER)
public class GenericFilter extends AbstractProviderFilter {

    private final static Logger logger = LoggerFactory.getLogger(GenericFilter.class);

    /**
     * 默认泛化处理器
     */
    protected final GenericSerializer defSerializer = GENERIC_SERIALIZER.get(STANDARD);

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        Invocation invocation = request.getPayLoad();

        if (!invocation.isGeneric()) {
            // 如果不是generic请求，直接执行下一filter
            return invoker.invoke(request);
        }

        CompletableFuture<Result> future = null;
        GenericSerializer[] serializers = new GenericSerializer[1];
        //把泛化调用进行标准化
        try {
            String type = invocation.getAttachment(GenericSerializer.GENERIC_SERIALIZER);
            if (type != null && !type.isEmpty()) {
                serializers[0] = GENERIC_SERIALIZER.get(type);
                if (serializers[0] == null) {
                    throw new CodecException("The generic arguments serialization is not found. " + type);
                }
            } else {
                serializers[0] = defSerializer;
            }
            // 根据调用的参数来获取方法及参数类型
            invocation.setArgs(serializers[0].deserialize(invocation));
        } catch (Exception e) {
            String message = String.format(ExceptionCode.format(ExceptionCode.FILTER_GENERIC_CONVERT) +
                            " Error occurs while processing request %s/%s/%s from channel %s->%s, caused by: %s",
                    invocation.getClassName(), invocation.getMethodName(), invocation.getAlias(),
                    Ipv4.toIp(request.getRemoteAddress()),
                    Ipv4.toIp(request.getLocalAddress()),
                    e.getMessage());
            //转换出错
            logger.error(message, e);
            future = CompletableFuture.completedFuture(new Result(request.getContext(), new RpcException(message, e)));
        }
        // 解析完毕后，将invocation从generic换成正常invocatio，往下调用
        if (future == null) {
            future = invoker.invoke(request);
        }
        return future.thenApply(r -> {
            // 有异常
            if (r.isException()) {
                Throwable err = r.getException();
                if (!(err instanceof LafException)) {
                    //非joy rpc异常，可能调用方没有此异常类，这里包装异常
                    r.setException(new GenericException(err.getMessage(), err));
                }
                return r;
            } else {
                // 无异常
                return new Result(request.getContext(), serializers[0].serialize(r.getValue()));
            }
        });

    }

    @Override
    public int type() {
        return INNER;
    }
}
