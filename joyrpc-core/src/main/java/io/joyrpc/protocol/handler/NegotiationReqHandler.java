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

import io.joyrpc.Invoker;
import io.joyrpc.constants.Version;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.Converts;
import io.joyrpc.invoker.Exporter;
import io.joyrpc.invoker.InvokerManager;
import io.joyrpc.protocol.MessageHandler;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.ServerProtocol;
import io.joyrpc.protocol.message.Message;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.protocol.message.negotiation.AbstractNegotiation;
import io.joyrpc.protocol.message.negotiation.NegotiationResponse;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.session.DefaultSession;
import io.joyrpc.transport.session.Session;
import io.joyrpc.transport.transport.ChannelTransport;

import java.net.InetSocketAddress;
import java.util.Map;

import static io.joyrpc.Plugin.*;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.transport.session.Session.REMOTE_START_TIMESTAMP;

/**
 * @date: 2019/3/18
 */
public class NegotiationReqHandler extends AbstractNegotiationHandler<Message> implements MessageHandler {

    @Override
    protected Message createResponseMessage(final Message request, final NegotiationResponse negotiation) {
        return ResponseMessage.build(request, MsgType.NegotiationResp.getType(), negotiation);
    }

    @Override
    protected NegotiationResponse negotiate(final AbstractNegotiation negotiation) {
        NegotiationResponse response = super.negotiate(negotiation);
        if (response.isSuccess()) {
            Map<String, String> attributes = negotiation.getAttributes();
            response.addAttribute(CONFIG_KEY_INTERFACE, attributes.get(CONFIG_KEY_INTERFACE));
            response.addAttribute(ALIAS_OPTION.getName(), attributes.get(ALIAS_OPTION.getName()));
            response.addAttribute(KEY_JAVA_VERSION, GlobalContext.getString(KEY_JAVA_VERSION));
            response.addAttribute(BUILD_VERSION_KEY, String.valueOf(Version.BUILD_VERSION));
            response.addAttribute(KEY_APPID, GlobalContext.getString(KEY_APPID));
            response.addAttribute(KEY_APPNAME, GlobalContext.getString(KEY_APPNAME));
            response.addAttribute(KEY_APPINSID, GlobalContext.getString(KEY_APPINSID));
            response.addAttribute(KEY_APPGROUP, GlobalContext.getString(KEY_APPGROUP));
            response.addAttribute(REMOTE_START_TIMESTAMP, GlobalContext.getString(KEY_START_TIME));
            //兼容
            response.addAttribute(JAVA_VERSION_KEY, GlobalContext.getString(KEY_JAVA_VERSION));
            response.addAttribute(APPLICATION_ID, GlobalContext.getString(KEY_APPID));
            response.addAttribute(APPLICATION_NAME, GlobalContext.getString(KEY_APPNAME));
            response.addAttribute(APPLICATION_INSTANCE, GlobalContext.getString(KEY_APPINSID));
            response.addAttribute(APPLICATION_GROUP, GlobalContext.getString(KEY_APPGROUP));
        }
        return response;
    }

    @Override
    protected void session(final ChannelContext context, final int sessionId, final AbstractNegotiation negotiation) {
        Map<String, String> attributes = negotiation.getAttributes();
        long timeout = Converts.getLong(attributes.remove(SESSION_TIMEOUT_OPTION.getName()), SESSION_TIMEOUT_OPTION.getValue());
        ProviderSession session = new ProviderSession(sessionId, timeout);
        Channel channel = context.getChannel();
        session.setProtocol(channel.getAttribute(Channel.PROTOCOL));
        session.setLocalAddress(channel.getLocalAddress());
        session.setRemoteAddress(channel.getRemoteAddress());
        session.setTransport(channel.getAttribute(Channel.CHANNEL_TRANSPORT));
        session.setSerialization(SERIALIZATION.get(negotiation.getSerialization()));
        session.setCompression(COMPRESSION.get(negotiation.getCompression()));
        session.setChecksum(CHECKSUM.get(negotiation.getChecksum()));
        session.setSerializations(negotiation.getSerializations());
        session.setCompressions(negotiation.getCompressions());
        session.setChecksums(negotiation.getChecksums());
        session.putAll(attributes);
        //提前绑定Exporter
        session.setExporter(InvokerManager.getExporter(session.getInterfaceName(), session.getAlias(),
                session.localAddress.getPort()));
        channel.addSession(sessionId, session);
    }

    @Override
    public Integer type() {
        return (int) MsgType.NegotiationReq.getType();
    }

    /**
     * 服务端会话
     */
    protected static class ProviderSession extends DefaultSession implements Session.ServerSession {
        /**
         * 服务端输出
         */
        protected Exporter exporter;
        /**
         * 远程地址
         */
        protected InetSocketAddress remoteAddress;
        /**
         * 本地地址
         */
        protected InetSocketAddress localAddress;
        /**
         * 通道
         */
        protected ChannelTransport transport;
        /**
         * 服务端协议
         */
        protected ServerProtocol protocol;

        public ProviderSession(int sessionId, long timeout) {
            super(sessionId, timeout);
        }

        @Override
        public Invoker getProvider() {
            return exporter;
        }

        public Exporter getExporter() {
            return exporter;
        }

        public void setExporter(Exporter exporter) {
            this.exporter = exporter;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return remoteAddress;
        }

        public void setRemoteAddress(InetSocketAddress remoteAddress) {
            this.remoteAddress = remoteAddress;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return localAddress;
        }

        public void setLocalAddress(InetSocketAddress localAddress) {
            this.localAddress = localAddress;
        }

        @Override
        public ChannelTransport getTransport() {
            return transport;
        }

        public void setTransport(ChannelTransport transport) {
            this.transport = transport;
        }

        @Override
        public ServerProtocol getProtocol() {
            return protocol;
        }

        public void setProtocol(ServerProtocol protocol) {
            this.protocol = protocol;
        }
    }
}
