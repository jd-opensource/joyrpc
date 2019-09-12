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

import io.joyrpc.constants.Constants;
import io.joyrpc.constants.Version;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.context.Environment;
import io.joyrpc.extension.Converts;
import io.joyrpc.protocol.MessageHandler;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.message.Message;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.protocol.message.negotiation.AbstractNegotiation;
import io.joyrpc.protocol.message.negotiation.NegotiationResponse;
import io.joyrpc.transport.session.DefaultSession;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.session.Session;

import java.util.Map;

import static io.joyrpc.Plugin.*;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.context.Environment.*;
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
            response.addAttribute(Constants.JAVA_VERSION_KEY, GlobalContext.getString(Environment.JAVA_VERSION));
            response.addAttribute(BUILD_VERSION_KEY, String.valueOf(Version.BUILD_VERSION));
            response.addAttribute(APPLICATION_ID, GlobalContext.getString(APPLICATION_ID));
            response.addAttribute(APPLICATION_NAME, GlobalContext.getString(APPLICATION_NAME));
            response.addAttribute(APPLICATION_INSTANCE, GlobalContext.getString(APPLICATION_INSTANCE));
            response.addAttribute(APPLICATION_GROUP, GlobalContext.getString(APPLICATION_GROUP));
            response.addAttribute(REMOTE_START_TIMESTAMP, GlobalContext.getString(START_TIME));
        }
        return response;
    }

    @Override
    protected void session(final ChannelContext context, final int sessionId, final AbstractNegotiation negotiation) {
        Map<String, String> attributes = negotiation.getAttributes();
        long timeout = Converts.getLong(attributes.remove(SESSION_TIMEOUT_OPTION.getName()), SESSION_TIMEOUT_OPTION.getValue());
        Session session = new DefaultSession(sessionId, timeout);
        session.setSerialization(SERIALIZATION.get(negotiation.getSerialization()));
        session.setCompression(COMPRESSION.get(negotiation.getCompression()));
        session.setChecksum(CHECKSUM.get(negotiation.getChecksum()));
        session.setSerializations(negotiation.getSerializations());
        session.setCompressions(negotiation.getCompressions());
        session.setChecksums(negotiation.getChecksums());
        session.putAll(attributes);
        context.getChannel().addSession(sessionId, session);
    }

    @Override
    public Integer type() {
        return (int) MsgType.NegotiationReq.getType();
    }
}
