package io.joyrpc.transport.netty4.test.serialization;

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
 * @date: 2019/3/22
 */
public class MessageBody {
    private String bodyName;
    private String bodyDate;

    public MessageBody() {

    }

    public MessageBody(String bodyName, String bodyDate) {
        this.bodyName = bodyName;
        this.bodyDate = bodyDate;
    }

    public String getBodyName() {
        return bodyName;
    }

    public void setBodyName(String bodyName) {
        this.bodyName = bodyName;
    }

    public String getBodyDate() {
        return bodyDate;
    }

    public void setBodyDate(String bodyDate) {
        this.bodyDate = bodyDate;
    }

    @Override
    public String toString() {
        return "MessageBody{" +
                "bodyName='" + bodyName + '\'' +
                ", bodyDate='" + bodyDate + '\'' +
                '}';
    }
}
