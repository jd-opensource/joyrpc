package io.joyrpc.transport.telnet;

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

import io.joyrpc.transport.channel.SendResult;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * telnet 应答对象
 *
 * @date: 2019/4/28
 */
public class TelnetResponse {
    /**
     * 应答消息
     */
    protected StringBuilder builder;

    /**
     * 发送应答消息后结果处理consumer
     */
    private BiConsumer<Void,Throwable> consumer;

    public TelnetResponse(String response) {
        this(new StringBuilder(response == null ? "" : response), null);
    }

    public TelnetResponse(StringBuilder builder) {
        this(builder, null);
    }

    public TelnetResponse(String response, BiConsumer<Void,Throwable> consumer) {
        this(new StringBuilder(response == null ? "" : response), consumer);
    }

    public TelnetResponse(StringBuilder builder, BiConsumer<Void,Throwable> consumer) {
        this.builder = builder;
        this.consumer = consumer;
    }

    public String getResponse() {
        return builder.toString();
    }

    public boolean isEmpty() {
        return builder == null || builder.length() == 0;
    }

    public StringBuilder getBuilder() {
        return builder;
    }

    public BiConsumer<Void,Throwable> getConsumer() {
        return consumer;
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
