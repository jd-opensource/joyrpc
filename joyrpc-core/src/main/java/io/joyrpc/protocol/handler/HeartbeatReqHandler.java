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
import io.joyrpc.health.HealthProbe;
import io.joyrpc.protocol.MessageHandler;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.message.Message;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.protocol.message.heartbeat.DefaultHeartbeatResponse;
import io.joyrpc.transport.channel.ChannelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @date: 2019/3/14
 */
public class HeartbeatReqHandler extends AbstractReqHandler implements MessageHandler {

    private final static Logger logger = LoggerFactory.getLogger(HeartbeatReqHandler.class);

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public void handle(final ChannelContext context, final Message message) throws HandlerException {
        //支持插件进行判断
        ResponseMessage response = ResponseMessage.build(message, MsgType.HbResp.getType(),
                new DefaultHeartbeatResponse(HealthProbe.getInstance().getState()));
        context.getChannel().send(response, sendFailed);
        context.end();
    }

    @Override
    public Integer type() {
        return (int) MsgType.HbReq.getType();
    }
}
