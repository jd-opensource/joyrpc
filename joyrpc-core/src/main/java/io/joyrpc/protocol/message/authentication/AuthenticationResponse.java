package io.joyrpc.protocol.message.authentication;

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

import io.joyrpc.protocol.message.SuccessResponse;

/**
 * 认证应答
 */
public class AuthenticationResponse implements SuccessResponse {

    /**
     * 认证未通过
     */
    public static final int NOT_PASS = 1;

    /**
     * 认证通过
     */
    public static final int PASS = 0;

    /**
     * 协商状态
     */
    protected int status;
    /**
     * 异常信息
     */
    protected String message;

    public AuthenticationResponse() {
    }

    public AuthenticationResponse(int status) {
        this.status = status;
    }

    public AuthenticationResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean isSuccess() {
        return status == PASS;
    }

}
