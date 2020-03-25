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
import io.joyrpc.config.InterfaceOption.ProviderMethodOption;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.context.auth.IPPermission;
import io.joyrpc.exception.RpcException;
import io.joyrpc.extension.Extension;
import io.joyrpc.filter.AbstractProviderFilter;
import io.joyrpc.filter.ProviderFilter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.network.Ipv4;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * IP访问控制过滤器
 */
@Extension(value = "ip", order = ProviderFilter.IP_WHITE_BLACK_LIST_ORDER)
public class IPPermissionFilter extends AbstractProviderFilter {

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        ProviderMethodOption option = (ProviderMethodOption) request.getOption();
        IPPermission permission = option.getIPPermission();
        if (permission != null) {
            Invocation invocation = request.getPayLoad();
            InetSocketAddress remoteAddress = request.getRemoteAddress();
            String remoteIp = Ipv4.toIp(remoteAddress);
            if (!permission.permit(invocation.getAlias(), remoteIp)) {
                String errorMsg = String.format(
                        "[%s]Error occurs while processing request %s/%s/%s from channel %s->%s, caused by: Fail to pass the ip blackWhiteList",
                        ExceptionCode.PROVIDER_AUTH_FAIL,
                        invocation.getClassName(), invocation.getMethodName(), invocation.getAlias(),
                        remoteIp, Ipv4.toAddress(request.getLocalAddress()));

                return CompletableFuture.completedFuture(new Result(request.getContext(), new RpcException(errorMsg)));
            }
        }
        return invoker.invoke(request);
    }

    @Override
    public int type() {
        return SYSTEM_GLOBAL;
    }
}
