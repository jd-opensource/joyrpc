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
 * 消息头
 *
 * @date: 2019/1/7
 */
public interface Header extends Cloneable {

    /**
     * 获取ID
     *
     * @return
     */
    default int getMsgId() {
        return 0;
    }

    /**
     * 设置ID
     *
     * @param id
     */
    default void setMsgId(int id) {
    }

    /**
     * 获取类型
     *
     * @return
     */
    default int getMsgType() {
        return 0;
    }

    /**
     * 设置类型
     *
     * @param type
     */
    default void setMsgType(final int type) {

    }

    /**
     * 获取会话ID
     *
     * @return
     */
    default int getSessionId() {
        return 0;
    }

    /**
     * 设置会话ID
     *
     * @param sessionId
     */
    default void setSessionId(int sessionId) {

    }

    /**
     * 获取压缩方式
     *
     * @return
     */
    default byte getCompression() {
        return 0;
    }

    /**
     * 设置压缩方式
     *
     * @param compression
     */
    default void setCompression(byte compression) {
    }

    /**
     * 获取校验和
     *
     * @return
     */
    default byte getChecksum() {
        return 0;
    }

    /**
     * 设置校验和
     *
     * @param checksum
     */
    default void setChecksum(byte checksum) {
    }

    /**
     * 获取序列化方式
     *
     * @return
     */
    default byte getSerialization() {
        return 0;
    }

    /**
     * 设置序列化方式
     *
     * @param serialization
     */
    default void setSerialization(byte serialization) {
    }

    /**
     * 获取超时时间
     *
     * @return
     */
    default int getTimeout() {
        return 5000;
    }

    /**
     * 设置超时时间
     *
     * @param timeout
     */
    default void setTimeout(int timeout) {

    }

    /**
     * 获取数据长度
     *
     * @return
     */
    default Integer getLength() {
        return null;
    }

    /**
     * 设置数据包长度，去掉魔法头
     *
     * @param length
     */
    default void setLength(Integer length) {
    }

    /**
     * 获取数据头长度
     *
     * @return
     */
    Short getHeaderLength();

    /**
     * 设置消息头长度
     *
     * @param headerLength
     */
    default void setHeaderLength(Short headerLength) {
    }

    /**
     * 复制一份，用于协议转换
     *
     * @return
     */
    Header clone();

    /**
     * 获取session
     *
     * @return
     */
    Session getSession();

    /**
     * 设置session
     *
     * @param session
     */
    void setSession(Session session);

}
