package io.joyrpc.option;

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

import java.util.concurrent.atomic.AtomicLong;

/**
 * 并发数指标
 */
public class Concurrency {

    /**
     * 最大并发数
     */
    protected int max;

    /**
     * 活动并发
     */
    protected AtomicLong actives = new AtomicLong();

    public Concurrency(int max) {
        this.max = max;
    }

    public int getMax() {
        return max;
    }

    /**
     * 当前并发数
     *
     * @return 并发数
     */
    public long getActives() {
        return actives.get();
    }

    /**
     * 增加
     */
    public void add() {
        actives.incrementAndGet();
    }

    /**
     * 减少并发数
     */
    public void decrement() {
        actives.decrementAndGet();
    }

    /**
     * 唤醒
     */
    public void wakeup() {
        synchronized (this) {
            // 调用结束 通知等待的人
            notifyAll();
        }
    }

    /**
     * 等到
     *
     * @param time 时间
     * @return 成功标识
     */
    public boolean await(final long time) {
        if (time <= 0) {
            return true;
        }
        synchronized (this) {
            try {
                // 等待执行
                wait(time);
                return true;
            } catch (InterruptedException e) {
                return false;
            }
        }
    }

}
