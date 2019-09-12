package io.joyrpc.transport.netty4.test.http;

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

import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.channel.ChannelHandler;
import io.joyrpc.transport.http.DefaultHttpResponseMessage;
import io.joyrpc.transport.http.HttpRequestMessage;
import io.joyrpc.transport.http.HttpResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @date: 2019/1/28
 */
public class MockHttpChannelHandler implements ChannelHandler {

    private static final Logger logger = LoggerFactory.getLogger(MockHttpChannelHandler.class);

    @Override
    public Object received(ChannelContext context, Object message) {
        if (message instanceof HttpRequestMessage) {
            HttpResponseMessage responseMessage = new DefaultHttpResponseMessage();
            responseMessage.headers().set("Content-Type", "text/html; charset=UTF-8");
            responseMessage.headers().setKeepAlive(((HttpRequestMessage) message).headers().isKeepAlive());
            responseMessage.setStatus(200);
            responseMessage.setContent("response message".getBytes());
            context.getChannel().send(responseMessage);
        }
        return message;
    }

    @Override
    public Object wrote(ChannelContext context, Object message) {
        return message;
    }

    @Override
    public void caught(ChannelContext context, Throwable throwable) {
        logger.error(throwable.getMessage(), throwable);
    }
}
