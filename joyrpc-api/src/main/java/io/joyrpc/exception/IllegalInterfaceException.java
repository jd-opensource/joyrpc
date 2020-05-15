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
 * 不合法的接口
 */
public class IllegalInterfaceException extends InitializationException {

    private static final long serialVersionUID = 8435511195915542547L;

    public IllegalInterfaceException() {
    }

    public IllegalInterfaceException(String message) {
        super(message);
    }

    public IllegalInterfaceException(String message, String errorCode) {
        super(message, errorCode);
    }

    public IllegalInterfaceException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalInterfaceException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }

    public IllegalInterfaceException(Throwable cause) {
        super(cause);
    }

    public IllegalInterfaceException(Throwable cause, String errorCode) {
        super(cause, errorCode);
    }
}
