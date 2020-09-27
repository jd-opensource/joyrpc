package io.joyrpc.protocol.http.handler;

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

import io.joyrpc.protocol.http.HttpResponse;
import io.joyrpc.protocol.http.message.JsonResponseMessage;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.channel.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 结果转换成HTTP应答
 */
public class JoyToHttpHandler implements ChannelHandler {

    protected static final Logger logger = LoggerFactory.getLogger(JoyToHttpHandler.class);

    @Override
    public Object wrote(final ChannelContext context, final Object message) {
        if (message instanceof ResponseMessage) {
            HttpResponse httpResponse = message instanceof HttpResponse ? (HttpResponse) message
                    : new JsonResponseMessage((ResponseMessage<ResponsePayload>) message);
            return httpResponse.apply();
        }
        return message;
    }
}
