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

import io.joyrpc.metric.TPSnapshot;

/**
 * 性能指标快照
 */
public class McTPSnapshot implements TPSnapshot {

    //总的请求数
    protected long requests;
    //成功请求数
    protected long successes;
    //失败请求数
    protected long failures;
    //可用率 默认100%
    protected double availability = 100.0d;
    //成功的记录数
    protected long records;
    //成功的数据包大小
    protected long dataSize;
    //成功请求耗费的实际
    protected int elapsedTime;
    //最大时间
    protected int max;
    //最小时间
    protected int min;
    //平均时间
    protected int avg;
    //TP40
    protected int tp30;
    //TP50
    protected int tp50;
    //TP90
    protected int tp90;
    //TP99
    protected int tp99;
    //TP999
    protected int tp999;

    public McTPSnapshot() {
    }

    public McTPSnapshot(long requests, long successes,
                        long failures, long records,
                        long dataSize, int elapsedTime,
                        int max, int min, int tp30, int tp50, int tp90, int tp99, int tp999) {
        this.requests = requests;
        this.successes = successes;
        this.failures = failures;
        this.availability = failures <= 0 ? 100.0 : ((long) ((successes * 1.0 / (successes + failures)) * 10000) / 100.0);
        this.records = records;
        this.dataSize = dataSize;
        this.elapsedTime = elapsedTime;
        this.max = max;
        this.min = min;
        this.avg = successes <= 0 ? 0 : (int) Math.ceil(elapsedTime * 1.0 / successes);
        this.tp30 = tp30;
        this.tp50 = tp50;
        this.tp90 = tp90;
        this.tp99 = tp99;
        this.tp999 = tp999;
    }

    @Override
    public long getRequests() {
        return requests;
    }

    @Override
    public long getSuccesses() {
        return successes;
    }

    @Override
    public long getFailures() {
        return failures;
    }

    @Override
    public double getAvailability() {
        return availability;
    }

    @Override
    public long getRecords() {
        return records;
    }

    @Override
    public long getDataSize() {
        return dataSize;
    }

    @Override
    public int getElapsedTime() {
        return elapsedTime;
    }

    @Override
    public int getMax() {
        return max;
    }

    @Override
    public int getMin() {
        return min;
    }

    @Override
    public int getAvg() {
        return avg;
    }

    @Override
    public int getTp30() {
        return tp30;
    }

    @Override
    public int getTp50() {
        return tp50;
    }

    @Override
    public int getTp90() {
        return tp90;
    }

    @Override
    public int getTp99() {
        return tp99;
    }

    @Override
    public int getTp999() {
        return tp999;
    }

    @Override
    public String toString() {
        StringBuilder sbuilder = new StringBuilder();
        sbuilder.append("availability::").append(this.availability).append("_")
                .append("avg::").append(this.avg).append("_")
                .append("dataSize::").append(this.dataSize).append("_")
                .append("elapsedTime::").append(this.elapsedTime).append("_")
                .append("failures::").append(this.failures).append("_")
                .append("max::").append(this.max).append("_")
                .append("min::").append(this.min).append("_")
                .append("records::").append(this.records).append("_")
                .append("requests::").append(this.requests).append("_")
                .append("successes::").append(this.successes).append("_")
                .append("tp30::").append(this.tp30).append("_")
                .append("tp50::").append(this.tp50).append("_")
                .append("tp90::").append(this.tp90).append("_")
                .append("tp99::").append(this.tp99).append("_")
                .append("tp999::").append(this.tp999);
        return sbuilder.toString();
    }

}
