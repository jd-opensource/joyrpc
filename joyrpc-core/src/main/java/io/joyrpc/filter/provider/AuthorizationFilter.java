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
import io.joyrpc.constants.Constants;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.exception.AuthenticationException;
import io.joyrpc.exception.AuthorizationException;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.URL;
import io.joyrpc.filter.AbstractProviderFilter;
import io.joyrpc.filter.ProviderFilter;
import io.joyrpc.permission.Authentication;
import io.joyrpc.permission.Authorization;
import io.joyrpc.permission.Identification;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.protocol.message.authentication.AuthenticationRequest;
import io.joyrpc.transport.session.Session;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static io.joyrpc.transport.session.Session.*;

/**
 * 方法鉴权<br>
 *
 * @description: 请求携带方法token，用于判断是否有权限调用</br>
 */
@Extension(value = "authorization", order = ProviderFilter.AUTHORIZATION_ODER)
public class AuthorizationFilter extends AbstractProviderFilter {

    protected static final String ERROR = "%s! Invocation of %s.%s from consumer %s to %s.";

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        Function<Session, Integer> authenticated = request.getAuthenticated();
        Invocation invocation = request.getPayLoad();
        RpcSession session = (RpcSession) request.getSession();
        //判断会话是否支持认证
        int sessionAuth = authenticated != null ? authenticated.apply(session) : AUTH_SESSION_NONE;
        if (sessionAuth == AUTH_SESSION_NONE) {
            //认证
            Identification identification = request.getIdentification();
            Authentication authentication = request.getAuthentication();
            if (identification == null || authentication == null) {
                sessionAuth = AUTH_SESSION_SUCCESS;
            } else {
                try {
                    Map<String, String> identity = identification.identity(new MapParametric(invocation.getAttachments()));
                    AuthenticationRequest authRequest = new AuthenticationRequest(identification.type(), identity);
                    authRequest.addAttribute(Constants.CONFIG_KEY_INTERFACE, invocation.getClassName());
                    authRequest.addAttribute(Constants.KEY_APPID, invocation.getAttachment(Constants.HIDDEN_KEY_APPID));
                    authRequest.addAttribute(Constants.KEY_APPNAME, invocation.getAttachment(Constants.HIDDEN_KEY_APPNAME));
                    authRequest.addAttribute(Constants.KEY_APPNAME, session.getRemoteAppName());
                    authRequest.addAttribute(Constants.KEY_APPID, session.getRemoteAppId());
                    authRequest.addAttribute(Constants.KEY_APPGROUP, session.getRemoteAppGroup());

                    sessionAuth = authentication.authenticate(authRequest).isSuccess() ? AUTH_SESSION_SUCCESS : AUTH_SESSION_FAIL;
                    ;
                } catch (Throwable e) {
                    return CompletableFuture.completedFuture(new Result(request.getContext(),
                            new AuthenticationException(
                                    String.format(ERROR, "Error occurs while authenticating.",
                                            invocation.getClassName(), invocation.getMethodName(),
                                            request.getRemoteAddress(), request.getLocalAddress()),
                                    e, ExceptionCode.PROVIDER_AUTH_FAIL)));
                }
            }
        }
        if (sessionAuth != AUTH_SESSION_SUCCESS) {
            return CompletableFuture.completedFuture(new Result(request.getContext(),
                    new AuthenticationException(
                            String.format(ERROR, "Authentication is not passed", invocation.getClassName(),
                                    invocation.getMethodName(), request.getRemoteAddress(), request.getLocalAddress()),
                            ExceptionCode.PROVIDER_AUTH_FAIL)));
        }
        Authorization authorization = request.getAuthorization();
        //细粒度鉴权
        if (authorization != null && !authorization.authenticate(request)) {
            //鉴权没有通过
            return CompletableFuture.completedFuture(new Result(request.getContext(),
                    new AuthorizationException(
                            String.format(ERROR, "No authorization", invocation.getClassName(), invocation.getMethodName(),
                                    request.getRemoteAddress(), request.getLocalAddress()),
                            ExceptionCode.PROVIDER_AUTH_FAIL, null)));
        }
        return invoker.invoke(request);
    }

    @Override
    public boolean test(final URL url) {
        return true;
    }

    @Override
    public int type() {
        return SYSTEM_GLOBAL;
    }
}
