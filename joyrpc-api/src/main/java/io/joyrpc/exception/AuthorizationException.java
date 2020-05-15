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

import io.joyrpc.extension.URL;

/**
 * 权限异常
 */
public class AuthorizationException extends LafException {
    private static final long serialVersionUID = -3209403955589243356L;

    protected URL url;

    public AuthorizationException(URL url) {
        super(null, null, false, false, null, false);
        this.url = url;
    }

    public AuthorizationException(String message, URL url) {
        super(message, null, false, false, null, false);
        this.url = url;
    }

    public AuthorizationException(String message, String errorCode, URL url) {
        super(message, null, false, false, errorCode, false);
        this.url = url;
    }

    public AuthorizationException(String message, Throwable cause, URL url) {
        super(message, cause, false, false, null, false);
        this.url = url;
    }

    public AuthorizationException(String message, Throwable cause, String errorCode, URL url) {
        super(message, cause, false, false, errorCode, false);
        this.url = url;
    }

    public AuthorizationException(Throwable cause, URL url) {
        super(cause == null ? null : cause.toString(), cause, false, false, null, false);
        this.url = url;
    }

    public AuthorizationException(Throwable cause, String errorCode, URL url) {
        super(cause == null ? null : cause.toString(), cause, false, false, errorCode, false);
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }
}
