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
 * 网络异常
 */
public class TransportException extends LafException {

    private static final long serialVersionUID = -7120201445145592600L;

    public TransportException() {
    }

    public TransportException(boolean retry) {
        super(retry);
    }

    public TransportException(String message) {
        super(message);
    }

    public TransportException(String message, boolean retry) {
        super(message, retry);
    }

    public TransportException(String message, String errorCode, boolean retry) {
        super(message, errorCode, retry);
    }

    public TransportException(String message, String errorCode) {
        super(message, errorCode);
    }

    public TransportException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransportException(String message, Throwable cause, boolean retry) {
        super(message, cause, retry);
    }

    public TransportException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }

    public TransportException(String message, Throwable cause, String errorCode, boolean retry) {
        super(message, cause, errorCode, retry);
    }

    public TransportException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, String errorCode, boolean retry) {
        super(message, cause, enableSuppression, writableStackTrace, errorCode, retry);
    }

    public TransportException(Throwable cause) {
        super(cause);
    }

    public TransportException(Throwable cause, boolean retry) {
        super(cause, retry);
    }

    public TransportException(Throwable cause, String errorCode) {
        super(cause, errorCode);
    }

    public TransportException(Throwable cause, String errorCode, boolean retry) {
        super(cause, errorCode, retry);
    }
}
