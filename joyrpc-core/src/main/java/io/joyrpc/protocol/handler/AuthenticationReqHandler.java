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
import io.joyrpc.invoker.InvokerManager;
import io.joyrpc.permission.Authentication;
import io.joyrpc.permission.Identification;
import io.joyrpc.protocol.MessageHandler;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.message.Message;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.protocol.message.authentication.AuthenticationRequest;
import io.joyrpc.protocol.message.authentication.AuthenticationResponse;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.session.Session;
import io.joyrpc.transport.session.Session.RpcSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.joyrpc.protocol.MsgType.AuthenticationResp;
import static io.joyrpc.protocol.message.authentication.AuthenticationResponse.NOT_PASS;
import static io.joyrpc.protocol.message.authentication.AuthenticationResponse.PASS;

/**
 * 认证处理器
 */
public class AuthenticationReqHandler extends AbstractReqHandler implements MessageHandler {

    private final static Logger logger = LoggerFactory.getLogger(AuthenticationReqHandler.class);

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public void handle(ChannelContext context, Message message) throws HandlerException {
        if (!(message instanceof RequestMessage)) {
            return;
        }
        RequestMessage request = (RequestMessage) message;
        Channel channel = context.getChannel();

        RpcSession session = (RpcSession) request.getSession();
        if (session == null) {
            channel.send(ResponseMessage.build(request, AuthenticationResp.getType(),
                    new AuthenticationResponse(NOT_PASS, "Session is not exists.")), sendFailed);
            return;
        }
        AuthenticationRequest authReq = (AuthenticationRequest) request.getPayLoad();
        //查找exporter，获取其上绑定的认证
        int port = channel.getLocalAddress().getPort();
        String className = session.getInterfaceName();
        String alias = session.getAlias();
        Exporter exporter = InvokerManager.getExporter(className, alias, port);
        if (exporter == null) {
            //抛出异常
            channel.send(ResponseMessage.build(request, AuthenticationResp.getType(),
                    new AuthenticationResponse(NOT_PASS,
                            String.format("exporter is not exists. class=%s,alias=%s,port=%d", className, alias, port))),
                    sendFailed);
            return;
        }
        //判断认证算法是否一致
        Identification identification = exporter.getIdentification();
        Authentication authenticator = exporter.getAuthentication();
        if (identification == null || authenticator == null) {
            //没有配置认证
            session.setAuthenticated(Session.AUTH_SESSION_SUCCESS);
            channel.send(ResponseMessage.build(request, AuthenticationResp.getType(), new AuthenticationResponse(PASS)), sendFailed);
        } else if (!identification.type().equals(authReq.getType())) {
            //身份类型不对
            channel.send(ResponseMessage.build(request, AuthenticationResp.getType(),
                    new AuthenticationResponse(NOT_PASS,
                            String.format("identification must be %s. class=%s,alias=%s,port=%d",
                                    identification.type(), className, alias, port))),
                    sendFailed);
        } else {
            try {
                AuthenticationResponse response = authenticator.authenticate(authReq);
                session.setAuthenticated(response.isSuccess() ? Session.AUTH_SESSION_SUCCESS : Session.AUTH_SESSION_FAIL);
                channel.send(ResponseMessage.build(request, AuthenticationResp.getType(), response), sendFailed);
            } catch (Exception e) {
                channel.send(ResponseMessage.build(request, AuthenticationResp.getType(),
                        new AuthenticationResponse(NOT_PASS, e.getMessage())), sendFailed);
            }
        }

    }

    @Override
    public Integer type() {
        return (int) MsgType.AuthenticationReq.getType();
    }
}
