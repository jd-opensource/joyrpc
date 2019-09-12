package io.joyrpc.protocol.grpc;

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

import io.grpc.internal.GrpcUtil;

public enum HeaderMapping {

    STREAM_ID("stream_id", (byte) 1),
    /**
     * Accept-Encoding
     */
    ACCEPT_ENCODING(GrpcUtil.MESSAGE_ACCEPT_ENCODING, (byte) 3);

    private String key;
    private byte num;

    HeaderMapping(String key, byte num) {
        this.key = key;
        this.num = num;
    }

    public String getKey() {
        return key;
    }

    public byte getNum() {
        return num;
    }

    public HeaderMapping valueOf(byte num) {
        switch (num) {
            case 1:
                return STREAM_ID;
            case 2:
                return ACCEPT_ENCODING;
            default:
                throw new IllegalArgumentException("no matched header mapping, num:" + num);
        }
    }
}
