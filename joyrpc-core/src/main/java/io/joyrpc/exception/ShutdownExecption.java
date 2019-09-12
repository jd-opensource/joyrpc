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
 * 关机异常
 */
public class ShutdownExecption extends RejectException {

    public ShutdownExecption() {
    }

    public ShutdownExecption(boolean retry) {
        super(retry);
    }

    public ShutdownExecption(String message) {
        super(message);
    }

    public ShutdownExecption(String message, boolean retry) {
        super(message, retry);
    }

    public ShutdownExecption(String message, String errorCode, boolean retry) {
        super(message, errorCode, retry);
    }

    public ShutdownExecption(String message, String errorCode) {
        super(message, errorCode);
    }

    public ShutdownExecption(String message, Throwable cause) {
        super(message, cause);
    }

    public ShutdownExecption(String message, Throwable cause, boolean retry) {
        super(message, cause, retry);
    }

    public ShutdownExecption(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }

    public ShutdownExecption(String message, Throwable cause, String errorCode, boolean retry) {
        super(message, cause, errorCode, retry);
    }

    public ShutdownExecption(Throwable cause) {
        super(cause);
    }

    public ShutdownExecption(Throwable cause, boolean retry) {
        super(cause, retry);
    }

    public ShutdownExecption(Throwable cause, String errorCode) {
        super(cause, errorCode);
    }

    public ShutdownExecption(Throwable cause, String errorCode, boolean retry) {
        super(cause, errorCode, retry);
    }
}
