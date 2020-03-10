package io.joyrpc.transport.session;

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

import io.joyrpc.codec.checksum.Checksum;
import io.joyrpc.codec.compression.Compression;
import io.joyrpc.codec.serialization.Serialization;
import io.joyrpc.util.SystemClock;

import java.util.List;
import java.util.Map;

/**
 * 会话
 */
public interface Session {

    /**
     * 远程节点启动时间key
     */
    String REMOTE_START_TIMESTAMP = "remoteStartTime";

    /**
     * 会话认证成功
     */
    int AUTH_SESSION_SUCCESS = 1;

    /**
     * 会话认证失败
     */
    int AUTH_SESSION_FAIL = -1;

    /**
     * 没有进行会话认证
     */
    int AUTH_SESSION_NONE = 0;

    /**
     * 获取会话ID
     *
     * @return
     */
    int getSessionId();

    /**
     * 设置会话ID
     *
     * @param sessionId
     */
    void setSessionId(int sessionId);

    /**
     * 获取超时时间
     *
     * @return
     */
    long getTimeout();

    /**
     * 设置超时时间
     *
     * @param timeout
     */
    void setTimeout(long timeout);

    /**
     * 获取上次心跳时间
     *
     * @return
     */
    long getLastTime();

    /**
     * 设置上次心跳时间
     *
     * @param time
     */
    void setLastTime(long time);

    /**
     * 是否过期
     *
     * @return
     */
    default boolean isExpire() {
        return SystemClock.now() - getLastTime() > getTimeout();
    }

    /**
     * 远程节点启动时间
     *
     * @return
     */
    default long getRemoteStartTime() {
        try {
            return Long.valueOf(getOrDefault(REMOTE_START_TIMESTAMP, "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 是否认证
     *
     * @return
     */
    int getAuthenticated();

    /**
     * 设置认证
     *
     * @param authenticated
     */
    void setAuthenticated(int authenticated);

    /**
     * 获取序列化
     *
     * @return
     */
    Serialization getSerialization();

    /**
     * 设置序列化
     *
     * @param serialization
     */
    void setSerialization(Serialization serialization);

    /**
     * 获取压缩
     *
     * @return
     */
    Compression getCompression();

    /**
     * 设置压缩算法
     *
     * @param compression
     */
    void setCompression(Compression compression);

    /**
     * 获取校验和
     *
     * @return
     */
    Checksum getChecksum();

    /**
     * 设置校验和
     *
     * @param checksum
     */
    void setChecksum(Checksum checksum);

    /**
     * 获取序列化类型
     *
     * @return
     */
    default byte getSerializationType() {
        Serialization serialization = getSerialization();
        return serialization == null ? Serialization.JAVA_ID : serialization.getTypeId();
    }

    /**
     * 获取压缩类型
     *
     * @return
     */
    default byte getCompressionType() {
        Compression compression = getCompression();
        return compression == null ? 0 : compression.getTypeId();
    }

    /**
     * 获取校验和类型
     *
     * @return
     */
    default byte getChecksumType() {
        Checksum checksum = getChecksum();
        return checksum == null ? 0 : checksum.getTypeId();
    }

    /**
     * 获取可用序列化类型
     *
     * @return
     */
    List<String> getSerializations();

    /**
     * 设置序列化类型
     *
     * @param serializations
     */
    void setSerializations(List<String> serializations);


    /**
     * 获取可用压缩算法
     *
     * @return
     */
    default List<String> getCompressions() {
        return null;
    }

    /**
     * 设置可用的压缩算法
     *
     * @param compressions
     */
    void setCompressions(List<String> compressions);

    /**
     * 获取可用校验和
     *
     * @return
     */
    default List<String> getChecksums() {
        return null;
    }

    /**
     * 设置可用校验和算法
     *
     * @param checksums
     */
    void setChecksums(List<String> checksums);

    /**
     * 获取扩展属性
     *
     * @return
     */
    Map<String, String> getAttributes();

    /**
     * 获取扩展属性值
     *
     * @param key
     * @return
     */
    String get(final String key);

    /**
     * 获取扩展属性值，没有则返回默认值
     *
     * @param key
     * @param def
     * @return
     */
    default String getOrDefault(final String key, final String def) {
        if (key == null) {
            return def;
        }
        String v = get(key);
        if (v == null) {
            return def;
        }
        return v;
    }

    /**
     * 添加扩展属性
     *
     * @param key
     * @param value
     * @return
     */
    String put(final String key, final String value);

    /**
     * 不存在的时候添加扩展属性
     *
     * @param key
     * @param value
     * @return
     */
    String putIfAbsent(final String key, final String value);

    /**
     * 添加扩展属性
     *
     * @param attrs
     */
    void putAll(final Map<String, String> attrs);

    /**
     * 删除扩展属性
     *
     * @param key
     * @return
     */
    String remove(final String key);

}
