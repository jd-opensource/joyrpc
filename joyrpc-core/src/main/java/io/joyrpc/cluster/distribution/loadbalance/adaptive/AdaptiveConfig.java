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

import io.joyrpc.cluster.Node;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * 自适应配置
 */
public class AdaptiveConfig implements Serializable, Cloneable {

    private static final long serialVersionUID = 7863244492270952743L;

    /**
     * 综合评分算法
     */
    protected String arbiter;
    /**
     * 选择器算法
     */
    protected String election;
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
     * 构造函数
     */
    public AdaptiveConfig() {
    }

    /**
     * 构造函数
     *
     * @param arbiter
     * @param election
     * @param enoughGoods
     * @param concurrencyScore
     * @param qpsScore
     * @param tpScore
     * @param availabilityScore
     * @param decubation
     * @param exclusionRooms
     * @param ratios
     */
    public AdaptiveConfig(final String arbiter,
                          final String election,
                          final Integer enoughGoods,
                          final RankScore<Long> concurrencyScore,
                          final RankScore<Long> qpsScore,
                          final RankScore<Integer> tpScore,
                          final RankScore<Double> availabilityScore,
                          final Long decubation,
                          final Set<String> exclusionRooms,
                          final Map<String, Integer> ratios) {
        this.arbiter = arbiter;
        this.election = election;
        this.enoughGoods = enoughGoods;
        this.concurrencyScore = concurrencyScore;
        this.qpsScore = qpsScore;
        this.tpScore = tpScore;
        this.availabilityScore = availabilityScore;
        this.decubation = decubation;
        this.exclusionRooms = exclusionRooms;
        this.ratios = ratios;
    }

    /**
     * 获取裁判系数
     *
     * @param name
     * @param defaultValue
     * @return
     */
    public int getRatio(final String name, final int defaultValue) {
        Integer result = ratios == null ? null : ratios.get(name);
        return result != null ? result : defaultValue;
    }

    public String getArbiter() {
        return arbiter;
    }

    public void setArbiter(String arbiter) {
        this.arbiter = arbiter;
    }

    public String getElection() {
        return election;
    }

    public void setElection(String election) {
        this.election = election;
    }

    public Integer getEnoughGoods() {
        return enoughGoods;
    }

    public void setEnoughGoods(Integer enoughGoods) {
        this.enoughGoods = enoughGoods;
    }

    public RankScore<Long> getConcurrencyScore() {
        return concurrencyScore;
    }

    public void setConcurrencyScore(RankScore<Long> concurrencyScore) {
        this.concurrencyScore = concurrencyScore;
    }

    public RankScore<Long> getQpsScore() {
        return qpsScore;
    }

    public void setQpsScore(RankScore<Long> qpsScore) {
        this.qpsScore = qpsScore;
    }

    public RankScore<Integer> getTpScore() {
        return tpScore;
    }

    public void setTpScore(RankScore<Integer> tpScore) {
        this.tpScore = tpScore;
    }

    public RankScore<Double> getAvailabilityScore() {
        return availabilityScore;
    }

    public void setAvailabilityScore(RankScore<Double> availabilityScore) {
        this.availabilityScore = availabilityScore;
    }

    public Long getDecubation() {
        return decubation;
    }

    public void setDecubation(Long decubation) {
        this.decubation = decubation;
    }

    public Set<String> getExclusionRooms() {
        return exclusionRooms;
    }

    public void setExclusionRooms(Set<String> exclusionRooms) {
        this.exclusionRooms = exclusionRooms;
    }

    public Map<String, Integer> getRatios() {
        return ratios;
    }

    public void setRatios(Map<String, Integer> ratios) {
        this.ratios = ratios;
    }

    public AdaptiveConfig clone() {
        try {
            return (AdaptiveConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }


    /**
     * 是否要排除某个服务
     *
     * @param node
     * @return
     */
    public boolean exclude(final Node node) {
        //过滤掉排除的服务和机房
        return node == null ||
                exclusionRooms != null && node.getDataCenter() != null && exclusionRooms.contains(node.getDataCenter());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AdaptiveConfig{");
        sb.append("arbiter='").append(arbiter).append('\'');
        sb.append(", election='").append(election).append('\'');
        sb.append(", enoughGoods=").append(enoughGoods);
        sb.append(", concurrencyScore=").append(concurrencyScore);
        sb.append(", qpsScore=").append(qpsScore);
        sb.append(", tpScore=").append(tpScore);
        sb.append(", availabilityScore=").append(availabilityScore);
        sb.append(", decubation=").append(decubation);
        sb.append(", exclusionRooms=").append(exclusionRooms);
        sb.append(", ratios=").append(ratios);
        sb.append('}');
        return sb.toString();
    }
}
