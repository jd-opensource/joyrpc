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
 * Failover失败，达到最大重试次数
 */
public class FailoverException extends RpcException {

    public FailoverException() {
    }

    public FailoverException(String message) {
        super(message);
    }

    public FailoverException(String message, boolean retry) {
        super(message, retry);
    }

    public FailoverException(String message, String errorCode, boolean retry) {
        super(message, errorCode, retry);
    }

    public FailoverException(String message, String errorCode) {
        super(message, errorCode);
    }

    public FailoverException(String message, Throwable cause) {
        super(message, cause);
    }

    public FailoverException(String message, Throwable cause, boolean retry) {
        super(message, cause, retry);
    }

    public FailoverException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }

    public FailoverException(String message, Throwable cause, String errorCode, boolean retry) {
        super(message, cause, errorCode, retry);
    }

    public FailoverException(Throwable cause) {
        super(cause);
    }

    public FailoverException(Throwable cause, boolean retry) {
        super(cause, retry);
    }

    public FailoverException(Throwable cause, String errorCode) {
        super(cause, errorCode);
    }

    public FailoverException(Throwable cause, String errorCode, boolean retry) {
        super(cause, errorCode, retry);
    }

    public FailoverException(Header header, String message) {
        super(header, message);
    }

    public FailoverException(Header header, String message, String errorCode) {
        super(header, message, errorCode);
    }

    public FailoverException(Header header, Throwable cause) {
        super(header, cause);
    }

    public FailoverException(Header header, String message, boolean retry) {
        super(header, message, retry);
    }

    public FailoverException(Header header, String message, String errorCode, boolean retry) {
        super(header, message, errorCode, retry);
    }

    public FailoverException(Header header, Throwable cause, boolean retry) {
        super(header, cause, retry);
    }
}
