package io.joyrpc.cluster.discovery.backup;

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

import io.joyrpc.cluster.Shard;
import io.joyrpc.extension.URL;

import java.io.Serializable;

/**
 * 存储的分片信息
 */
public class BackupShard implements Serializable {
    private static final long serialVersionUID = -1479600922503780968L;
    /**
     * The Name.
     */
    protected String name;
    /**
     * The Region.
     */
    protected String region;
    /**
     * The Data center.
     */
    protected String dataCenter;
    /**
     * The Protocol.
     */
    protected String protocol;
    /**
     * The Address.
     */
    protected String address;
    /**
     * The Weight.
     */
    protected int weight;

    public BackupShard() {
    }

    public BackupShard(Shard shard) {
        this.name = shard.getName();
        this.region = shard.getRegion();
        this.dataCenter = shard.getDataCenter();
        this.protocol = shard.getProtocol();
        this.weight = shard.getWeight();
        this.address = shard.getUrl().toString();
    }

    public BackupShard(String name, String region, String dataCenter, String protocol, String address, int weight) {
        this.name = name;
        this.region = region;
        this.dataCenter = dataCenter;
        this.protocol = protocol;
        this.address = address;
        this.weight = weight;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getDataCenter() {
        return dataCenter;
    }

    public void setDataCenter(String dataCenter) {
        this.dataCenter = dataCenter;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    /**
     * 转换成Shard
     *
     * @return
     */
    public Shard toShard() {
        return new Shard.DefaultShard(name, region, dataCenter, protocol,
                address != null && !address.isEmpty() ? URL.valueOf(address) : null,
                weight, Shard.ShardState.INITIAL);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BackupShard shard = (BackupShard) o;

        if (weight != shard.weight) {
            return false;
        }
        if (name != null ? !name.equals(shard.name) : shard.name != null) {
            return false;
        }
        if (region != null ? !region.equals(shard.region) : shard.region != null) {
            return false;
        }
        if (dataCenter != null ? !dataCenter.equals(shard.dataCenter) : shard.dataCenter != null) {
            return false;
        }
        if (protocol != null ? !protocol.equals(shard.protocol) : shard.protocol != null) {
            return false;
        }

        if (address != null ? !address.equals(shard.address) : shard.address != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
