package io.joyrpc.cluster.distribution.loadbalance.adaptive;

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

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.joyrpc.Plugin.ARBITER;
import static io.joyrpc.Plugin.ELECTION;

/**
 * 自适应策略
 */
public class AdaptivePolicy {

    /**
     * 综合评分算法
     */
    protected Arbiter arbiter;
    /**
     * 选择器算法
     */
    protected Election election;
    /**
     * 足够的最佳候选人列表
     */
    protected Integer enoughGoods;
    /**
     * 并发评分
     */
    protected RankScore<Long> concurrencyScore;
    /**
     * QPS评分
     */
    protected RankScore<Long> qpsScore;
    /**
     * TP评分基线
     */
    protected RankScore<Integer> tpScore;
    /**
     * 可用率评分基线
     */
    protected RankScore<Double> availabilityScore;
    /**
     * 恢复期，默认10秒
     */
    protected Long decubation;
    /**
     * 排除的机房
     */
    protected Set<String> exclusionRooms;
    /**
     * 每个裁判的系数
     */
    protected Map<String, Integer> ratios;
    /**
     * 裁决者
     */
    protected List<Judge> judges;

    /**
     * 构造函数
     */
    public AdaptivePolicy() {
    }

    /**
     * 构造函数
     *
     * @param config 自适应配置
     */
    public AdaptivePolicy(final AdaptiveConfig config, final List<Judge> judges) {
        this.arbiter = ARBITER.getOrDefault(config.getArbiter());
        this.election = ELECTION.getOrDefault(config.getElection());
        this.enoughGoods = config.getEnoughGoods();
        this.concurrencyScore = config.getConcurrencyScore();
        this.qpsScore = config.getQpsScore();
        this.tpScore = config.getTpScore();
        this.availabilityScore = config.getAvailabilityScore();
        this.decubation = config.getDecubation();
        this.exclusionRooms = config.getExclusionRooms();
        this.ratios = config.getRatios();
        this.judges = judges;
    }

    /**
     * 获取裁判系数
     *
     * @param name         名称
     * @param defaultValue 默认值
     * @return 系数
     */
    public int getRatio(final String name, final int defaultValue) {
        Integer result = ratios == null ? null : ratios.get(name);
        return result != null ? result : defaultValue;
    }

    public Arbiter getArbiter() {
        return arbiter;
    }

    public Election getElection() {
        return election;
    }

    public Integer getEnoughGoods() {
        return enoughGoods;
    }

    public RankScore<Long> getConcurrencyScore() {
        return concurrencyScore;
    }

    public RankScore<Long> getQpsScore() {
        return qpsScore;
    }

    public RankScore<Integer> getTpScore() {
        return tpScore;
    }

    public RankScore<Double> getAvailabilityScore() {
        return availabilityScore;
    }

    public Long getDecubation() {
        return decubation;
    }

    public Set<String> getExclusionRooms() {
        return exclusionRooms;
    }

    public Map<String, Integer> getRatios() {
        return ratios;
    }

    public List<Judge> getJudges() {
        return judges;
    }
}
