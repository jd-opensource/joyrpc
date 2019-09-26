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
import io.joyrpc.context.circuit.CircuitConfiguration;
import io.joyrpc.extension.URL;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class RegionCandidatureTest {

    @Test
    public void test() {
        String name = "test";
        URL url = URL.valueOf("joyrpc://127.0.0.1/xxx.xxx.xxx.xxx");
        List<Node> nodes = new LinkedList<>();
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard1", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.1"), 1, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard2", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.2"), 2, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard3", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.3"), 3, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard4", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.4"), 4, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard5", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.5"), 5, ShardState.CONNECTED)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard6", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.6"), 6, ShardState.WEAK)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard7", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.7"), 7, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard8", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.8"), 8, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard9", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.9"), 9, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard10", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.10"), 10, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard11", "huabei", "ht", "joyrpc", URL.valueOf("joyrpc://192.168.1.11"), 11, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard12", "huabei", "ht", "joyrpc", URL.valueOf("joyrpc://192.168.1.12"), 12, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard13", "huabei", "ht", "joyrpc", URL.valueOf("joyrpc://192.168.1.13"), 13, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard14", "huabei", "ht", "joyrpc", URL.valueOf("joyrpc://192.168.1.14"), 14, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard15", "huabei", "ht", "joyrpc", URL.valueOf("joyrpc://192.168.1.15"), 15, ShardState.CONNECTED)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard16", "huanan", "gz", "joyrpc", URL.valueOf("joyrpc://192.168.1.16"), 16, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard17", "huanan", "gz", "joyrpc", URL.valueOf("joyrpc://192.168.1.17"), 17, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard18", "huanan", "gz", "joyrpc", URL.valueOf("joyrpc://192.168.1.18"), 18, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard19", "huanan", "gz", "joyrpc", URL.valueOf("joyrpc://192.168.1.19"), 19, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard20", "huanan", "gz", "joyrpc", URL.valueOf("joyrpc://192.168.1.20"), 20, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard21", "", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.21"), 21, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard22", "", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.22"), 22, ShardState.INITIAL)));

        RegionCandidature candidature = new RegionCandidature();

        Candidate.Builder builder = Candidate.builder().region(new Region.DefaultRegion("huabei", "lf")).nodes(nodes);
        Candidature.Result result = candidature.candidate(null, builder.size(5).build());
        Assert.assertEquals(result.getCandidates().size(), 10);
        Assert.assertEquals(result.getStandbys().size(), 1);
        Assert.assertEquals(result.getBackups().size(), 4);
        Assert.assertEquals(result.getDiscards().size(), 7);
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

    @Test
    public void testEmptyDc() {
        String name = "test";
        URL url = URL.valueOf("joyrpc://127.0.0.1/xxx.xxx.xxx.xxx");
        List<Node> nodes = new LinkedList<>();
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard1", "huabei", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.1"), 1, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard2", "huabei", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.2"), 2, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard3", "huabei", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.3"), 3, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard4", "huabei", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.4"), 4, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard5", "huabei", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.5"), 5, ShardState.CONNECTED)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard6", "huabei", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.6"), 6, ShardState.WEAK)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard7", "huabei", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.7"), 7, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard8", "huabei", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.8"), 8, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard9", "huanan", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.9"), 9, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard10", "huanan", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.10"), 10, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard11", "huanan", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.11"), 11, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard12", "huanan", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.22"), 12, ShardState.INITIAL)));

        RegionCandidature candidature = new RegionCandidature();

        Candidate.Builder builder = Candidate.builder().region(new Region.DefaultRegion("huabei", "")).nodes(nodes);
        Candidature.Result result = candidature.candidate(null, builder.size(5).build());
        Assert.assertEquals(result.getCandidates().size(), 8);
        Assert.assertEquals(result.getStandbys().size(), 0);
        Assert.assertEquals(result.getBackups().size(), 0);
    }

    @Test
    public void testEmptyRegionDc() {
        String name = "test";
        URL url = URL.valueOf("joyrpc://127.0.0.1/xxx.xxx.xxx.xxx");
        List<Node> nodes = new LinkedList<>();
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard1", "huabei", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.1"), 1, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard2", "huabei", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.2"), 2, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard3", "huabei", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.3"), 3, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard4", "huabei", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.4"), 4, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard5", "huabei", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.5"), 5, ShardState.CONNECTED)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard6", "huabei", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.6"), 6, ShardState.WEAK)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard7", "huabei", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.7"), 7, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard8", "huabei", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.8"), 8, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard9", "huanan", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.9"), 9, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard10", "huanan", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.10"), 10, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard11", "huanan", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.11"), 11, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard12", "huanan", "", "joyrpc", URL.valueOf("joyrpc://192.168.1.22"), 12, ShardState.INITIAL)));

        RegionCandidature candidature = new RegionCandidature();

        Candidate.Builder builder = Candidate.builder().region(new Region.DefaultRegion("", "")).nodes(nodes);
        Candidature.Result result = candidature.candidate(null, builder.size(5).build());
        Assert.assertEquals(result.getCandidates().size(), 8);
        Assert.assertEquals(result.getStandbys().size(), 0);
        Assert.assertEquals(result.getBackups().size(), 0);
    }

    @Test
    public void testPrefer() {
        String name = "test";
        URL url = URL.valueOf("joyrpc://127.0.0.1/xxx.xxx.xxx.xxx");
        List<Node> nodes = new LinkedList<>();
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard1", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.1"), 1, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard2", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.2"), 2, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard3", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.3"), 3, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard4", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.4"), 4, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard5", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.5"), 5, ShardState.CONNECTED)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard6", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.6"), 6, ShardState.WEAK)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard7", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.7"), 7, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard8", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.8"), 8, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard9", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.9"), 9, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard10", "huabei", "lf", "joyrpc", URL.valueOf("joyrpc://192.168.1.10"), 10, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard11", "huabei", "ht", "joyrpc", URL.valueOf("joyrpc://192.168.1.11"), 11, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard12", "huabei", "ht", "joyrpc", URL.valueOf("joyrpc://192.168.1.12"), 12, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard13", "huabei", "ht", "joyrpc", URL.valueOf("joyrpc://192.168.1.13"), 13, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard14", "huabei", "ht", "joyrpc", URL.valueOf("joyrpc://192.168.1.14"), 14, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard15", "huabei", "ht", "joyrpc", URL.valueOf("joyrpc://192.168.1.15"), 15, ShardState.CONNECTED)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard16", "huanan", "gz", "joyrpc", URL.valueOf("joyrpc://192.168.1.16"), 16, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard17", "huanan", "gz", "joyrpc", URL.valueOf("joyrpc://192.168.1.17"), 17, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard18", "huanan", "gz", "joyrpc", URL.valueOf("joyrpc://192.168.1.18"), 18, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard19", "huanan", "gz", "joyrpc", URL.valueOf("joyrpc://192.168.1.19"), 19, ShardState.INITIAL)));
        nodes.add(new Node(name, url, new Shard.DefaultShard("shard20", "huanan", "gz", "joyrpc", URL.valueOf("joyrpc://192.168.1.20"), 20, ShardState.INITIAL)));

        RegionCandidature candidature = new RegionCandidature();

        Map<String, List<String>> map = new HashMap<>();
        map.put("lf1", Arrays.asList("lf"));
        CircuitConfiguration.INSTANCE.update(map);
        Candidate.Builder builder = Candidate.builder().region(new Region.DefaultRegion("huabei1", "lf1")).nodes(nodes);
        Candidature.Result result = candidature.candidate(null, builder.size(5).build());
        Assert.assertEquals(result.getCandidates().size(), 10);
        Assert.assertEquals(result.getStandbys().size(), 0);
        Assert.assertEquals(result.getBackups().size(), 0);

    }
}
