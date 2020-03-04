package io.joyrpc;

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

import io.joyrpc.context.RequestContext;
import io.joyrpc.transport.message.Message;

import java.io.Serializable;

/**
 * 业务方法调用结果，使用在Filter链里面
 *
 * @date: 31/1/2019
 */
public class Result implements Serializable {
    /**
     * 异常
     */
    protected Throwable exception;
    /**
     * 返回值
     */
    protected Object value;
    /**
     * 应答消息
     */
    protected Message message;
    /**
     * 保存的用户线程上下文
     */
    protected transient RequestContext context;

    /**
     * 构造函数
     *
     * @param context
     */
    public Result(final RequestContext context) {
        this.context = context;
    }

    /**
     * 构造函数
     *
     * @param context
     * @param value
     */
    public Result(final RequestContext context, final Object value) {
        this.context = context;
        this.value = value;
    }

    /**
     * 构造函数
     *
     * @param context
     * @param value
     * @param response
     */
    public Result(final RequestContext context, final Object value, final Message response) {
        this.context = context;
        this.value = value;
        this.message = response;
    }

    /**
     * 构造函数
     *
     * @param context
     * @param throwable
     */
    public Result(final RequestContext context, final Throwable throwable) {
        this.context = context;
        this.exception = throwable;
    }

    /**
     * 构造函数
     *
     * @param context
     * @param throwable
     * @param response
     */
    public Result(final RequestContext context, final Throwable throwable, final Message response) {
        this.context = context;
        this.exception = throwable;
        this.message = response;
    }

    public Object getValue() {
        return value;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public Throwable getException() {
        return exception;
    }

    public boolean isException() {
        return exception != null;
    }

    public Message getMessage() {
        return message;
    }

    public RequestContext getContext() {
        return context;
    }

}
