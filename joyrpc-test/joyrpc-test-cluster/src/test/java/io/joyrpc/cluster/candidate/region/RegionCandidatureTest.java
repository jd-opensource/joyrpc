package io.joyrpc.cluster.candidate.region;

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

import io.joyrpc.cluster.Candidate;
import io.joyrpc.cluster.Node;
import io.joyrpc.cluster.Region;
import io.joyrpc.cluster.Shard;
import io.joyrpc.cluster.Shard.ShardState;
import io.joyrpc.cluster.candidate.Candidature;
import io.joyrpc.cluster.candidate.region.RegionCandidature;
import io.joyrpc.extension.URL;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

public class RegionCandidatureTest {

    @Test
    public void test() {
        String name = "test";
        URL url = URL.valueOf("joyrpc://127.0.0.1/xxx.xxx.xxx.xxx");
        List<Node> nodes = new LinkedList<>();
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard1", "huabei", "langfang", "joyrpc", URL.valueOf("joyrpc://192.168.1.1"), 1, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard2", "huabei", "langfang", "joyrpc", URL.valueOf("joyrpc://192.168.1.2"), 2, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard3", "huabei", "langfang", "joyrpc", URL.valueOf("joyrpc://192.168.1.3"), 3, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard4", "huabei", "langfang", "joyrpc", URL.valueOf("joyrpc://192.168.1.4"), 4, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard5", "huabei", "langfang", "joyrpc", URL.valueOf("joyrpc://192.168.1.5"), 5, ShardState.CONNECTED)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard6", "huabei", "langfang", "joyrpc", URL.valueOf("joyrpc://192.168.1.6"), 6, ShardState.WEAK)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard7", "huabei", "langfang", "joyrpc", URL.valueOf("joyrpc://192.168.1.7"), 7, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard8", "huabei", "langfang", "joyrpc", URL.valueOf("joyrpc://192.168.1.8"), 8, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard9", "huabei", "langfang", "joyrpc", URL.valueOf("joyrpc://192.168.1.9"), 9, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard10", "huabei", "langfang", "joyrpc", URL.valueOf("joyrpc://192.168.1.10"), 10, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard11", "huabei", "huitian", "joyrpc", URL.valueOf("joyrpc://192.168.1.11"), 11, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard12", "huabei", "huitian", "joyrpc", URL.valueOf("joyrpc://192.168.1.12"), 12, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard13", "huabei", "huitian", "joyrpc", URL.valueOf("joyrpc://192.168.1.13"), 13, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard14", "huabei", "huitian", "joyrpc", URL.valueOf("joyrpc://192.168.1.14"), 14, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard15", "huabei", "huitian", "joyrpc", URL.valueOf("joyrpc://192.168.1.15"), 15, ShardState.CONNECTED)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard16", "huanan", "guangzhou", "joyrpc", URL.valueOf("joyrpc://192.168.1.16"), 16, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard17", "huanan", "guangzhou", "joyrpc", URL.valueOf("joyrpc://192.168.1.17"), 17, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard18", "huanan", "guangzhou", "joyrpc", URL.valueOf("joyrpc://192.168.1.18"), 18, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard19", "huanan", "guangzhou", "joyrpc", URL.valueOf("joyrpc://192.168.1.19"), 19, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard20", "huanan", "guangzhou", "joyrpc", URL.valueOf("joyrpc://192.168.1.20"), 20, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard21", "", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.21"), 21, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard22", "", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.22"), 22, ShardState.INITIAL)));

        RegionCandidature candidature = new RegionCandidature();

        Candidate.Builder builder = Candidate.builder().region(new Region.DefaultRegion("huabei", "langfang")).nodes(nodes);
        Candidature.Result result = candidature.candidate(null, builder.size(5).build());
        Assert.assertEquals(result.getCandidates().size(), 10);
        Assert.assertEquals(result.getStandbys().size(), 1);
        Assert.assertEquals(result.getBackups().size(), 4);
        Assert.assertEquals(result.getCandidates().get(0).getName(), "shard5");
        Assert.assertEquals(result.getCandidates().get(1).getName(), "shard6");
        Assert.assertEquals(result.getStandbys().get(0).getName(), "shard15");


        result = candidature.candidate(null, builder.size(12).build());
        Assert.assertEquals(result.getCandidates().size(), 12);
        Assert.assertEquals(result.getStandbys().size(), 0);
        Assert.assertEquals(result.getBackups().size(), 3);
        Assert.assertEquals(result.getCandidates().get(0).getName(), "shard5");
        Assert.assertEquals(result.getCandidates().get(1).getName(), "shard6");
        Assert.assertEquals(result.getCandidates().get(10).getName(), "shard15");
    }
}
