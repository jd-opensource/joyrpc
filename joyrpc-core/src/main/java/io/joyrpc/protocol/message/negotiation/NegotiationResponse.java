package io.joyrpc.protocol.message.negotiation;

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

import java.util.List;

/**
 * @date: 2019/1/8
 */
public class NegotiationResponse extends AbstractNegotiation implements SuccessResponse, Cloneable {
    private static final long serialVersionUID = -6985194305592778824L;

    /**
     * 协商未通过
     */
    public static final int NOT_SUPPORT = 1;

    /**
     * 协商通过
     */
    public static final int SUCCESS = 0;

    /**
     * 协商状态
     */
    protected int status;
    /**
     * 异常信息
     */
    protected String message;

    public NegotiationResponse() {
    }

    public NegotiationResponse(int status) {
        this.status = status;
    }

    public NegotiationResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public NegotiationResponse(int status, String serialization,
                               String compression, String checksum,
                               List<String> serializations, List<String> compressions,
                               List<String> checksums) {
        super(serialization, compression, checksum, serializations, compressions, checksums);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public boolean isSuccess() {
        return status == SUCCESS;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public NegotiationResponse clone() {
        try {
            return (NegotiationResponse) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

}
