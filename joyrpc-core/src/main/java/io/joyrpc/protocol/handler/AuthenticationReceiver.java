package io.joyrpc.protocol.handler;

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

import io.joyrpc.exception.HandlerException;
import io.joyrpc.invoker.Exporter;
import io.joyrpc.invoker.ServiceManager;
import io.joyrpc.permission.Authentication;
import io.joyrpc.permission.Identification;
import io.joyrpc.protocol.MessageHandler;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.message.Message;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.protocol.message.authentication.AuthenticationRequest;
import io.joyrpc.protocol.message.authentication.AuthenticationResponse;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.session.Session;
import io.joyrpc.transport.session.Session.RpcSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.joyrpc.protocol.MsgType.AuthenticationResp;
import static io.joyrpc.protocol.message.ResponseMessage.build;
import static io.joyrpc.protocol.message.authentication.AuthenticationResponse.NOT_PASS;
import static io.joyrpc.protocol.message.authentication.AuthenticationResponse.PASS;

/**
 * 认证请求处理器
 */
public class AuthenticationReceiver extends AbstractReceiver implements MessageHandler {

    private final static Logger logger = LoggerFactory.getLogger(AuthenticationReceiver.class);

    @Override
    public void handle(final ChannelContext context, final Message message) throws HandlerException {
        if (!(message instanceof RequestMessage)) {
            return;
        }
        RequestMessage request = (RequestMessage) message;
        Channel channel = context.getChannel();

        RpcSession session = (RpcSession) request.getSession();
        if (session == null) {
            acknowledge(context, request, "Session is not exists.");
            return;
        }
        AuthenticationRequest authReq = (AuthenticationRequest) request.getPayLoad();
        //查找exporter，获取其上绑定的认证
        int port = channel.getLocalAddress().getPort();
        String className = session.getInterfaceName();
        String alias = session.getAlias();
        Exporter exporter = ServiceManager.getExporter(className, alias, port);
        if (exporter == null) {
            //抛出异常
            acknowledge(context, request,
                    String.format("exporter is not exists. class=%s,alias=%s,port=%d",
                            className, alias, port));
            return;
        }
        //判断认证算法是否一致
        Identification identification = exporter.getIdentification();
        Authentication authenticator = exporter.getAuthentication();
        if (identification == null || authenticator == null) {
            //没有配置认证
            session.setAuthenticated(Session.AUTH_SESSION_SUCCESS);
            acknowledge(context, request, new AuthenticationResponse(PASS));
        } else if (!identification.type().equals(authReq.getType())) {
            //身份类型不对
            acknowledge(context, request,
                    String.format("identification must be %s. class=%s,alias=%s,port=%d",
                            identification.type(), className, alias, port));
        } else {
            try {
                AuthenticationResponse response = authenticator.authenticate(authReq);
                session.setAuthenticated(response.isSuccess() ? Session.AUTH_SESSION_SUCCESS : Session.AUTH_SESSION_FAIL);
                acknowledge(context, request, response);
            } catch (Exception e) {
                acknowledge(context, request, e.getMessage());
            }
        }

    }

    /**
     * 应答消息
     *
     * @param context  连接通道
     * @param request  请求
     * @param response 应答
     */
    protected void acknowledge(final ChannelContext context, final RequestMessage request, final AuthenticationResponse response) {
        acknowledge(context, request, build(request, AuthenticationResp.getType(), response), logger);
    }

    /**
     * 应答消息
     *
     * @param context 连接通道
     * @param request 请求
     * @param error   错误
     */
    protected void acknowledge(final ChannelContext context, final RequestMessage request, final String error) {
        acknowledge(context, request, new AuthenticationResponse(NOT_PASS, error));
    }

    @Override
    public Integer type() {
        return (int) MsgType.AuthenticationReq.getType();
    }
}
