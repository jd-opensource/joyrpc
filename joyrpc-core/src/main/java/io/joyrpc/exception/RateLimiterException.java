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
 * 限流异常
 */
public class RateLimiterException extends RejectException {

    private static final long serialVersionUID = 8641808114658145236L;

    public RateLimiterException() {
        super(null, null, false, false, null, false);
    }

    public RateLimiterException(String message) {
        super(message, null, false, false, null, false);
    }

    public RateLimiterException(String message, String errorCode) {
        super(message, null, false, false, errorCode, false);
    }

    public RateLimiterException(String message, Throwable cause) {
        super(message, cause, false, false, null, false);
    }

    public RateLimiterException(String message, Throwable cause, String errorCode) {
        super(message, cause, false, false, errorCode, false);
    }

    public RateLimiterException(Throwable cause) {
        super(cause == null ? null : cause.toString(), cause, false, false, null, false);
    }

    public RateLimiterException(Throwable cause, String errorCode) {
        super(cause == null ? null : cause.toString(), cause, false, false, errorCode, false);
    }
}
