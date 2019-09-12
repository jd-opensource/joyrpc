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
import io.joyrpc.constants.Constants;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.joyrpc.constants.Constants.ALIAS_OPTION;
import static io.joyrpc.constants.Constants.CONFIG_KEY_INTERFACE;
import static io.joyrpc.constants.Constants.BUILD_VERSION_KEY;
import static io.joyrpc.context.Environment.*;

/**
 * @date: 2019/5/15
 */
public class DefaultSession implements Session {

    /**
     * session id
     */
    protected int id;

    /**
     * 默认会话超时时间，单位毫秒
     */
    protected long timeout = 30000;
    /**
     * 是否认证通过
     */
    protected boolean authenticated = true;
    /**
     * 序列化类型（性能优化）
     */
    protected byte serializationType = Serialization.JAVA_ID;
    /**
     * 压缩类型（性能优化）
     */
    protected byte compressionType = Compression.NONE;
    /**
     * 校验和类型（性能优化）
     */
    protected byte checksumType = Checksum.NONE;

    /**
     * 序列化
     */
    protected Serialization serialization;

    /**
     * 压缩算法
     */
    protected Compression compression;

    /**
     * 校验和算法
     */
    protected Checksum checksum;

    /**
     * 可选序列化
     */
    protected List<String> serializations;

    /**
     * 可选压缩算法
     */
    protected List<String> compressions;

    /**
     * 可选校验和算法
     */
    protected List<String> checksums;

    /**
     * 接口名称
     */
    protected String interfaceName;
    /**
     * 别名
     */
    protected String alias;
    /**
     * 远端Java版本
     */
    protected String remoteJavaVersion;
    /**
     * 远端版本
     */
    protected Short remoteBuildVersion;
    /**
     * 远端应用ID
     */
    protected String remoteAppId;
    /**
     * 远端应用名称
     */
    protected String remoteAppName;
    /**
     * 远端应用实例
     */
    protected String remoteAppIns;
    /**
     * 远端应用分组
     */
    protected String remoteAppGroup;

    /**
     * 会话属性集
     */
    protected Map<String, String> attrs = new ConcurrentHashMap<>();

    public DefaultSession() {
    }

    public DefaultSession(int sessionId) {
        this.id = sessionId;
    }

    public DefaultSession(int sessionId, long timeout) {
        this.id = sessionId;
        this.timeout = timeout > 0 ? timeout : this.timeout;
    }

    @Override
    public int getSessionId() {
        return id;
    }

    @Override
    public void setSessionId(int sessionId) {
        this.id = sessionId;
    }

    @Override
    public long getTimeout() {
        return timeout;
    }

    @Override
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    @Override
    public Serialization getSerialization() {
        return serialization;
    }

    @Override
    public void setSerialization(final Serialization serialization) {
        this.serializationType = serialization == null ? Serialization.JAVA_ID : serialization.getTypeId();
        this.serialization = serialization;
    }

    @Override
    public Compression getCompression() {
        return compression;
    }

    @Override
    public void setCompression(final Compression compression) {
        this.compressionType = compression == null ? Compression.NONE : compression.getTypeId();
        this.compression = compression;
    }

    @Override
    public Checksum getChecksum() {
        return checksum;
    }

    @Override
    public void setChecksum(final Checksum checksum) {
        this.checksumType = checksum == null ? Checksum.NONE : checksum.getTypeId();
        this.checksum = checksum;
    }

    @Override
    public byte getSerializationType() {
        return serializationType;
    }

    @Override
    public byte getCompressionType() {
        return compressionType;
    }

    @Override
    public byte getChecksumType() {
        return checksumType;
    }

    @Override
    public List<String> getSerializations() {
        return serializations;
    }

    @Override
    public void setSerializations(List<String> serializations) {
        this.serializations = serializations;
    }

    @Override
    public List<String> getCompressions() {
        return compressions;
    }

    @Override
    public void setCompressions(List<String> compressions) {
        this.compressions = compressions;
    }

    @Override
    public List<String> getChecksums() {
        return checksums;
    }

    @Override
    public void setChecksums(List<String> checksums) {
        this.checksums = checksums;
    }

    public String getInterfaceName() {
        if (interfaceName == null) {
            interfaceName = attrs.get(CONFIG_KEY_INTERFACE);
        }
        return interfaceName;
    }

    public String getAlias() {
        if (alias == null) {
            alias = attrs.get(ALIAS_OPTION.getName());
        }
        return alias;
    }

    public String getRemoteJavaVersion() {
        if (remoteJavaVersion == null) {
            remoteJavaVersion = attrs.get(Constants.JAVA_VERSION_KEY);
        }
        return remoteJavaVersion;
    }

    public Short getRemoteBuildVersion() {
        if (remoteBuildVersion == null) {
            String version = attrs.get(BUILD_VERSION_KEY);
            if (version != null) {
                try {
                    remoteBuildVersion = Short.parseShort(version);
                } catch (NumberFormatException e) {
                }
            }
        }
        return remoteBuildVersion;
    }

    public String getRemoteAppId() {
        if (remoteAppId == null) {
            remoteAppId = attrs.get(APPLICATION_ID);
        }
        return remoteAppId;
    }

    public String getRemoteAppName() {
        if (remoteAppName == null) {
            remoteAppName = attrs.get(APPLICATION_NAME);
        }
        return remoteAppName;
    }

    public String getRemoteAppIns() {
        if (remoteAppIns == null) {
            remoteAppIns = attrs.get(APPLICATION_INSTANCE);
        }
        return remoteAppIns;
    }

    public String getRemoteAppGroup() {
        if (remoteAppGroup == null) {
            remoteAppGroup = attrs.get(APPLICATION_GROUP);
        }
        return remoteAppGroup;
    }

    @Override
    public String get(String key) {
        return key == null ? null : attrs.get(key);
    }

    @Override
    public Map<String, String> getAttributes() {
        return attrs;
    }

    @Override
    public String put(String key, String value) {
        if (key == null || value == null) {
            return null;
        }
        return attrs.put(key, value);
    }

    @Override
    public String putIfAbsent(String key, String value) {
        if (key == null || value == null) {
            return null;
        }
        return attrs.putIfAbsent(key, value);
    }

    @Override
    public void putAll(final Map<String, String> attrs) {
        if (attrs == null) {
            return;
        }
        this.attrs.putAll(attrs);
    }

    @Override
    public String remove(String key) {
        return key == null || attrs == null ? null : attrs.remove(key);
    }


}
