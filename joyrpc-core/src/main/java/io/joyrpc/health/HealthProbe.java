package io.joyrpc.health;

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

import io.joyrpc.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static io.joyrpc.Plugin.DOCTOR;
import static io.joyrpc.util.Timer.timer;

/**
 * 监控状态探针
 */
public class HealthProbe {

    protected static final Logger logger = LoggerFactory.getLogger(HealthProbe.class);
    protected static volatile HealthProbe INSTANCE;

    /**
     * 健康状态
     */
    protected volatile HealthState state = HealthState.HEALTHY;

    /**
     * 构造函数
     */
    protected HealthProbe() {
        timer().add(new DiagnoseTask(s -> state = s));
    }

    public HealthState getState() {
        return state;
    }

    /**
     * 获取单例
     *
     * @return 探针
     */
    public static HealthProbe getInstance() {
        if (INSTANCE == null) {
            synchronized (HealthProbe.class) {
                if (INSTANCE == null) {
                    INSTANCE = new HealthProbe();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 诊断任务
     */
    protected static class DiagnoseTask implements Timer.TimeTask {
        /**
         * 消费者
         */
        protected Consumer<HealthState> consumer;

        /**
         * 构造函数
         *
         * @param consumer
         */
        public DiagnoseTask(Consumer<HealthState> consumer) {
            this.consumer = consumer;
        }

        @Override
        public String getName() {
            return "DiagnoseTask";
        }

        @Override
        public long getTime() {
            return 5000L;
        }

        @Override
        public void run() {
            //调用插件进行诊断
            HealthState result = HealthState.HEALTHY;
            HealthState state;
            int plugins = 0;
            for (Doctor doctor : DOCTOR.extensions()) {
                plugins++;
                state = doctor.diagnose();
                if (state.ordinal() > result.ordinal()) {
                    result = state;
                }
                if (state == HealthState.DEAD) {
                    break;
                }
            }
            //有插件继续执行
            if (plugins > 0) {
                consumer.accept(result);
                timer().add(this);
            }
        }
    }

}
