package io.joyrpc.protocol.http.controller;

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

import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.http.HeaderMapping;
import io.joyrpc.protocol.http.HttpController;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.http.HttpRequestMessage;

import java.util.List;

import static io.joyrpc.protocol.http.HeaderMapping.KEEP_ALIVE;

/**
 * 处理favicon.ico
 */
@Extension("/favicon.ico")
public class FaviconController implements HttpController {

    @Override
    public Object execute(final ChannelContext ctx, final HttpRequestMessage message, final URL url, final List<String> params) {
        ResponseMessage<ResponsePayload> response = new ResponseMessage<>();
        response.getHeader()
                .addAttribute(HeaderMapping.CONTENT_TYPE.getNum(), "text/html; charset=UTF-8")
                .addAttribute(KEEP_ALIVE.getNum(), message.headers().isKeepAlive());
        response.setPayLoad(new ResponsePayload(null));
        ctx.getChannel().send(response);
        return null;
    }
}
