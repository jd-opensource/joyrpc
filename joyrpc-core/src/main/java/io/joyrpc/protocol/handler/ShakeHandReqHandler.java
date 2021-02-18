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
import io.joyrpc.protocol.MessageHandler;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.message.Message;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.transport.channel.ChannelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 握手协议处理器
 */
public class ShakeHandReqHandler implements MessageHandler {

    private final static Logger logger = LoggerFactory.getLogger(ShakeHandReqHandler.class);

    @Override
    public void handle(ChannelContext context, Message message) throws HandlerException {
        ResponseMessage response = new ResponseMessage(MsgType.ShakeHandResp.getType(), message.getMsgId());
        context.getChannel().send(response, (event) -> {
            if (!event.isSuccess()) {
                logger.error("Shake hand response error, message : {}", message.toString());
            }
        });
    }

    @Override
    public Integer type() {
        return (int) MsgType.ShakeHandReq.getType();
    }
}
