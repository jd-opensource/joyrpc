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
 * 连接异常
 */
public class ConnectionException extends TransportException {

    private static final long serialVersionUID = 4401440531171871948L;

    public ConnectionException() {
    }

    public ConnectionException(boolean retry) {
        super(retry);
    }

    public ConnectionException(String message) {
        super(message);
    }

    public ConnectionException(String message, boolean retry) {
        super(message, retry);
    }

    public ConnectionException(String message, String errorCode, boolean retry) {
        super(message, errorCode, retry);
    }

    public ConnectionException(String message, String errorCode) {
        super(message, errorCode);
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionException(String message, Throwable cause, boolean retry) {
        super(message, cause, retry);
    }

    public ConnectionException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }

    public ConnectionException(String message, Throwable cause, String errorCode, boolean retry) {
        super(message, cause, errorCode, retry);
    }

    public ConnectionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, String errorCode, boolean retry) {
        super(message, cause, enableSuppression, writableStackTrace, errorCode, retry);
    }

    public ConnectionException(Throwable cause) {
        super(cause);
    }

    public ConnectionException(Throwable cause, boolean retry) {
        super(cause, retry);
    }

    public ConnectionException(Throwable cause, String errorCode) {
        super(cause, errorCode);
    }

    public ConnectionException(Throwable cause, String errorCode, boolean retry) {
        super(cause, errorCode, retry);
    }
}
