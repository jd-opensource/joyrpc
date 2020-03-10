package io.joyrpc.protocol.joy;

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

import io.joyrpc.constants.Constants;
import io.joyrpc.constants.Version;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.permission.Identification;
import io.joyrpc.protocol.AbstractProtocol;
import io.joyrpc.protocol.ClientProtocol;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.joy.codec.JoyCodec;
import io.joyrpc.protocol.message.MessageHeader;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.protocol.message.authentication.AuthenticationRequest;
import io.joyrpc.protocol.message.negotiation.NegotiationRequest;
import io.joyrpc.protocol.message.negotiation.NegotiationRequest.NegotiationOption;
import io.joyrpc.protocol.message.session.Sessionbeat;
import io.joyrpc.transport.Client;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.session.DefaultSession;
import io.joyrpc.transport.session.Session;

import java.util.Map;

import static io.joyrpc.Plugin.IDENTIFICATION;
import static io.joyrpc.constants.Constants.SESSION_TIMEOUT_OPTION;
import static io.joyrpc.transport.session.Session.REMOTE_START_TIMESTAMP;

/**
 * 客户端协议
 *
 * @date: 2019/3/5
 */
@Extension(value = "joy")
public class JoyClientProtocol extends AbstractProtocol implements ClientProtocol {

    @Override
    protected Codec createCodec() {
        return new JoyCodec(this);
    }

    @Override
    public Message negotiate(final URL clusterUrl, final Client client) {
        //构造协商请求
        NegotiationRequest negotiation = new NegotiationRequest(clusterUrl,
                new NegotiationOption(Constants.SERIALIZATION_OPTION, "msgpack", "json0", "json1"),
                new NegotiationOption(Constants.COMPRESS_OPTION), null);
        //设置client本地session属性
        negotiation.addAttribute(Constants.CONFIG_KEY_INTERFACE, clusterUrl.getPath());
        negotiation.addAttribute(Constants.ALIAS_OPTION.getName(), clusterUrl.getString(Constants.ALIAS_OPTION));
        negotiation.addAttribute(Constants.KEY_JAVA_VERSION, GlobalContext.getString(Constants.KEY_JAVA_VERSION));
        negotiation.addAttribute(Constants.BUILD_VERSION_KEY, String.valueOf(Version.BUILD_VERSION));
        negotiation.addAttribute(Constants.KEY_APPID, GlobalContext.getString(Constants.KEY_APPID));
        negotiation.addAttribute(Constants.KEY_APPNAME, GlobalContext.getString(Constants.KEY_APPNAME));
        negotiation.addAttribute(Constants.KEY_APPINSID, GlobalContext.getString(Constants.KEY_APPINSID));
        negotiation.addAttribute(Constants.KEY_APPGROUP, GlobalContext.getString(Constants.KEY_APPGROUP));
        //做兼容
        negotiation.addAttribute(Constants.JAVA_VERSION_KEY, GlobalContext.getString(Constants.KEY_JAVA_VERSION));
        negotiation.addAttribute(Constants.APPLICATION_ID, GlobalContext.getString(Constants.KEY_APPID));
        negotiation.addAttribute(Constants.APPLICATION_NAME, GlobalContext.getString(Constants.KEY_APPNAME));
        negotiation.addAttribute(Constants.APPLICATION_GROUP, GlobalContext.getString(Constants.KEY_APPGROUP));
        negotiation.addAttribute(Constants.APPLICATION_INSTANCE, GlobalContext.getString(Constants.KEY_APPINSID));
        negotiation.addAttribute(SESSION_TIMEOUT_OPTION.getName(), String.valueOf(clusterUrl.getPositiveLong(SESSION_TIMEOUT_OPTION)));
        negotiation.addAttribute(REMOTE_START_TIMESTAMP, GlobalContext.getString(Constants.KEY_START_TIME));
        //构造协商请求消息
        return new RequestMessage<>(new MessageHeader(MsgType.NegotiationReq.getType()), negotiation);
    }

    @Override
    public Message authenticate(final URL clusterUrl, final Client client) {
        //身份信息
        String type = clusterUrl.getString(Constants.IDENTIFICATION_OPTION);
        Identification identification = IDENTIFICATION.get(type);
        if (identification != null) {
            Map<String, String> result = identification.identity(clusterUrl);
            if (result != null) {
                result.put(Constants.CONFIG_KEY_INTERFACE, clusterUrl.getPath());
                result.put(Constants.KEY_APPID, GlobalContext.getString(Constants.KEY_APPID));
                result.put(Constants.KEY_APPNAME, GlobalContext.getString(Constants.KEY_APPNAME));
                AuthenticationRequest request = new AuthenticationRequest(type, result);
                return new RequestMessage(new MessageHeader(MsgType.AuthenticationReq.getType()), request);
            }
        }
        return null;
    }

    @Override
    public Session session(final URL clusterUrl, final Client client) {
        return new DefaultSession();
    }

    @Override
    public Message heartbeat(final URL clusterUrl, final Client client) {
        return new RequestMessage(new MessageHeader(MsgType.HbReq.getType()));
    }

    @Override
    public Message sessionbeat(final URL clusterUrl, final Client client) {
        return new RequestMessage<>(new MessageHeader(MsgType.SessionbeatReq.getType()), new Sessionbeat());
    }

}
