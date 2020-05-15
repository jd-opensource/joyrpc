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

import io.joyrpc.transport.message.Header;

import java.io.Serializable;

/**
 * 用于Consumer Group自动适配
 */
public class NoReferException extends RpcException implements Serializable {

    /**
     */
    protected NoReferException() {
    }

    /**
     *
     * @param errorMsg the error msg
     */
    public NoReferException(String errorMsg) {
        super(errorMsg);
    }

    /**
     *
     * @param errorMsg the error msg
     * @param e        the e
     */
    public NoReferException(String errorMsg, Throwable e) {
        super(errorMsg, e);
    }

    public NoReferException(Header header, String message, String errorCode) {
        super(header, message, errorCode);
    }
}
