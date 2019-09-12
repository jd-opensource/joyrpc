package io.joyrpc.protocol.grpc.message;

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

import io.joyrpc.protocol.message.MessageHeader;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.util.GrpcType;

/**
 * @date: 2019/8/20
 */
public class GrpcResponseMessage<T> extends ResponseMessage<T> {

    protected transient GrpcType grpcType;

    public GrpcResponseMessage(MessageHeader header, GrpcType grpcType) {
        super(header);
        this.grpcType = grpcType;
    }

    public GrpcType getGrpcType() {
        return grpcType;
    }

    public void setGrpcType(GrpcType grpcType) {
        this.grpcType = grpcType;
    }
}
