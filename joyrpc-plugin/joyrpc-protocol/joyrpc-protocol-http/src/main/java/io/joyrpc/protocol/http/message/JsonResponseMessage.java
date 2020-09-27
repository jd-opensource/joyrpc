package io.joyrpc.protocol.http.message;

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

import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.protocol.message.ResponsePayload;

import java.nio.charset.StandardCharsets;

import static io.joyrpc.Plugin.JSON;

/**
 * 普通JSON应答
 */
public class JsonResponseMessage extends AbstractJsonResponseMessage {

    public static final byte[] EMPTY = "".getBytes();

    public JsonResponseMessage() {
    }

    public JsonResponseMessage(ResponseMessage<ResponsePayload> message) {
        super(message.getHeader(), message.getPayLoad());
    }

    @Override
    protected void render() {
        if (!response.isError()) {
            status = 200;
            Object res = response.getResponse();
            if (res == null) {
                content = EMPTY;
            } else if (res instanceof CharSequence) {
                content = ((CharSequence) res).toString().getBytes(StandardCharsets.UTF_8);
            } else {
                content = JSON.get().toJSONBytes(res);
            }
        } else {
            status = 500;
            content = JSON.get().toJSONBytes(new ErrorResponse(status, response.getException().getMessage()));
        }
    }
}
