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
 * 没有存活的服务提供者异常
 */
public class NoAliveProviderException extends RpcException {

    public NoAliveProviderException() {
    }

    public NoAliveProviderException(String message) {
        super(message);
    }

    public NoAliveProviderException(String message, String errorCode) {
        super(message, errorCode);
    }

    public NoAliveProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoAliveProviderException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }

    public NoAliveProviderException(Throwable cause) {
        super(cause);
    }

    public NoAliveProviderException(Throwable cause, String errorCode) {
        super(cause, errorCode);
    }
}
