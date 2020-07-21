package io.joyrpc.codec.serialization.jackson;

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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.joyrpc.cluster.discovery.backup.BackupShard;

import java.io.IOException;

/**
 * 备份序列化，加快序列化性能
 */
public class BackupShardSerializer extends JsonSerializer<BackupShard> {

    public static final BackupShardSerializer INSTANCE = new BackupShardSerializer();

    @Override
    public void serialize(final BackupShard shard, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {
        if (shard == null) {
            gen.writeNull();
        } else {
            gen.writeStartObject();
            gen.writeStringField(BackupShard.NAME, shard.getName());
            gen.writeStringField(BackupShard.REGION, shard.getRegion());
            gen.writeStringField(BackupShard.DATA_CENTER, shard.getDataCenter());
            gen.writeStringField(BackupShard.PROTOCOL, shard.getProtocol());
            gen.writeStringField(BackupShard.ADDRESS, shard.getAddress());
            gen.writeNumberField(BackupShard.WEIGHT, shard.getWeight());
            gen.writeEndObject();
        }
    }
}
