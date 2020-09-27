package io.joyrpc.protocol.jsonrpc.exception;

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

import io.joyrpc.exception.CodecException;

/**
 * 内部捕获异常使用
 */
public class JsonRpcCodecException extends CodecException {

    protected Long id;

    public JsonRpcCodecException(Long id) {
        this.id = id;
    }

    public JsonRpcCodecException(String message, String errorCode, Long id) {
        super(message, errorCode);
        this.id = id;
    }

    public JsonRpcCodecException(String message, Throwable cause, String errorCode, Long id) {
        super(message, cause, errorCode);
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
