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

import java.io.Serializable;
import java.util.List;

/**
 * 备份集群数据
 */
public class BackupCluster implements Serializable {

    private static final long serialVersionUID = 4418447400095441113L;
    /**
     * 集群名称
     */
    protected String name;
    /**
     * 集群分片
     */
    protected List<BackupShard> shards;

    public BackupCluster() {
    }

    public BackupCluster(String name, List<BackupShard> shards) {
        this.name = name;
        this.shards = shards;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<BackupShard> getShards() {
        return shards;
    }

    public void setShards(List<BackupShard> shards) {
        this.shards = shards;
    }
}
