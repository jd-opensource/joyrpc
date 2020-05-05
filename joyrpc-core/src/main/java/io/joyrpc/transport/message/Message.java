package io.joyrpc.transport.message;

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

/**
 * 消息接口
 */
public interface Message<H extends Header, T> {

    /**
     * 获取头部
     *
     * @return 头部
     */
    H getHeader();

    /**
     * 设置头部
     *
     * @param header 头部
     */
    void setHeader(H header);

    /**
     * 获取载体
     *
     * @return 载体
     */
    T getPayLoad();

    /**
     * 设置载体
     *
     * @param payload 载体
     */
    void setPayLoad(T payload);

    /**
     * 获取消息ID
     *
     * @return 消息ID
     */
    default long getMsgId() {
        H header = getHeader();
        return header == null ? -1 : header.getMsgId();
    }

    /**
     * 设置消息ID
     *
     * @param id 消息ID
     */
    default void setMsgId(long id) {
        getHeader().setMsgId(id);
    }

    /**
     * 获取消息类型
     *
     * @return 消息类型
     */
    default int getMsgType() {
        H header = getHeader();
        return header == null ? -1 : header.getMsgType();
    }

    /**
     * 判断是否是请求消息
     *
     * @return 请求标识
     */
    boolean isRequest();

    /**
     * 获取会话ID
     *
     * @return 会话ID
     */
    default int getSessionId() {
        H header = getHeader();
        return header == null ? -1 : header.getSessionId();
    }

    /**
     * 设置会话ID
     *
     * @param sessionId 会话ID
     */
    default void setSessionId(int sessionId) {
        getHeader().setSessionId(sessionId);
    }

    /**
     * 获取会话
     *
     * @return 会话
     */
    default Session getSession() {
        H header = getHeader();
        return header == null ? null : header.getSession();
    }

    /**
     * 设置会话
     *
     * @param session 会话
     */
    default void setSession(Session session) {
        H header = getHeader();
        if (header != null) {
            header.setSession(session);
        }
    }

}

