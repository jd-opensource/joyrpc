package io.joyrpc.transport.channel;

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

import io.joyrpc.transport.session.Session;
import io.joyrpc.util.SystemClock;

import java.util.concurrent.CompletableFuture;

/**
 * @date: 2019/5/9
 */
public class EnhanceCompletableFuture<I, M> extends CompletableFuture<M> {
    /**
     * 消息ID
     */
    protected I messageId;
    /**
     * 会话
     */
    protected Session session;
    /**
     * 超时时间
     */
    protected long expireTime;
    /**
     * 扩展属性
     */
    protected Object attr;

    /**
     * 构造函数
     *
     * @param messageId
     * @param session
     * @param expireTime
     */
    public EnhanceCompletableFuture(I messageId, Session session, long expireTime) {
        this.messageId = messageId;
        this.session = session;
        this.expireTime = expireTime;
    }

    public Session getSession() {
        return session;
    }

    public I getMessageId() {
        return messageId;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public Object getAttr() {
        return attr;
    }

    public void setAttr(Object attr) {
        this.attr = attr;
    }

    /**
     * 是否过期
     *
     * @return
     */
    public boolean isExpire() {
        return expireTime <= SystemClock.now();
    }
}
