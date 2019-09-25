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

import io.joyrpc.cluster.Node;
import io.joyrpc.cluster.candidate.Candidature;
import io.joyrpc.context.circuit.CircuitConfiguration;
import io.joyrpc.extension.URL;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 区域分布
 */
public class RegionDistribution {
    protected static final int REJECT_RATIO = 10;
    //区域
    protected String region;
    //机房
    protected String dataCenter;
    //必须机房一致,排除空机房
    protected boolean dcExclusive;
    //每个机房热备数量
    protected int standbyPerDc;
    //机房淘汰百分比
    protected int rejectRatio;
    //同区域分片数量
    protected int size;
    //区域的分片
    protected Map<String, Map<String, DataCenterDistribution>> regions = new HashMap<>(5);
    //机房分片
    protected Map<String, DataCenterDistribution> dataCenters = new HashMap<>(10);

    /**
     * 构造函数
     *
     * @param region
     * @param dataCenter
     * @param nodes
     */
    public RegionDistribution(final String region, final String dataCenter, final List<Node> nodes) {
        this(region, dataCenter, nodes, null);
    }

    /**
     * 构造函数
     *
     * @param region
     * @param dataCenter
     * @param nodes
     * @param url
     */
    public RegionDistribution(final String region, final String dataCenter, final List<Node> nodes, final URL url) {
        this.region = region == null ? "" : region;
        this.dataCenter = dataCenter == null ? "" : dataCenter;
        this.dcExclusive = url == null ? false : url.getBoolean("dcExclusive", Boolean.FALSE);
        this.standbyPerDc = url == null ? 1 : url.getPositive("standbyPerDc", 1);
        this.rejectRatio = url == null ? REJECT_RATIO : url.getInteger("rejectRatio", REJECT_RATIO);
        this.rejectRatio = Math.max(Math.min(100, rejectRatio), 0);
        add(nodes);
    }

    /**
     * 添加分片
     *
     * @param node
     */
    protected void add(final Node node) {
        if (node == null) {
            return;
        }
        //区域匹配，机房来进行统计
        String rg = node.getRegion() == null ? "" : node.getRegion();
        String dc = node.getDataCenter() == null ? "" : node.getDataCenter();
        regions.computeIfAbsent(rg, v -> new HashMap<>()).
                computeIfAbsent(dc, o -> {
                    DataCenterDistribution v = new DataCenterDistribution(rg, dc);
                    if (!dc.isEmpty()) {
                        dataCenters.put(dc, v);
                    }
                    return v;
                }).add(node);
    }

    /**
     * 添加分片列表
     *
     * @param nodes
     */
    protected void add(final List<Node> nodes) {
        if (nodes != null) {
            for (Node node : nodes) {
                add(node);
            }
        }
    }


    /**
     * 候选机器
     *
     * @param minSize 选择的最小条数
     */
    public Candidature.Result candidate(final int minSize) {
        List<Node> candidates = new LinkedList<>();
        List<Node> standbys = new LinkedList<>();
        List<Node> localBackups = new LinkedList<>();
        List<Node> otherBackups = new LinkedList<>();
        List<Node> discards = new LinkedList<>();

        DataCenterDistribution local = !dataCenter.isEmpty() ? dataCenters.get(dataCenter) : null;
        //确保本地机房都能选择上
        int remain = local == null ? size : Math.max(minSize, local.getSize());
        //全量添加本地机房，不淘汰
        remain -= local == null ? 0 : local.candidate(candidates, localBackups, remain);
        if (!dcExclusive) {
            if (remain > 0 || candidates.isEmpty()) {
                //数量不够，或者本地机房没有，则尝试获取其它机房的节点
                LinkedHashSet<DataCenterDistribution> prefers = preferredDc(local);
                AtomicInteger count = new AtomicInteger();
                prefers.forEach(o -> count.addAndGet(o.getSize()));
                if (remain == 0 && candidates.isEmpty()) {
                    remain = count.get() / prefers.size();
                }
                int mount = remain;
                for (DataCenterDistribution v : prefers) {
                    remain -= v.candidate(candidates, otherBackups,
                            Math.max(standbyPerDc, (int) Math.ceil(mount * v.getSize() * 1.0 / count.get())));
                }
                regions.forEach((rg, v) -> v.forEach((dc, o) -> {
                    if (!prefers.contains(o)) {
                        //丢弃其它机房
                        o.candidate(candidates, discards, 0);
                    }
                }));
            } else {
                regions.forEach((rg, v) -> v.forEach((dc, o) -> {
                    if (!region.isEmpty() && region.equals(rg) && o != local) {
                        //同区域，其它机房热备
                        o.candidate(standbys, otherBackups, standbyPerDc);
                    }
                }));
            }
            //保持本地机房的优先级
            if (!otherBackups.isEmpty()) {
                //其它机房随机
                Collections.shuffle(otherBackups);
                localBackups.addAll(otherBackups);
            }
        } else {
            //其它机房都丢弃
            regions.forEach((rg, v) -> v.forEach((dc, o) -> {
                if (o != local) {
                    o.candidate(candidates, discards, 0);
                }
            }));
        }
        return new Candidature.Result(candidates, standbys, localBackups, discards);
    }

    /**
     * 跨机房，首选连接的机房
     *
     * @param local 当前机房
     */
    protected LinkedHashSet<DataCenterDistribution> preferredDc(final DataCenterDistribution local) {
        LinkedHashSet<DataCenterDistribution> result = new LinkedHashSet<>();
        DataCenterDistribution distribution;
        Map<String, DataCenterDistribution> dataCenters = this.dataCenters;
        if (!dataCenter.isEmpty()) {
            //判断当前机房的跨机房首选连接机房配置
            List<String> prefers = CircuitConfiguration.INSTANCE.get(dataCenter);
            if (prefers != null && !prefers.isEmpty()) {
                for (String prefer : prefers) {
                    distribution = dataCenters.get(prefer);
                    if (distribution != null && distribution != local) {
                        result.add(distribution);
                    }
                }
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }
        if (!region.isEmpty()) {
            //连接本区域其它机房
            dataCenters = regions.get(region);
            if (dataCenters != null && !dataCenters.isEmpty()) {
                dataCenters.forEach((dc, v) -> {
                    if (local != v) {
                        result.add(v);
                    }
                });
                return result;
            }
        }
        //连接最大节点机房所在区域的机房
        DataCenterDistribution max = null;
        for (Map.Entry<String, Map<String, DataCenterDistribution>> entry : regions.entrySet()) {
            dataCenters = entry.getValue();
            for (Map.Entry<String, DataCenterDistribution> e : dataCenters.entrySet()) {
                distribution = e.getValue();
                if (max == null || distribution.getSize() > max.getSize()) {
                    max = distribution;
                }
            }
        }
        if (max != null) {
            if (max != local) {
                result.add(max);
            }
            if (!max.getRegion().isEmpty()) {
                dataCenters = regions.get(max.getRegion());
                if (dataCenters != null && !dataCenters.isEmpty()) {
                    dataCenters.forEach((dc, v) -> {
                        if (local != v) {
                            result.add(v);
                        }
                    });
                    return result;
                }
            }
        }
        return result;
    }

    public int getSize() {
        return size;
    }
}
