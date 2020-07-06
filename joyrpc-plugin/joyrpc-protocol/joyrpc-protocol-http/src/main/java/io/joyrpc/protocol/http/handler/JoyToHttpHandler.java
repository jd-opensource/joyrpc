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

import io.joyrpc.protocol.AbstractHttpHandler;
import io.joyrpc.protocol.http.message.ErrorResponse;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.http.DefaultHttpResponseMessage;
import io.joyrpc.transport.http.HttpHeaders;
import io.joyrpc.transport.http.HttpHeaders.Names;
import io.joyrpc.transport.http.HttpResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.joyrpc.Plugin.JSON;
import static io.joyrpc.protocol.http.HeaderMapping.ACCEPT_ENCODING;
import static io.joyrpc.protocol.http.HeaderMapping.KEEP_ALIVE;
import static io.joyrpc.transport.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.joyrpc.transport.http.HttpHeaders.Names.CONTENT_TYPE;

/**
 * 结果转换成HTTP应答
 */
public class JoyToHttpHandler extends AbstractHttpHandler {

    protected static final Logger logger = LoggerFactory.getLogger(JoyToHttpHandler.class);

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public Object wrote(final ChannelContext context, final Object message) {
        if (message instanceof ResponseMessage) {
            return convert((ResponseMessage<ResponsePayload>) message);
        }
        return message;
    }

    /**
     * 进行协议转换
     *
     * @param message
     * @return
     */
    protected HttpResponseMessage convert(final ResponseMessage<ResponsePayload> message) {
        byte[] content;
        int status;
        ResponsePayload responsePayload = message.getPayLoad();
        if (!responsePayload.isError()) {
            status = 200;
            content = JSON.get().toJSONBytes(responsePayload.getResponse());
        } else {
            status = 500;
            content = JSON.get().toJSONBytes(new ErrorResponse(status, responsePayload.getException().getMessage()));
        }
        HttpResponseMessage result = new DefaultHttpResponseMessage();
        result.setStatus(status);
        HttpHeaders headers = result.headers();
        headers.set(CONTENT_TYPE, "text/json; charset=UTF-8");
        content = compress(content, message, ACCEPT_ENCODING.getNum(), encoding -> headers.set(Names.CONTENT_ENCODING, encoding));
        result.setContent(content);
        headers.set(CONTENT_LENGTH, content.length);
        headers.setKeepAlive(Boolean.TRUE.equals(message.getHeader().getAttribute(KEEP_ALIVE.getNum())));
        return result;
    }

}
