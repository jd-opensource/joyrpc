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

/**
 * 创建SSL连接异常
 */
public class SslException extends ConnectionException {

    public SslException() {
    }

    public SslException(boolean retry) {
        super(retry);
    }

    public SslException(String message) {
        super(message);
    }

    public SslException(String message, boolean retry) {
        super(message, retry);
    }

    public SslException(String message, String errorCode, boolean retry) {
        super(message, errorCode, retry);
    }

    public SslException(String message, String errorCode) {
        super(message, errorCode);
    }

    public SslException(String message, Throwable cause) {
        super(message, cause);
    }

    public SslException(String message, Throwable cause, boolean retry) {
        super(message, cause, retry);
    }

    public SslException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }

    public SslException(String message, Throwable cause, String errorCode, boolean retry) {
        super(message, cause, errorCode, retry);
    }

    public SslException(Throwable cause) {
        super(cause);
    }

    public SslException(Throwable cause, boolean retry) {
        super(cause, retry);
    }

    public SslException(Throwable cause, String errorCode) {
        super(cause, errorCode);
    }

    public SslException(Throwable cause, String errorCode, boolean retry) {
        super(cause, errorCode, retry);
    }
}
