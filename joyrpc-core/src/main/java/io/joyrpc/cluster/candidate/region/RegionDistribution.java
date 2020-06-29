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
import java.util.function.Consumer;
import java.util.function.Predicate;

import static io.joyrpc.constants.Constants.REGION_DCEXCLUSIVE;
import static io.joyrpc.constants.Constants.REGION_STANDBYPERDC;

/**
 * 区域分布
 */
public class RegionDistribution {
    /**
     * 区域
     */
    protected String region;
    /**
     * 机房
     */
    protected String dataCenter;
    /**
     * 排除其它机房标识
     */
    protected boolean dcExclusive;
    /**
     * 本区域，其它机房热备节点数
     */
    protected int standbyPerDc;
    /**
     * 区域的分片
     */
    protected Map<String, Map<String, DataCenterDistribution>> regions = new HashMap<>(5);
    /**
     * 机房分片
     */
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
        this.dcExclusive = url == null ? false : url.getBoolean(REGION_DCEXCLUSIVE);
        this.standbyPerDc = url == null ? 1 : url.getInteger(REGION_STANDBYPERDC);
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
     * @return 结果
     */
    public Candidature.Result candidate(final int minSize) {
        List<Node> candidates = new LinkedList<>();
        List<Node> standbys = new LinkedList<>();
        List<Node> localBackups = new LinkedList<>();
        List<Node> otherBackups = new LinkedList<>();
        List<Node> discards = new LinkedList<>();

        DataCenterDistribution local = !dataCenter.isEmpty() ? dataCenters.get(dataCenter) : null;
        //确保本地机房都能选择上
        int remain = local == null ? minSize : Math.max(minSize, local.getSize());
        //全量添加本地机房，不淘汰
        remain -= local == null ? 0 : local.candidate(candidates, localBackups, remain);
        if (!dcExclusive) {
            //本地机房没有，则尝试获取其它机房的节点，包括首选或同区域的或最多节点机房所在区域的
            //本地机房有，则尝试首选或同区域的补充
            LinkedHashSet<DataCenterDistribution> prefers = candidates.isEmpty() ? preferredOrNeighbourOrMaxDc(local) : preferredOrNeighbourDc(local);
            int count = getCount(prefers);
            if (count > 0) {
                if (minSize > 0 && remain <= 0) {
                    //如果本地机房满足指定的数量，则补充热备
                    remain = 0;
                } else if (candidates.isEmpty()) {
                    //本地机房没有，则按照就近机房平均值按比例补充
                    remain = count / prefers.size();
                } else if (minSize <= 0 && remain > 0) {
                    //如果没有指定最小数量，则获取多个机房的平均值作为最小数量，防止当前机房的服务节点数量过小造成压力
                    remain = (count + local.getSize()) / (prefers.size() + 1) - local.getSize();
                }
                if (remain > 0) {
                    int mount = remain;
                    //按比例补充
                    for (DataCenterDistribution v : prefers) {
                        remain -= v.candidate(candidates, otherBackups, (int) Math.ceil(mount * v.getSize() * 1.0 / count));
                    }
                } else {
                    //热备
                    prefers.forEach(o -> o.candidate(standbys, otherBackups, standbyPerDc));
                }
            }
            //丢弃其它机房
            foreach(o -> o != local && !prefers.contains(o), o -> o.candidate(candidates, discards, 0));
            //保持本地机房的优先级
            if (!otherBackups.isEmpty()) {
                //其它机房随机
                Collections.shuffle(otherBackups);
                localBackups.addAll(otherBackups);
            }
        } else {
            //其它机房都丢弃
            foreach(o -> o != local, o -> o.candidate(candidates, discards, 0));
        }
        return new Candidature.Result(candidates, standbys, localBackups, discards);
    }

    /**
     * 迭代机房分布
     *
     * @param predicate
     * @param consumer
     */
    protected void foreach(final Predicate<DataCenterDistribution> predicate,
                           final Consumer<DataCenterDistribution> consumer) {
        if (consumer == null) {
            return;
        }
        regions.forEach((rg, v) -> v.forEach((dc, o) -> {
            if (predicate == null || predicate.test(o)) {
                consumer.accept(o);
            }
        }));
    }

    /**
     * 获取节点数量
     *
     * @param dcs 机房分布
     * @return 节点数量
     */
    protected int getCount(final Set<DataCenterDistribution> dcs) {
        int result = 0;
        for (DataCenterDistribution dc : dcs) {
            result += dc.getSize();
        }
        return result;
    }

    /**
     * 跨机房，首选或同区域的机房
     *
     * @param local 当前机房
     * @return 目标机房
     */
    protected LinkedHashSet<DataCenterDistribution> preferredOrNeighbourDc(final DataCenterDistribution local) {
        LinkedHashSet<DataCenterDistribution> neighbours = new LinkedHashSet<>(8);
        LinkedHashSet<DataCenterDistribution> others = new LinkedHashSet<>(8);
        //首选
        preferredDc(local, neighbours, others);
        if (neighbours.isEmpty()) {
            //首选里面没有本区域的，则从新计算本区域
            neighbourDc(local, neighbours);
        }
        if (!neighbours.isEmpty()) {
            return neighbours;
        } else {
            return others;
        }
    }

    /**
     * 跨机房，首选或同区域或最多节点机房所在区域的机房
     *
     * @param local 当前机房
     */
    protected LinkedHashSet<DataCenterDistribution> preferredOrNeighbourOrMaxDc(final DataCenterDistribution local) {
        //跨机房，首选或同区域的机房
        LinkedHashSet<DataCenterDistribution> result = preferredOrNeighbourDc(local);
        if (result.isEmpty()) {
            //最多节点机房所在区域的机房
            maxDc(local, result);
        }
        return result;
    }

    /**
     * 跨机房，首选连接的机房
     *
     * @param local      当前机房
     * @param neighbours 本区域首选集合
     * @param others     其它区域首选集合
     */
    protected void preferredDc(final DataCenterDistribution local, final LinkedHashSet<DataCenterDistribution> neighbours,
                               final LinkedHashSet<DataCenterDistribution> others) {
        if (!dataCenter.isEmpty()) {
            //判断当前机房的跨机房首选连接机房配置
            List<String> prefers = CircuitConfiguration.CIRCUIT.get(dataCenter);
            if (prefers != null && !prefers.isEmpty()) {
                DataCenterDistribution distribution;
                for (String prefer : prefers) {
                    distribution = dataCenters.get(prefer);
                    if (distribution != null && distribution != local) {
                        //同区域
                        if (!region.isEmpty() && region.equals(distribution.getRegion())) {
                            //首选的同区域机房
                            neighbours.add(distribution);
                        } else {
                            //首选的其它区域机房
                            others.add(distribution);
                        }
                    }
                }
            }
        }
    }

    /**
     * 跨机房，同区域的机房
     *
     * @param local 当前机房
     * @param dcds
     */
    protected void neighbourDc(final DataCenterDistribution local, final LinkedHashSet<DataCenterDistribution> dcds) {
        if (!region.isEmpty()) {
            //连接本区域其它机房
            Map<String, DataCenterDistribution> dataCenters = regions.get(region);
            if (dataCenters != null && !dataCenters.isEmpty()) {
                dataCenters.forEach((dc, v) -> {
                    if (local != v) {
                        dcds.add(v);
                    }
                });
            }
        }
    }

    /**
     * 跨机房，最大节点机房所在区域的机房
     *
     * @param local 当前机房
     * @param dcds
     */
    protected void maxDc(final DataCenterDistribution local, final LinkedHashSet<DataCenterDistribution> dcds) {
        DataCenterDistribution distribution;
        Map<String, DataCenterDistribution> dataCenters;
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
                dcds.add(max);
            }
            if (!max.getRegion().isEmpty()) {
                dataCenters = regions.get(max.getRegion());
                if (dataCenters != null && !dataCenters.isEmpty()) {
                    dataCenters.forEach((dc, v) -> {
                        if (local != v) {
                            dcds.add(v);
                        }
                    });
                }
            }
        }
    }
}
