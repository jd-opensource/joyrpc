package io.joyrpc.codec.serialization;

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

import io.joyrpc.cluster.discovery.backup.BackupDatum;
import io.joyrpc.cluster.discovery.backup.BackupShard;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static io.joyrpc.Plugin.SERIALIZATION;

public class BackupTest {

    @Test
    public void testBackup() {

        Map<String, List<BackupShard>> clusters = new HashMap<>();
        List<BackupShard> shards = new LinkedList<>();
        shards.add(new BackupShard("test", null, null, "joyrpc", "joyrpc://192.168.1.1:22000", 100));
        clusters.put("test", shards);
        Map<String, Map<String, String>> configs = new HashMap<>();
        Map<String, String> config = new HashMap<>();
        config.put("socketTimeout", "10000");
        configs.put("test", config);

        BackupDatum datum = new BackupDatum();
        datum.setClusters(clusters);
        datum.setConfigs(configs);

        String[] types = new String[]{"fst", "hessian", "java", "json@fastjson", "json@jackson", "kryo", "protostuff"};

        Serialization serialization;
        Serializer serializer;
        UnsafeByteArrayOutputStream baos = new UnsafeByteArrayOutputStream(1024);
        UnsafeByteArrayInputStream bais;

        for (int j = 0; j < types.length; j++) {
            serialization = SERIALIZATION.get(types[j]);
            serializer = serialization.getSerializer();

            baos.reset();
            serializer.serialize(baos, datum);
            bais = new UnsafeByteArrayInputStream(baos.toByteArray());
            BackupDatum data = serializer.deserialize(bais, BackupDatum.class);
            Assert.assertEquals(data, datum);
        }
    }

}
