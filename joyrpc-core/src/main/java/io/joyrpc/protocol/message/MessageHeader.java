package io.joyrpc.protocol.message;

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

import io.joyrpc.constants.Constants;
import io.joyrpc.constants.Head;
import io.joyrpc.transport.message.Header;
import io.joyrpc.transport.session.Session;
import io.joyrpc.util.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息头
 */
public class MessageHeader implements Header {
    /**
     * 总长度 length of (header+body), not include MEGICCODE
     */
    protected Integer length;
    /**
     * 消息头长度 length of (PROTOCOLTYPE+...+header tail), not include FULLLENGTH and HEADERLENGTH
     */
    protected Short headerLength;
    /**
     * 协议类别
     */
    protected byte protocolType;
    /**
     * 序列化类别
     */
    protected byte serialization;
    /**
     * 消息标识ID
     */
    protected int msgId;
    /**
     * 客户端超时时长
     */
    protected int timeout = Constants.DEFAULT_TIMEOUT;
    /**
     * 消息类别
     */
    protected byte msgType;
    /**
     * 压缩类别
     */
    protected byte compression;
    /**
     * 会话ID
     */
    protected int sessionId;
    /**
     * 扩展属性
     */
    protected Map<Byte, Object> attributes;

    /**
     * session 对象
     */
    protected transient Session session;

    /**
     * 构造函数
     */
    public MessageHeader() {
    }

    /**
     * 构造函数
     *
     * @param msgType
     */
    public MessageHeader(byte msgType) {
        this.msgType = msgType;
    }

    /**
     * 构造函数
     *
     * @param msgType
     * @param msgId
     */
    public MessageHeader(byte msgType, int msgId) {
        this.msgType = msgType;
        this.msgId = msgId;
    }

    /**
     * 构造函数
     *
     * @param msgType
     * @param serialization
     */
    public MessageHeader(byte msgType, byte serialization) {
        this.serialization = serialization;
        this.msgType = msgType;
    }

    /**
     * 构造函数
     *
     * @param msgType
     * @param serialization
     * @param protocolType
     */
    public MessageHeader(byte msgType, byte serialization, byte protocolType) {
        this.protocolType = protocolType;
        this.serialization = serialization;
        this.msgType = msgType;
    }

    @Override
    public Integer getLength() {
        return length;
    }

    @Override
    public void setLength(Integer length) {
        this.length = length;
    }

    @Override
    public Short getHeaderLength() {
        return headerLength;
    }

    @Override
    public void setHeaderLength(Short headerLength) {
        this.headerLength = headerLength;
    }

    public byte getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(byte protocolType) {
        this.protocolType = protocolType;
    }

    @Override
    public byte getCompression() {
        return compression;
    }

    @Override
    public void setCompression(byte compression) {
        this.compression = compression;
    }

    @Override
    public byte getSerialization() {
        return serialization;
    }

    @Override
    public void setSerialization(byte serialization) {
        this.serialization = serialization;
    }

    @Override
    public int getMsgId() {
        return msgId;
    }

    @Override
    public void setMsgId(int msgId) {
        this.msgId = msgId;
    }

    @Override
    public int getMsgType() {
        return msgType;
    }

    @Override
    public void setMsgType(int msgType) {
        this.msgType = (byte) msgType;
    }

    @Override
    public int getSessionId() {
        return sessionId;
    }

    @Override
    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public Map<Byte, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<Byte, Object> attributes) {
        this.attributes = attributes;
    }

    /**
     * 复制属性
     *
     * @param session 会话
     */
    public void copy(final Session session) {
        serialization = session.getSerializationType();
        compression = session.getCompressionType();
    }

    /**
     * 获取或创建扩展属性
     *
     * @return
     */
    protected Map<Byte, Object> getOrCreateAttributes() {
        if (attributes == null) {
            //TODO 是否是单线程操作
            synchronized (this) {
                if (attributes == null) {
                    attributes = new ConcurrentHashMap<>(5);
                }
            }
        }
        return attributes;
    }

    /**
     * 添加扩展属性
     *
     * @param key   键
     * @param value 值
     */
    public void addAttribute(final Head key, final Object value) {
        if (!key.getType().isInstance(value)) { // 检查类型
            throw new IllegalArgumentException("type mismatch of key:" + key.getKey() + ", expect:"
                    + key.getType().getName() + ", actual:" + value.getClass().getName());
        }
        addAttribute(key.getKey(), value);
    }

    /**
     * 添加扩展属性
     *
     * @param key   键
     * @param value 值
     */
    public void addAttribute(final Byte key, final Object value) {
        if (key == null || value == null) {
            return;
        }
        getOrCreateAttributes().put(key, value);
    }

    /**
     * 删除扩展属性
     *
     * @param key 键
     * @return 原值
     */
    public Object removeAttribute(final Head key) {
        if (attributes == null || key == null) {
            return null;
        }
        return attributes.remove(key.getKey());
    }

    /**
     * 删除扩展属性
     *
     * @param key 键
     * @return 原值
     */
    public Object removeAttribute(final Byte key) {
        if (attributes == null || key == null) {
            return null;
        }
        return attributes.remove(key);
    }

    /**
     * 获取扩展属性
     *
     * @param key 键
     * @return 值
     */
    public Object getAttribute(final Head key) {
        if (attributes == null || key == null) {
            return null;
        }
        return attributes.get(key.getKey());
    }

    /**
     * 获取扩展属性
     *
     * @param key 键
     * @return 值
     */
    public Object getAttribute(final Byte key) {
        if (attributes == null || key == null) {
            return null;
        }
        return attributes.get(key);
    }

    /**
     * 获取扩展属性
     *
     * @param key 键
     * @return 值
     */
    public Byte getAttribute(final Byte key, final Byte def) {
        if (attributes == null || key == null) {
            return def;
        }
        Object result = attributes.get(key);
        if (result instanceof Byte) {
            return (Byte) result;
        } else if (result instanceof Number) {
            return ((Number) result).byteValue();
        } else {
            return def;
        }
    }

    /**
     * 获取扩展属性
     *
     * @param key 键
     * @return 值
     */
    public Short getAttribute(final Byte key, final Short def) {
        if (attributes == null || key == null) {
            return def;
        }
        Object result = attributes.get(key);
        if (result instanceof Short) {
            return (Short) result;
        } else if (result instanceof Number) {
            return ((Number) result).shortValue();
        } else {
            return def;
        }
    }

    /**
     * 获取扩展属性
     *
     * @param key 键
     * @return 值
     */
    public Integer getAttribute(final Byte key, final Integer def) {
        if (attributes == null || key == null) {
            return def;
        }
        Object result = attributes.get(key);
        if (result instanceof Integer) {
            return (Integer) result;
        } else if (result instanceof Number) {
            return ((Number) result).intValue();
        } else {
            return def;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageHeader)) {
            return false;
        }

        MessageHeader that = (MessageHeader) o;

        if (serialization != that.serialization) {
            return false;
        }
        if (headerLength != null ? !headerLength.equals(that.headerLength) : that.headerLength != null) {
            return false;
        }
        if (msgId != that.msgId) {
            return false;
        }
        if (msgType != that.msgType) {
            return false;
        }
        if (protocolType != that.protocolType) {
            return false;
        }
        if (compression != that.compression) {
            return false;
        }
        if (length != null ? !length.equals(that.length) : that.length != null) {
            return false;
        }
        if (timeout != that.timeout) {
            return false;
        }
        if (sessionId != that.sessionId) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = msgId;
        result = 31 * result + (length != null ? length.hashCode() : 0);
        result = 31 * result + serialization;
        result = 31 * result + msgType;
        result = 31 * result + protocolType;
        result = 31 * result + timeout;
        result = 31 * result + compression;
        result = 31 * result + sessionId;
        result = 31 * result + (headerLength != null ? headerLength.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        String keymapStr = StringUtils.EMPTY;
        if (Objects.nonNull(attributes)) {
            for (Map.Entry<Byte, Object> entry : attributes.entrySet()) {
                keymapStr = keymapStr + " " + entry.getKey().toString() + " : " + entry.getValue().toString();
            }
        }
        return "MessageHeader{" +
                "msgId=" + msgId +
                ", length=" + length +
                ", serialization=" + serialization +
                ", msgType=" + msgType +
                ", protocolType=" + protocolType +
                ", compression=" + compression +
                ", headerLength=" + headerLength +
                ", timeout=" + timeout +
                ", sessionId=" + sessionId +
                ", keysMap=" + keymapStr +
                "}";
    }

    /**
     * 构造应答的消息头
     *
     * @param msgType
     * @return
     */
    public MessageHeader response(final byte msgType) {
        return response(msgType, compression);
    }

    /**
     * 构造应答的消息头
     *
     * @param msgType
     * @param compression
     * @return
     */
    public MessageHeader response(final byte msgType, final byte compression) {
        return response(msgType, compression, null);
    }

    public MessageHeader response(final byte msgType, final byte compression, Map<Byte, Object> attributes) {
        MessageHeader result = new MessageHeader();
        result.msgId = msgId;
        result.msgType = msgType;
        result.serialization = serialization;
        result.protocolType = protocolType;
        result.compression = compression;
        result.sessionId = sessionId;
        result.attributes = attributes;
        return result;
    }

    /**
     * 克隆后和整体原来不是一个对象，
     * 属性相同，修改当前属性不会改变原来的
     * map和原来是一个对象，修改当前map也会改原来的
     *
     * @return
     */
    @Override
    public MessageHeader clone() {
        MessageHeader result = new MessageHeader();
        result.msgId = msgId;
        result.serialization = serialization;
        result.msgType = msgType;
        result.protocolType = protocolType;
        result.timeout = timeout;
        result.compression = compression;
        result.length = length;
        result.headerLength = headerLength;
        result.sessionId = sessionId;
        result.attributes = attributes;
        return result;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public void setSession(Session session) {
        this.session = session;
    }
}
