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
 * 实例化异常
 */
public class CreationException extends ReflectionException {

    private static final long serialVersionUID = -7067155283140779827L;

    public CreationException() {
    }

    public CreationException(String message) {
        super(message);
    }

    public CreationException(String message, String errorCode) {
        super(message, errorCode);
    }

    public CreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CreationException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }

    public CreationException(Throwable cause) {
        super(cause);
    }

    public CreationException(Throwable cause, String errorCode) {
        super(cause, errorCode);
    }
}
