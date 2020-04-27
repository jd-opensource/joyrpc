package io.joyrpc.exception;

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

import io.joyrpc.transport.message.Header;

/**
 * Rpc异常
 */
public class RpcException extends LafException {

    private static final long serialVersionUID = 3269562091618562124L;

    protected transient Header header;

    public RpcException() {
    }

    public RpcException(String message) {
        super(message);
    }

    public RpcException(String message, boolean retry) {
        super(message, retry);
    }

    public RpcException(String message, String errorCode, boolean retry) {
        super(message, errorCode, retry);
    }

    public RpcException(String message, String errorCode) {
        super(message, errorCode);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcException(String message, Throwable cause, boolean retry) {
        super(message, cause, retry);
    }

    public RpcException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }

    public RpcException(String message, Throwable cause, String errorCode, boolean retry) {
        super(message, cause, errorCode, retry);
    }

    public RpcException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, String errorCode, boolean retry) {
        super(message, cause, enableSuppression, writableStackTrace, errorCode, retry);
    }

    public RpcException(Throwable cause) {
        super(cause);
    }

    public RpcException(Throwable cause, boolean retry) {
        super(cause, retry);
    }

    public RpcException(Throwable cause, String errorCode) {
        super(cause, errorCode);
    }

    public RpcException(Throwable cause, String errorCode, boolean retry) {
        super(cause, errorCode, retry);
    }

    public RpcException(Header header, String message) {
        super(message);
        this.header = header;
    }

    public RpcException(Header header, String message, String errorCode) {
        super(message, errorCode);
        this.header = header;
    }

    public RpcException(Header header, Throwable cause) {
        super(cause);
        this.header = header;
    }

    public RpcException(Header header, String message, boolean retry) {
        super(message, retry);
        this.header = header;
    }

    public RpcException(Header header, String message, String errorCode, boolean retry) {
        super(message, errorCode, retry);
        this.header = header;
    }

    public RpcException(Header header, Throwable cause, boolean retry) {
        super(cause, retry);
        this.header = header;
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }
}
