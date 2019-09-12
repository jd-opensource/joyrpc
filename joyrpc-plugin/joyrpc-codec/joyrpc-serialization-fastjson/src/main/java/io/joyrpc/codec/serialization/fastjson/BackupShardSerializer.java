package io.joyrpc.codec.serialization.fastjson;

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

import com.alibaba.fastjson.serializer.AutowiredObjectSerializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import io.joyrpc.cluster.discovery.backup.BackupShard;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class BackupShardSerializer implements AutowiredObjectSerializer {

    public static final BackupShardSerializer INSTANCE = new BackupShardSerializer();

    @Override
    public Set<Type> getAutowiredFor() {
        Set<Type> result = new HashSet<>(1);
        result.add(BackupShard.class);
        return result;
    }

    @Override
    public void write(final JSONSerializer serializer, final Object object, final Object fieldName, final Type fieldType, final int features) throws IOException {
        if (object == null) {
            serializer.writeNull();
        } else {
            SerializeWriter out = serializer.getWriter();
            out.write('{');
            BackupShard backupShard = (BackupShard) object;
            if (backupShard.getName() != null) {
                out.writeFieldName("name");
                out.writeString(backupShard.getName());
                out.append(",");
            }
            if (backupShard.getRegion() != null) {
                out.writeFieldName("region");
                out.writeString(backupShard.getRegion());
                out.append(",");
            }
            if (backupShard.getDataCenter() != null) {
                out.writeFieldName("dataCenter");
                out.writeString(backupShard.getDataCenter());
                out.append(",");
            }
            if (backupShard.getProtocol() != null) {
                out.writeFieldName("protocol");
                out.writeString(backupShard.getProtocol());
                out.append(",");
            }
            if (backupShard.getAddress() != null) {
                out.writeFieldName("address");
                out.writeString(backupShard.getAddress());
                out.append(",");
            }
            out.writeFieldName("weight");
            out.writeInt(backupShard.getWeight());
            out.write('}');
        }
    }
}
