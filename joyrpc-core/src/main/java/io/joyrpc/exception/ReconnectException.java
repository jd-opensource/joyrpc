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
 * 重连异常，超过了重试次数
 */
public class ReconnectException extends ConnectionException {

    public ReconnectException() {
    }

    public ReconnectException(boolean retry) {
        super(retry);
    }

    public ReconnectException(String message) {
        super(message);
    }

    public ReconnectException(String message, boolean retry) {
        super(message, retry);
    }

    public ReconnectException(String message, String errorCode, boolean retry) {
        super(message, errorCode, retry);
    }

    public ReconnectException(String message, String errorCode) {
        super(message, errorCode);
    }

    public ReconnectException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReconnectException(String message, Throwable cause, boolean retry) {
        super(message, cause, retry);
    }

    public ReconnectException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }

    public ReconnectException(String message, Throwable cause, String errorCode, boolean retry) {
        super(message, cause, errorCode, retry);
    }

    public ReconnectException(Throwable cause) {
        super(cause);
    }

    public ReconnectException(Throwable cause, boolean retry) {
        super(cause, retry);
    }

    public ReconnectException(Throwable cause, String errorCode) {
        super(cause, errorCode);
    }

    public ReconnectException(Throwable cause, String errorCode, boolean retry) {
        super(cause, errorCode, retry);
    }
}
