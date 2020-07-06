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

import io.joyrpc.extension.Parametric;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.util.StringUtils.*;

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
     * 构造函数
     *
     * @param source 源自适应配置
     */
    public AdaptiveConfig(final AdaptiveConfig source) {
        if (source != null) {
            this.arbiter = source.arbiter;
            this.election = source.election;
            this.enoughGoods = source.enoughGoods;
            this.concurrencyScore = source.concurrencyScore;
            this.qpsScore = source.qpsScore;
            this.tpScore = source.tpScore;
            this.availabilityScore = source.availabilityScore;
            this.decubation = source.decubation;
            this.exclusionRooms = source.exclusionRooms;
            this.ratios = source.ratios;
        }
    }

    /**
     * 构造函数
     *
     * @param url url
     */
    public AdaptiveConfig(final Parametric url) {
        if (url != null) {
            String[] rooms = split(url.getString(ADAPTIVE_EXCLUSION_ROOMS), SEMICOLON_COMMA_WHITESPACE);
            arbiter = url.getString(ADAPTIVE_ARBITER);
            election = url.getString(ADAPTIVE_ELECTION);
            enoughGoods = url.getInteger(ADAPTIVE_ENOUGH_GOODS);
            decubation = url.getLong(ADAPTIVE_DECUBATION);
            exclusionRooms = rooms == null ? null : new HashSet<>(Arrays.asList(rooms));
            concurrencyScore = computeConcurrencyScore(url);
            qpsScore = computeQpsScore(url);
            tpScore = computeTpScore(url);
            availabilityScore = computeAvailabilityScore(url);
        }
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
     * 计算集群并发评分
     *
     * @param actives 并发数
     * @return 并发评分
     */
    public static RankScore<Long> computeConcurrencyScore(final long[] actives) {
        return computeConcurrencyScore(median(actives));
    }

    /**
     * 计算集群可用性评分
     *
     * @param actives 可用性
     * @return 可用性评分
     */
    public static RankScore<Double> computeAvailabilityScore(final long[] actives) {
        return computeAvailabilityScore(median(actives) / 1000.0);
    }

    /**
     * 计算集群QPS评分
     *
     * @param requests 请求数
     * @return QPS评分
     */
    public static RankScore<Long> computeQpsScore(final long[] requests) {
        return computeQpsScore(median(requests));
    }

    /**
     * 合并配置
     *
     * @param config 配置
     */
    public void merge(final AdaptiveConfig config) {
        if (config == null) {
            return;
        }
        arbiter = config.arbiter == null || config.arbiter.isEmpty() ? arbiter : config.arbiter;
        election = config.election == null || config.election.isEmpty() ? election : config.election;
        enoughGoods = config.enoughGoods == null ? enoughGoods : config.enoughGoods;
        decubation = config.decubation == null ? decubation : config.decubation;
        concurrencyScore = config.concurrencyScore == null ? concurrencyScore : config.concurrencyScore;
        qpsScore = config.qpsScore == null ? qpsScore : config.qpsScore;
        tpScore = config.tpScore == null ? tpScore : config.tpScore;
        availabilityScore = config.availabilityScore == null ? availabilityScore : config.availabilityScore;
        exclusionRooms = config.exclusionRooms == null ? exclusionRooms : config.exclusionRooms;
        ratios = config.ratios == null ? ratios : config.ratios;
    }

    /**
     * 计算TP评分
     *
     * @param fair
     */
    public static RankScore<Integer> computeTpScore(final int fair) {
        switch (fair) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                return new RankScore<>(4, 8, 12);
            case 5:
            case 6:
            case 7:
            case 8:
                return new RankScore<>(8, 12, 16);
            default:
                return new RankScore<>((int) (fair * 1.2), (int) (fair * 1.5), fair * 2);
        }
    }

    /**
     * 计算TP评分
     *
     * @param url 参数
     */
    public static RankScore<Integer> computeTpScore(final Parametric url) {
        Integer fair = url.getInteger(ADAPTIVE_TP_FAIR);
        Integer poor = url.getInteger(ADAPTIVE_TP_POOR);
        Integer disable = url.getInteger(ADAPTIVE_TP_DISABLE);
        if (fair == null && poor == null && disable == null) {
            return null;
        } else if (fair != null && poor == null && disable == null) {
            return computeTpScore(fair);
        } else {
            return new RankScore<>(fair, poor, disable);
        }
    }

    /**
     * 计算可用率评分
     *
     * @param fair 可用率一般阈值
     */
    public static RankScore<Double> computeAvailabilityScore(final double fair) {
        return new RankScore<>(fair - 0.1, fair - 1d, fair - 5d);
    }

    /**
     * 计算可用率评分
     *
     * @param url 参数
     */
    public static RankScore<Double> computeAvailabilityScore(Parametric url) {
        Double fair = url.getDouble(ADAPTIVE_AVAILABILITY_FAIR);
        Double poor = url.getDouble(ADAPTIVE_AVAILABILITY_POOR);
        Double disable = url.getDouble(ADAPTIVE_AVAILABILITY_DISABLE);
        if (fair == null && poor == null && disable == null) {
            return null;
        } else if (fair != null && poor == null && disable == null) {
            return computeAvailabilityScore(fair);
        } else {
            return new RankScore<>(fair, poor, disable);
        }
    }

    /**
     * 计算并发评分
     *
     * @param fair 并发一般阈值
     */
    public static RankScore<Long> computeConcurrencyScore(final long fair) {
        if (fair <= 0) {
            return new RankScore<>(100L, null, null);
        }
        return new RankScore<>(fair, fair * 2, null);
    }

    /**
     * 计算并发评分
     */
    public static RankScore<Long> computeConcurrencyScore(Parametric url) {
        Long fair = url.getLong(ADAPTIVE_CONCURRENCY_FAIR);
        Long poor = url.getLong(ADAPTIVE_CONCURRENCY_POOR);
        if (fair == null && poor == null) {
            return null;
        } else if (fair != null && poor == null) {
            return computeConcurrencyScore(fair);
        } else {
            return new RankScore<>(fair, poor, null);
        }
    }

    /**
     * 计算Qps评分
     *
     * @param fair
     */
    public static RankScore<Long> computeQpsScore(final long fair) {
        if (fair <= 0) {
            return new RankScore<>(1000L, null, null);
        }
        return new RankScore<>(fair, fair * 2, null);
    }

    /**
     * 计算并发评分
     */
    public static RankScore<Long> computeQpsScore(Parametric url) {
        Long fair = url.getLong(ADAPTIVE_QPS_FAIR);
        Long poor = url.getLong(ADAPTIVE_QPS_POOR);
        if (fair == null && poor == null) {
            return null;
        } else if (fair != null && poor == null) {
            return computeQpsScore(fair);
        } else {
            return new RankScore<>(fair, poor, null);
        }
    }

    /**
     * 获取中位数，做了优化
     *
     * @param nums
     * @return
     */
    public static long median(final long[] nums) {
        switch (nums.length) {
            case 1:
                return nums[0];
            case 2:
                return (nums[0] + nums[1]) / 2;
            default:
                return partition(nums, 0, nums.length - 1);
        }
    }

    /**
     * 利用快排查找
     *
     * @param nums  数组
     * @param start 开始位置
     * @param end   结束位置
     * @return 中位数
     */
    protected static long partition(final long[] nums, final int start, final int end) {
        int left = start;
        int right = end + 1;

        long point = nums[start];
        long tmp;
        while (true) {
            while (left < right && nums[--right] >= point) {
            }
            while (left < right && nums[++left] <= point) {
            }
            if (left == right) {
                break;
            } else {
                tmp = nums[left];
                nums[left] = nums[right];
                nums[right] = tmp;
            }
        }
        nums[start] = nums[left];
        nums[left] = point;

        int median = (nums.length - 1) / 2;
        if (left == median) {
            return nums[left];
        } else if (left > median) {
            return partition(nums, start, left - 1);
        } else {
            return partition(nums, left + 1, end);
        }
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
