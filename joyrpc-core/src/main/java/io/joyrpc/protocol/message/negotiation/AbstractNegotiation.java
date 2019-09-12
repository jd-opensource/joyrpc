package io.joyrpc.protocol.message.negotiation;

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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 协商协议，这个协议采用java序列化，必须实现Serializable接口
 *
 * @date: 2019/1/8
 */
public class AbstractNegotiation implements Serializable {

    /**
     * 首选的序列化版本信息
     */
    protected String serialization;
    /**
     * 首选的压缩算法版本
     */
    protected String compression;
    /**
     * 首选的校验和算法版本
     */
    protected String checksum;
    /**
     * 可选序列化版本信息
     */
    protected List<String> serializations;
    /**
     * 可选压缩算法版本
     */
    protected List<String> compressions;
    /**
     * 可选校验和算法版本
     */
    protected List<String> checksums;
    /**
     * 扩展属性
     */
    protected Map<String, String> attributes = new HashMap<>();

    public AbstractNegotiation() {
    }

    /**
     * 构造函数
     *
     * @param serialization
     * @param compression
     * @param checksum
     * @param serializations
     * @param compressions
     * @param checksums
     */
    public AbstractNegotiation(String serialization,
                               String compression, String checksum,
                               List<String> serializations, List<String> compressions,
                               List<String> checksums) {
        this.serialization = serialization;
        this.compression = compression;
        this.checksum = checksum;
        this.serializations = serializations;
        this.compressions = compressions;
        this.checksums = checksums;
    }

    public String getSerialization() {
        return serialization;
    }

    public void setSerialization(String serialization) {
        this.serialization = serialization;
    }

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public List<String> getSerializations() {
        return serializations;
    }

    public void setSerializations(List<String> serializations) {
        this.serializations = serializations;
    }

    public List<String> getCompressions() {
        return compressions;
    }

    public void setCompressions(List<String> compressions) {
        this.compressions = compressions;
    }

    public List<String> getChecksums() {
        return checksums;
    }

    public void setChecksums(List<String> checksums) {
        this.checksums = checksums;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    /**
     * 添加扩展属性
     *
     * @param key
     * @param value
     */
    public void addAttribute(String key, String value) {
        if (key != null) {
            if (value == null) {
                attributes.remove(key);
            } else {
                attributes.put(key, value);
            }
        }
    }

    /**
     * 删除扩展属性
     *
     * @param key
     * @return
     */
    public String removeAttribute(String key) {
        return key == null ? null : attributes.remove(key);
    }
}
