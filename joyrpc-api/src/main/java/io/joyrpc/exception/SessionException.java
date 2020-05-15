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
 * 会话异常
 */
public class SessionException extends RejectException {

    public SessionException() {
        super(null, null, false, false, null, true);
    }

    public SessionException(boolean retry) {
        super(null, null, false, false, null, retry);
    }

    public SessionException(String message) {
        super(message, null, false, false, null, true);
    }

    public SessionException(String message, boolean retry) {
        super(message, null, false, false, null, retry);
    }

    public SessionException(String message, String errorCode, boolean retry) {
        super(message, null, false, false, errorCode, retry);
    }

    public SessionException(String message, String errorCode) {
        super(message, null, false, false, errorCode, true);
    }
}
