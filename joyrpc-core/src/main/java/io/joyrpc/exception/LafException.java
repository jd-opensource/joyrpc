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
 * 异常基类
 */
public class LafException extends RuntimeException {

    private static final long serialVersionUID = 5854006923128012364L;
    /**
     * 错误码
     */
    protected String errorCode;

    protected Boolean retry = false;

    public LafException() {
    }

    public LafException(boolean retry) {
        this.retry = retry;
    }

    public LafException(String message) {
        super(message);
    }

    public LafException(String message, boolean retry) {
        super(message);
        this.retry = retry;
    }

    public LafException(String message, String errorCode, boolean retry) {
        super(message);
        this.errorCode = errorCode;
        this.retry = retry;
    }

    public LafException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public LafException(String message, Throwable cause) {
        super(message, cause);
    }

    public LafException(String message, Throwable cause, boolean retry) {
        super(message, cause);
        this.retry = retry;
    }

    public LafException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public LafException(String message, Throwable cause, String errorCode, boolean retry) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retry = retry;
    }

    public LafException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, String errorCode, boolean retry) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = errorCode;
        this.retry = retry;
    }

    public LafException(Throwable cause) {
        super(cause);
    }

    public LafException(Throwable cause, boolean retry) {
        super(cause);
        this.retry = retry;
    }

    public LafException(Throwable cause, String errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public LafException(Throwable cause, String errorCode, boolean retry) {
        super(cause);
        this.errorCode = errorCode;
        this.retry = retry;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * 是否可重试
     *
     * @return
     */
    public boolean isRetry() {
        return retry != null && retry;
    }

    public void setRetry(boolean retry) {
        this.retry = retry;
    }

}
