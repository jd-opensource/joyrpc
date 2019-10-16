package io.joyrpc.transport.heartbeat.simple;

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

import io.joyrpc.thread.NamedThreadFactory;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.heartbeat.HeartbeatManager;
import io.joyrpc.transport.heartbeat.HeartbeatStrategy;
import io.joyrpc.transport.heartbeat.HeartbeatStrategy.HeartbeatMode;
import io.joyrpc.transport.heartbeat.HeartbeatTrigger;
import io.joyrpc.util.Daemon;
import io.joyrpc.util.Shutdown;
import io.joyrpc.util.SystemClock;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * 简单心跳管理
 */
public class SimpleHeartbeatManager implements HeartbeatManager {
    /**
     * 名称
     */
    protected String name;
    /**
     * 检查现场
     */
    protected Daemon daemon;
    /**
     * 线程池
     */
    protected ExecutorService executorService;

    /**
     * 触发器
     */
    protected Map<Channel, TriggerInfo> timingTriggers = new ConcurrentHashMap<>();

    /**
     * 构造函数
     *
     * @param name
     */
    public SimpleHeartbeatManager(final String name) {
        this.name = name;
        //多线程执行触发器
        executorService = Executors.newFixedThreadPool(3, new NamedThreadFactory("heartbeat-" + name, true));
        //单线程检查超时
        daemon = Daemon.builder().name("heartbeat-" + name).interval(100L)
                .condition(() -> !Shutdown.isShutdown())
                .runnable(this::trigger)
                .build();
        daemon.start();
        Shutdown.addHook(executorService::shutdown);
    }

    /**
     * 检查超时并触发心跳
     */
    protected void trigger() {
        TriggerInfo triggerInfo;
        //定时遍历
        for (Entry<Channel, TriggerInfo> entry : timingTriggers.entrySet()) {
            triggerInfo = entry.getValue();
            //已经超时，处理
            if (triggerInfo.isTimeout() && !triggerInfo.isRunning()) {
                //防止多次执行
                triggerInfo.setRunning(true);
                executorService.submit(triggerInfo);
            }
        }
    }

    @Override
    public void add(final Channel channel, final HeartbeatStrategy strategy, final Supplier<HeartbeatTrigger> supplier) {
        if (strategy.getHeartbeatMode() == HeartbeatMode.TIMING && strategy.getInterval() > 0) {
            timingTriggers.computeIfAbsent(channel, o -> new TriggerInfo(supplier.get(), strategy.getInterval(),
                    SystemClock.now() + ThreadLocalRandom.current().nextInt(strategy.getInterval())));
        } else if (strategy.getHeartbeatMode() == HeartbeatMode.IDLE) {
            channel.getAttribute(Channel.IDLE_HEARTBEAT_TRIGGER, o -> supplier.get());
        }
    }

    @Override
    public void remove(final Channel channel) {
        timingTriggers.remove(channel);
    }

    /**
     * 触发器信息
     */
    protected static class TriggerInfo implements Runnable {
        /**
         * 心跳触发器
         */
        protected HeartbeatTrigger heartbeatTrigger;
        /**
         * 时间间隔
         */
        protected int interval;
        /**
         * 执行时间
         */
        protected long executeTime;
        /**
         * 是否正在执行
         */
        protected boolean running;

        /**
         * 构造函数
         *
         * @param heartbeatTrigger
         * @param interval
         * @param executeTime
         */
        public TriggerInfo(HeartbeatTrigger heartbeatTrigger, int interval, long executeTime) {
            this.heartbeatTrigger = heartbeatTrigger;
            this.interval = interval;
            this.executeTime = executeTime;
        }

        /**
         * 是否超时
         *
         * @return
         */
        public boolean isTimeout() {
            return executeTime <= SystemClock.now();
        }

        public boolean isRunning() {
            return running;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        @Override
        public void run() {
            try {
                heartbeatTrigger.trigger();
            } finally {
                executeTime = SystemClock.now() + interval;
                running = false;
            }
        }
    }

}
