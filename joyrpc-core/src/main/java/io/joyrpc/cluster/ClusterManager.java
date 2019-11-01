package io.joyrpc.cluster;

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
import io.joyrpc.util.Daemon;
import io.joyrpc.util.Shutdown;
import io.joyrpc.util.Switcher;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;


/**
 * 集群管理器
 */
public class ClusterManager implements Closeable {

    public static final int THREADS = 5;
    public static final long INTERVAL = 200L;
    /**
     * 集群
     */
    protected Map<String, Cluster> clusters = new ConcurrentHashMap<>(500);
    /**
     * 检测线程
     */
    protected Daemon checker;
    /**
     * 执行线程池
     */
    protected ExecutorService executorService;
    /**
     * 开关
     */
    protected Switcher switcher = new Switcher();
    /**
     * 执行线程数
     */
    protected int threads = THREADS;
    /**
     * 检查间隔
     */
    protected long interval = INTERVAL;

    /**
     * 构造函数
     */
    public ClusterManager() {
        this(THREADS, INTERVAL);
    }

    /**
     * 构造函数
     *
     * @param threads
     */
    public ClusterManager(final int threads) {
        this(threads, INTERVAL);
    }

    /**
     * 构造函数
     *
     * @param threads
     * @param interval
     */
    public ClusterManager(final int threads, final long interval) {
        this.threads = threads <= 0 ? THREADS : threads;
        this.interval = interval > 0 ? interval : INTERVAL;
        //添加关闭钩子
        Shutdown.addHook(this::close);
    }

    /**
     * 根据名称获取或创建集群
     *
     * @param name     集群名称
     * @param function 构造集群的函数
     * @return the cluster
     */
    public <T extends Cluster> T getCluster(final String name, final Function<String, T> function) {
        return name == null ? null : (T) clusters.computeIfAbsent(name, function);
    }

    /**
     * 根据名称获取集群.
     *
     * @param name 集群名称
     * @return the cluster
     */
    public <T extends Cluster> T getCluster(final String name) {
        return name == null ? null : (T) clusters.get(name);
    }

    /**
     * Remove cluster by cluster name.
     *
     * @param name 集群名称
     */
    public <T extends Cluster> T removeCluster(final String name) {
        return name == null ? null : (T) clusters.remove(name);
    }

    /**
     * 启动
     */
    public void start() {
        switcher.open(() -> {
            executorService = new ThreadPoolExecutor(threads, threads, 5000L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    new NamedThreadFactory("clusterManager", true));
            checker = Daemon.builder().name("clusterManager").interval(interval)
                    .condition(() -> switcher.isOpened() && !Shutdown.isShutdown())
                    .runnable(() -> supervise())
                    .build();
            checker.start();
        });
    }

    /**
     * 监管
     */
    protected void supervise() {
        for (Map.Entry<String, Cluster> entry : clusters.entrySet()) {
            if (!switcher.isOpened() || Shutdown.isShutdown()) {
                break;
            }
            supervise(entry.getValue());
        }
    }

    /**
     * 监管
     *
     * @param cluster
     */
    protected void supervise(final Cluster cluster) {
        List<Runnable> runnables = cluster.supervise();
        if (runnables != null) {
            for (Runnable runnable : runnables) {
                executorService.submit(runnable);
            }
        }
    }

    @Override
    public void close() {
        switcher.close(() -> {
            if (executorService != null) {
                executorService.shutdownNow();
                executorService = null;
            }
            checker = null;
            clusters.values().forEach(v -> v.close());
            clusters.clear();
        });
    }
}
