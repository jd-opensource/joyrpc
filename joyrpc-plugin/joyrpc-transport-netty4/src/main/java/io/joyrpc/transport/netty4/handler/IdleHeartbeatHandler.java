package io.joyrpc.transport.netty4.handler;

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

import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.heartbeat.HeartbeatTrigger;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.Optional;

/**
 * @date: 2019/3/1
 */
public class IdleHeartbeatHandler extends ChannelDuplexHandler {

    private HeartbeatTrigger heartbeatTrigger;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            Optional.ofNullable(getHeartbeatTrigger(ctx))
                    .ifPresent(
                            HeartbeatTrigger::trigger);
        } else {
            super.userEventTriggered(ctx, evt);
        }

    }

    private HeartbeatTrigger getHeartbeatTrigger(ChannelHandlerContext ctx) {
        if (heartbeatTrigger == null) {
            Attribute<HeartbeatTrigger> attr =
                    ctx.channel().attr(AttributeKey.valueOf(Channel.IDLE_HEARTBEAT_TRIGGER));
            heartbeatTrigger = attr != null ? attr.get() : null;
        }
        return heartbeatTrigger;
    }
}
