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
    //匹配空区域
    protected boolean matchEmptyRegion;
    //匹配空机房
    protected boolean matchEmptyDc;
    //必须机房一致,排除空机房
    protected boolean dcExclusive;
    //每个机房热备数量
    protected int standbyPerDc;
    //机房淘汰百分比
    protected int rejectRatio;
    //同区域分片数量
    protected int size;
    //同区域其它机房分片
    protected Map<String, DataCenterDistribution> otherDc = new HashMap<>(5);
    //同区域没有机房信息的分片
    protected DataCenterDistribution emptyDc = new DataCenterDistribution("");
    //没有区域和机房信息的分片
    protected DataCenterDistribution emptyRegionDc = new DataCenterDistribution("");
    //本地机房分片
    protected DataCenterDistribution localDc;
    //其它区域的分片
    protected List<Node> discards = new LinkedList<>();

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
        this.matchEmptyRegion = url == null ? true : url.getBoolean("matchEmptyRegion", Boolean.TRUE);
        this.matchEmptyDc = url == null ? true : url.getBoolean("matchEmptyDc", Boolean.TRUE);
        this.dcExclusive = url == null ? false : url.getBoolean("dcExclusive", Boolean.FALSE);
        this.standbyPerDc = url == null ? 1 : url.getPositive("standbyPerDc", 1);
        this.rejectRatio = url == null ? REJECT_RATIO : url.getInteger("rejectRatio", REJECT_RATIO);
        this.rejectRatio = Math.max(Math.min(100, rejectRatio), 0);
        this.localDc = new DataCenterDistribution(dataCenter);
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
        //区域匹配
        if (!region.isEmpty() && region.equals(rg) || matchEmptyRegion && (region.isEmpty() || rg.isEmpty())) {
            String dc = node.getDataCenter() == null ? "" : node.getDataCenter();
            if (dc.isEmpty()) {
                //空机房
                if (matchEmptyDc) {
                    if (rg.isEmpty()) {
                        //区域为空
                        emptyRegionDc.add(node);
                    } else {
                        //区域相同
                        emptyDc.add(node);
                    }
                }
            } else if (dataCenter.equals(dc)) {
                //本地机房
                localDc.add(node);
            } else {
                //其它机房
                otherDc.computeIfAbsent(dc, o -> new DataCenterDistribution(o)).add(node);
            }
            //分片数量
            size++;
        } else {
            //其它区域丢弃
            discards.add(node);
        }
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

        DataCenterDistribution prefer = preferredDc();
        //确保本地机房都能选择上
        int remain = prefer.getSize() == 0 ? size : Math.max(minSize, prefer.getSize());
        //全量添加本地机房，不淘汰
        remain -= prefer.candidate(candidates, localBackups, remain);
        if (!dcExclusive) {
            //不排除其它机房
            if (remain > 0) {
                //同区域其它机房分片数量总和
                AtomicInteger others = new AtomicInteger();
                otherDc.values().forEach(o -> others.addAndGet(o.getSize()));
                int count = remain;
                //遍历同区域其它机房，按占比进行补充，如果小于热备节点数量，则按照热备数量进行选择
                for (DataCenterDistribution v : otherDc.values()) {
                    remain -= v.candidate(candidates, otherBackups,
                            Math.max(standbyPerDc, (int) Math.ceil(count * v.getSize() * 1.0 / others.get())));
                }
                //从本区域无机房补充
                remain -= emptyDc.candidate(candidates, otherBackups, remain);
                //从无区域无机房补充
                emptyRegionDc.candidate(candidates, discards, remain);
            } else if (candidates.isEmpty() && emptyDc.getSize() > 0) {
                //没有识别出机房的情况，看看是否识别出来了区域，尝试连接本区域所有节点
                remain = emptyDc.candidate(candidates, otherBackups, minSize > 0 ? minSize : 50);
                //从无区域无机房补充
                emptyRegionDc.candidate(candidates, discards, remain);
            } else {
                //只有本地机房，不限定机房，则从其它机房补充热备
                otherDc.values().forEach(v -> v.candidate(standbys, otherBackups, standbyPerDc));
                //本区域没有机房的作为冷备
                emptyDc.candidate(candidates, otherBackups, 0);
                //无区域无机房的丢弃
                emptyRegionDc.candidate(candidates, discards, 0);
            }
            //保持本地机房的优先级
            if (!otherBackups.isEmpty()) {
                //其它机房随机
                Collections.shuffle(otherBackups);
                localBackups.addAll(otherBackups);
            }
        } else {
            //其它机房都丢弃
            otherDc.values().forEach(o -> o.candidate(candidates, discards, 0));
            emptyDc.candidate(candidates, discards, 0);
            emptyRegionDc.candidate(candidates, discards, 0);
        }
        //补充丢弃的其它区域节点
        discards.addAll(this.discards);
        return new Candidature.Result(candidates, standbys, localBackups, discards);
    }

    /**
     * 首选的全部连接的机房
     */
    protected DataCenterDistribution preferredDc() {
        if (localDc.getSize() == 0 && !dcExclusive) {
            //本地机房没有，选择最大分片数的其它机房作为本地机房来计算
            String other = null;
            int max = 0;
            DataCenterDistribution distribution;
            for (Map.Entry<String, DataCenterDistribution> entry : otherDc.entrySet()) {
                distribution = entry.getValue();
                if (distribution.getSize() > max) {
                    max = distribution.getSize();
                    other = entry.getKey();
                }
            }
            if (other != null) {
                return otherDc.remove(other);
            }
        }
        return localDc;
    }

    public int getSize() {
        return size;
    }
}
