package io.joyrpc.util;

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

/**
 * 时间片段(毫秒)
 */
public class MilliPeriod {
    // 开始时间
    protected long startTime;
    // 终止时间
    protected long endTime;

    /**
     * 构造函数
     */
    public MilliPeriod() {
    }

    /**
     * 构造函数
     *
     * @param duration
     */
    public MilliPeriod(long duration) {
        this.startTime = SystemClock.now();
        this.endTime = startTime + duration;
    }

    /**
     * 构造函数
     *
     * @param startTime
     * @param endTime
     */
    public MilliPeriod(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    /**
     * 判断是否相等
     *
     * @param duration  持续时间
     * @param deviation 误差值
     * @return
     */
    public boolean similar(final long duration, final long deviation) {
        long now = SystemClock.now();
        return (now - startTime) <= (deviation < 0 ? 0 : deviation) && (endTime - startTime) == duration;
    }

    /**
     * 返回当前事件在区间里面的比例，用于平滑处理。如果不在区间里面则返回1
     *
     * @return
     */
    public double ratio() {
        long time = SystemClock.now();
        if (time >= endTime || time < startTime) {
            return 1;
        }
        return ((double) (time - startTime)) / (endTime - startTime);
    }

    /**
     * 记录开始时间
     */
    public void begin() {
        startTime = SystemClock.now();
    }

    /**
     * 记录结束时间
     */
    public void end() {
        endTime = SystemClock.now();
    }

    /**
     * 时间
     *
     * @return
     */
    public long time() {
        return endTime - startTime;
    }

    /**
     * 判断时间是否在区间里面
     *
     * @param now
     * @return
     */
    public boolean between(long now) {
        return now >= startTime && now <= endTime;
    }

    /**
     * 判断当前时间是否在区间里面
     *
     * @return
     */
    public boolean between() {
        return between(SystemClock.now());
    }

    /**
     * 清理
     */
    public void clear() {
        startTime = 0;
        endTime = 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MilliPeriod{");
        sb.append("startTime=").append(startTime);
        sb.append(", endTime=").append(endTime);
        sb.append('}');
        return sb.toString();
    }
}
