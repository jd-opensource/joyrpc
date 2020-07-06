package io.joyrpc.metric.mc;

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

import io.joyrpc.metric.Clock;
import io.joyrpc.metric.TPMetric;
import io.joyrpc.metric.TPWindow;
import io.joyrpc.util.MilliPeriod;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;
import java.util.function.Function;

/**
 * TPWindow实现
 */
public class McTPWindow implements TPWindow {

    public static final Function<String, TPWindow> MILLI_WINDOW_FUNCTION = t -> new McTPWindow();

    protected volatile Histogram histogram = new Histogram();
    //当前并发数
    protected AtomicLong actives = new AtomicLong();
    //待分发数量
    protected AtomicLong distribution = new AtomicLong();
    //连续失败数量
    protected AtomicLong successiveFailures = new AtomicLong();
    //快照数据
    protected volatile McTPMetric snapshot;
    //时间区间
    protected long windowTime;
    //熔断截止时间
    protected volatile MilliPeriod brokenPeriod;
    //虚弱开始时间
    protected volatile MilliPeriod weakPeriod;
    //时钟
    protected Clock clock;
    //上次快照时间
    protected volatile long lastSnapshotTime;

    /**
     * 构造函数
     */
    public McTPWindow() {
        this(1000, Clock.MILLI);
    }

    /**
     * 构造函数
     *
     * @param windowTimeMillis 时间窗口，单位毫秒
     * @param clock            时钟
     */
    public McTPWindow(final long windowTimeMillis, final Clock clock) {
        this.clock = clock == null ? Clock.MILLI : clock;
        //把毫秒时间窗口转换成指定时间单位的时间
        this.windowTime = this.clock.getTimeUnit().convert(windowTimeMillis <= 0 ? 1000 : windowTimeMillis, TimeUnit.MILLISECONDS);
        this.lastSnapshotTime = this.clock.getTime();
        this.snapshot = new McTPMetric(successiveFailures, actives, distribution, false, new McTPSnapshot());
    }

    @Override
    public synchronized void snapshot() {
        // 时间间隔
        if (isExpired()) {
            lastSnapshotTime = clock.getTime();
            Histogram old = histogram;
            histogram = new Histogram();
            snapshot = new McTPMetric(successiveFailures, actives, distribution,
                    brokenPeriod != null && brokenPeriod.between(), old.snapshot());
        }
    }

    @Override
    public TPMetric getSnapshot() {
        return snapshot;
    }

    @Override
    public boolean isExpired() {
        return clock.getTime() - lastSnapshotTime > windowTime;
    }

    @Override
    public void setLastSnapshotTime(final long timeMillis) {
        //转换成当前时钟的数据
        this.lastSnapshotTime = clock.getTimeUnit().convert(timeMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void success(final int timeMillis) {
        success(timeMillis, 1, 0);
    }

    @Override
    public void success(final int timeMillis, final int records, final long dataSize) {
        histogram.success(timeMillis, records, dataSize);
        successiveFailures.set(0);
    }

    @Override
    public void failure() {
        histogram.failure();
        successiveFailures.incrementAndGet();
    }

    @Override
    public void resetSuccessiveFailures() {
        successiveFailures.set(0);
    }

    @Override
    public boolean hasRequest() {
        return histogram.requests.longValue() > 0;
    }

    @Override
    public AtomicLong actives() {
        return actives;
    }

    @Override
    public AtomicLong distribution() {
        return distribution;
    }

    @Override
    public long getWindowTime() {
        //转换成毫秒
        return clock.getTimeUnit().toMillis(windowTime);
    }

    @Override
    public MilliPeriod getBrokenPeriod() {
        return brokenPeriod;
    }

    @Override
    public void broken(final long duration, final long decubation) {
        MilliPeriod period = this.brokenPeriod;
        if (period != null && period.similar(duration, 100)) {
            //忽略掉100毫秒，防止并发请求大量创建
            return;
        }
        this.brokenPeriod = new MilliPeriod(duration);
        this.weakPeriod = new MilliPeriod(brokenPeriod.getEndTime(), brokenPeriod.getEndTime() + decubation);
    }

    @Override
    public void weak(final MilliPeriod period, final long duration) {
        if (weakPeriod != period) {
            //放置并发
            return;
        }
        MilliPeriod mp = this.weakPeriod;
        if (mp != null && mp.similar(duration, 100)) {
            //忽略掉100毫秒，防止并发请求大量创建
            return;
        }
        this.weakPeriod = new MilliPeriod(duration);
    }

    @Override
    public MilliPeriod getWeakPeriod() {
        return weakPeriod;
    }

    /**
     * TP性能统计缓冲器，用于计算
     */
    protected static class Histogram {
        // 默认矩阵长度，2的指数，便于取余数
        protected static final int LENGTH = 256;
        // 矩阵，最多存放length*length-1
        protected AtomicReferenceArray<AtomicLongArray> timer;
        // 超过maxTime的数据存储在这俩
        protected AtomicReference<ConcurrentMap<Integer, LongAdder>> outstrip;
        // 成功处理的记录条数
        protected LongAdder records = new LongAdder();
        // 总调用次数
        protected LongAdder requests = new LongAdder();
        // 成功调用次数
        protected LongAdder successes = new LongAdder();
        // 失败调用次数
        protected LongAdder failures = new LongAdder();
        // 数据大小
        protected LongAdder dataSize = new LongAdder();
        // 总时间
        protected LongAdder elapsedTime = new LongAdder();
        // 最大时间
        protected int maxTime;
        // 矩阵的长度
        protected int length;
        // 2的指数
        protected int exponent;

        public Histogram() {
            this(LENGTH);
        }

        public Histogram(int length) {
            if (length < 1) {
                throw new IllegalArgumentException("length must be greater than 0");
            }

            // 容量是2的指数
            int cap = 1;
            int exponent = 0;
            while (length > cap) {
                cap <<= 1;
                exponent++;
            }
            this.length = cap;
            this.exponent = exponent;
            this.timer = new AtomicReferenceArray<>(cap);
            this.outstrip = new AtomicReference<>();
            this.maxTime = cap * cap - 1;
        }

        /**
         * 对齐时间，减少数据量
         *
         * @param timeMillis 时间，单位毫秒
         * @return 时间
         */
        protected int align(final int timeMillis) {
            //[64,128) 2ms对齐;[128,256) 4ms对齐;[256-1024) 8ms对齐，[1024,) 16ms对齐
            switch (timeMillis >> 6) {
                case 0:
                    return timeMillis;
                case 1:
                    return timeMillis & 0xFFFFFFFE;
                case 2:
                case 3:
                    return timeMillis & 0xFFFFFFFC;
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                    return timeMillis & 0xFFFFFFF8;
                default:
                    return timeMillis & 0xFFFFFFF0;
            }
        }

        /**
         * 成功调用，批量增加统计信息，每次调用时间一样.
         *
         * @param timeMillis 单次调用时间，单位毫秒
         * @param records    总共记录条数
         * @param size       总共数据包大小
         */
        public void success(final int timeMillis, final int records, final long size) {
            if (timeMillis < 0) {
                // 做性能统计时间不可能为负数
                return;
            }
            //数据对齐，减少存储
            int elapse = align(timeMillis);
            elapsedTime.add(elapse);
            requests.increment();
            successes.increment();
            if (records > 0) {
                this.records.add(records);
            }
            if (size > 0) {
                dataSize.add(size);
            }

            int maxIndex = length - 1;

            if (elapse > maxTime) {
                // 超过最大时间，矩阵不能存储，采用MAP存储
                ConcurrentMap<Integer, LongAdder> exceeds = outstrip.get();
                if (exceeds == null) {
                    // 按时间排序
                    exceeds = new ConcurrentSkipListMap<>();
                    if (!outstrip.compareAndSet(null, exceeds)) {
                        exceeds = outstrip.get();
                    }
                }
                LongAdder counts = exceeds.get(elapse);
                if (counts == null) {
                    counts = new LongAdder();
                    LongAdder old = exceeds.putIfAbsent(elapse, counts);
                    if (old != null) {
                        counts = old;
                    }
                }
                counts.add(1);
            } else {
                int i = elapse >> exponent;
                int j = elapse & maxIndex;
                AtomicLongArray v = timer.get(i);
                if (v == null) {
                    v = new AtomicLongArray(length);
                    if (!timer.compareAndSet(i, null, v)) {
                        v = timer.get(i);
                    }
                }
                v.addAndGet(j, 1);
            }
        }

        /**
         * 出错，增加TP计数
         */
        public void failure() {
            failures.increment();
            requests.increment();
        }


        /**
         * 获取性能统计
         *
         * @return 性能统计
         */
        public McTPSnapshot snapshot() {
            long m_requests = requests.longValue();
            long m_successes = successes.longValue();
            long m_failures = failures.longValue();
            long m_records = records.longValue();
            long m_dataSize = dataSize.longValue();
            int m_elapsedTime = elapsedTime.intValue();
            int m_tp30 = 0;
            int m_tp50 = 0;
            int m_tp90 = 0;
            int m_tp99 = 0;
            int m_tp999 = 0;

            if (m_requests <= 0) {
                return new McTPSnapshot(m_requests, m_successes, m_failures, m_records, m_dataSize, m_elapsedTime,
                        0, 0, m_tp30, m_tp50, m_tp90, m_tp99, m_tp999);
            }

            int m_min = -1;
            int m_max = -1;
            // 计算排序位置
            int tp999 = (int) Math.floor(m_successes * 99.9 / 100);
            int tp99 = (int) Math.floor(m_successes * 99.0 / 100);
            int tp90 = (int) Math.floor(m_successes * 90.0 / 100);
            int tp50 = (int) Math.floor(m_successes * 50.0 / 100);
            int tp30 = (int) Math.floor(m_successes * 30.0 / 100);

            long count;
            long prev = 0;
            long pos = 0;
            int time;
            AtomicLongArray v;
            // 递增遍历数组
            for (int i = 0; i < length; i++) {
                v = timer.get(i);
                if (v != null) {
                    for (int j = 0; j < length; j++) {
                        // 获取该时间的数量
                        count = v.get(j);
                        if (count > 0) {
                            time = i * length + j;
                            // 当前排序位置
                            pos = prev + count;
                            if (m_min == -1) {
                                m_min = time;
                            }
                            if (m_max == -1 || time > m_max) {
                                m_max = time;
                            }
                            if (prev < tp50 && pos >= tp30) {
                                m_tp30 = time;
                            }
                            if (prev < tp50 && pos >= tp50) {
                                m_tp50 = time;
                            }
                            if (prev < tp90 && pos >= tp90) {
                                m_tp90 = time;
                            }
                            if (prev < tp99 && pos >= tp99) {
                                m_tp99 = time;
                            }
                            if (prev < tp999 && pos >= tp999) {
                                m_tp999 = time;
                            }
                            prev = pos;
                            if (prev >= m_successes) {
                                //可以提前终止了
                                break;
                            }
                        }
                    }
                    if (prev >= m_successes) {
                        //可以提前终止了
                        break;
                    }
                }
            }
            // 遍历超过最大时间的数据
            ConcurrentMap<Integer, LongAdder> exceeds = outstrip.get();
            if (exceeds != null) {
                for (Map.Entry<Integer, LongAdder> entry : exceeds.entrySet()) {
                    time = entry.getKey();
                    pos = prev + entry.getValue().longValue();
                    if (m_min == -1) {
                        m_min = time;
                    }
                    if (m_max == -1 || time > m_max) {
                        m_max = time;
                    }
                    if (prev < tp30 && pos >= tp30) {
                        m_tp50 = time;
                    }
                    if (prev < tp50 && pos >= tp50) {
                        m_tp50 = time;
                    }
                    if (prev < tp90 && pos >= tp90) {
                        m_tp90 = time;
                    }
                    if (prev < tp99 && pos >= tp99) {
                        m_tp99 = time;
                    }
                    if (prev < tp999 && pos >= tp999) {
                        m_tp999 = time;
                    }
                    prev = pos;
                }
            }

            return new McTPSnapshot(m_requests, m_successes, m_failures, m_records, m_dataSize, m_elapsedTime,
                    m_max, m_min, m_tp30, m_tp50, m_tp90, m_tp99, m_tp999);
        }

    }

}
