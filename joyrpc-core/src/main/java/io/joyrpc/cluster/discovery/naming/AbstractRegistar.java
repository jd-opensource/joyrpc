package io.joyrpc.cluster.discovery.naming;

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

import io.joyrpc.cluster.Shard;
import io.joyrpc.cluster.discovery.backup.Backup;
import io.joyrpc.cluster.discovery.backup.BackupDatum;
import io.joyrpc.cluster.event.ClusterEvent;
import io.joyrpc.cluster.event.ClusterEvent.ShardEvent;
import io.joyrpc.cluster.event.ClusterEvent.ShardEventType;
import io.joyrpc.event.Publisher;
import io.joyrpc.event.UpdateEvent.UpdateType;
import io.joyrpc.exception.ProtocolException;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.URLOption;
import io.joyrpc.util.Maps;
import io.joyrpc.util.Switcher;
import io.joyrpc.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static io.joyrpc.Plugin.EVENT_BUS;
import static io.joyrpc.cluster.event.ClusterEvent.ShardEventType.ADD;

/**
 * 统一集群目录服务，每次全量拉取
 */
public class AbstractRegistar implements Registar {
    protected static final Logger logger = LoggerFactory.getLogger(AbstractRegistar.class);
    public static final URLOption<Long> UPDATE_INTERVAL = new URLOption<>("updateInterval", 10000L);
    /**
     * 目录服务名称
     */
    protected String name;
    /**
     * 目录服务URL
     */
    protected URL url;
    /**
     * 集群提供者
     */
    protected ClusterProvider provider;
    /**
     * 地域
     */
    protected String region;
    /**
     * 数据中心
     */
    protected String dataCenter;
    /**
     * 数据过期时间
     */
    protected long expireTime;
    /**
     * 备份
     */
    protected Backup backup;
    /**
     * 更新线程池
     */
    protected ExecutorService executorService;
    /**
     * 调度线程
     */
    protected Thread dispatcher;
    /**
     * 恢复的历史数据
     */
    protected Map<String, List<Shard>> backups;
    /**
     * 不同集群分片
     */
    protected Map<String, ClusterMeta> clusters = new ConcurrentHashMap<>(20);
    /**
     * 通知器
     */
    protected Map<String, Publisher<ClusterEvent>> publishers = new ConcurrentHashMap<>();
    /**
     * 任务队列
     */
    protected Deque<ClusterMeta> deque = new ConcurrentLinkedDeque<>();
    /**
     * 脏数据标识
     */
    protected AtomicBoolean dirty = new AtomicBoolean();
    /**
     * 等待锁
     */
    protected final Object mutex = new Object();
    /**
     * 开关
     */
    protected Switcher switcher = new Switcher();
    /**
     * registar实例Id
     */
    protected int registarId;

    /**
     * 构造函数
     *
     * @param url URL
     */
    public AbstractRegistar(URL url) {
        this(null, url, null, 0, null, null);
    }

    /**
     * 构造函数
     *
     * @param name 名称，要求符合Java标识符规范
     * @param url  URL
     */
    public AbstractRegistar(final String name, URL url) {
        this(name, url, null, 0, null, null);
    }

    /**
     * 构造函数
     *
     * @param name            名称，要求符合Java标识符规范
     * @param url             URL
     * @param provider        集群提供者
     * @param expireTime      过期时间
     * @param backup          备份
     * @param executorService 线程池
     */
    public AbstractRegistar(final String name, final URL url, final ClusterProvider provider, final long expireTime,
                            final Backup backup, final ExecutorService executorService) {
        this.name = name == null || name.isEmpty() ? url.getString("name", url.getProtocol()) : name;
        this.url = url;
        this.provider = provider;
        this.expireTime = expireTime > 0 ? expireTime : url.getPositiveLong(UPDATE_INTERVAL);
        this.backup = backup;
        this.executorService = executorService;
        this.region = url != null ? url.getString(REGION) : null;
        this.dataCenter = url != null ? url.getString(DATA_CENTER) : null;
        this.registarId = REGISTAR_ID_GENERATOR.get();
    }

    @Override
    public boolean subscribe(final URL url, final ClusterHandler handler) {
        if (url == null || handler == null) {
            return false;
        }
        return switcher.reader().quietAnyway(() -> {
            String key = getKey(url);
            Publisher<ClusterEvent> publisher = publishers.computeIfAbsent(key, o -> getPublisher(url));
            if (switcher.isOpened()) {
                publisher.start();
            }
            if (publisher.addHandler(handler)) {
                ClusterMeta meta = Maps.computeIfAbsent(clusters, key, k -> create(url, key), (v, added) -> {
                    if (added) {
                        deque.offerFirst(v);
                        //通知等待线程，有新的待更新数据
                        synchronized (mutex) {
                            mutex.notifyAll();
                        }
                    }
                });

                List<Shard> shards = meta.getShards();
                if (shards != null && !shards.isEmpty()) {
                    List<ShardEvent> events = new ArrayList<>(shards.size());
                    shards.forEach(o -> events.add(new ShardEvent(o, ADD)));
                    publisher.offer(new ClusterEvent(this, handler, UpdateType.FULL, meta.updates, events));
                }
                return true;
            }
            return false;
        });
    }

    /**
     * 构建集群元数据
     *
     * @param url  url
     * @param name 名称
     * @return
     */
    protected ClusterMeta create(final URL url, final String name) {
        return new ClusterMeta(url, name);
    }

    @Override
    public boolean unsubscribe(final URL url, final ClusterHandler handler) {
        if (url == null || handler == null) {
            return false;
        }
        return switcher.reader().quietAnyway(() -> {
            Publisher<ClusterEvent> publisher = publishers.get(getKey(url));
            return publisher != null && publisher.removeHandler(handler);
        });
    }

    /**
     * 获取URL的Key
     *
     * @param url
     * @return
     */
    protected String getKey(final URL url) {
        return url.toString(false, false);
    }

    @Override
    public String getDataCenter() {
        return dataCenter;
    }

    @Override
    public String getRegion() {
        return region;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    /**
     * 启动
     *
     * @return
     */
    public CompletableFuture<Void> open() {
        //多次调用也能拿到正确的Future
        return switcher.open(f -> {
            //启动事件监听器
            publishers.values().forEach(Publisher::start);
            dispatcher = new Thread(AbstractRegistar.this::schedule, getClass().getSimpleName());
            dispatcher.setDaemon(true);
            dispatcher.start();
            return CompletableFuture.completedFuture(null);
        });
    }

    /**
     * 关闭
     *
     * @return
     */
    public CompletableFuture<Void> close() {
        // 多次调用也能拿到正确的Future
        return switcher.close(f -> {
            if (dispatcher != null) {
                dispatcher.interrupt();
            }
            //关闭事件监听器
            publishers.values().forEach(Publisher::close);
            // 备份一下数据
            if (dirty.compareAndSet(true, false)) {
                backup();
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    /**
     * 调度更新集群信息
     */
    protected void schedule() {
        if (backup != null) {
            //初次运行恢复数据
            restore();
        }

        long waitTime;
        while (switcher.isOpened()) {
            //执行一次更新
            waitTime = update();
            //判断数据是否有修改，进行备份
            if (backup != null && dirty.compareAndSet(true, false)) {
                //执行备份操作
                backup();
            }
            //等待
            synchronized (mutex) {
                try {
                    mutex.wait(Math.max(waitTime, 500L));
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    /**
     * 执行任务队列更新
     *
     * @return 等等事件
     */
    protected long update() {
        ClusterMeta meta;
        long time;
        while (!deque.isEmpty()) {
            //获取队首数据，判断是否过期
            meta = deque.peek();
            time = meta == null ? expireTime : (meta.getExpireTime() - SystemClock.now());
            if (time > 0) {
                return time;
            }
            //队首出队
            meta = deque.poll();
            //二次校验
            if (meta == null) {
                return expireTime;
            }
            time = meta.getExpireTime() - SystemClock.now();
            if (time > 0) {
                //没有过期
                deque.addFirst(meta);
                return time;
            }
            update(meta);
        }
        return expireTime;
    }

    /**
     * 更新
     *
     * @param meta
     */
    protected void update(final ClusterMeta meta) {
        meta.setUpdates(meta.getUpdates() + 1);
        if (executorService != null) {
            //多线程更新
            executorService.submit(() -> doUpdate(meta));
        } else {
            //调度线程更新
            doUpdate(meta);
        }
    }

    /**
     * 更新
     *
     * @param meta
     */
    protected void doUpdate(final ClusterMeta meta) {
        if (!clusters.containsKey(meta.getName())) {
            return;
        }
        //下次重试过期时间
        long time = expireTime;
        boolean error = false;
        List<Shard> targets = null;
        try {
            targets = provider.apply(url, meta.getUrl());
        } catch (ProtocolException e) {
            //协议异常
            logger.error(String.format("Unrecoverable error occurs while updating %s cluster %s.", name, meta.getName()), e);
            time = 0;
        } catch (Throwable e) {
            error = true;
            if (expireTime <= 0) {
                //非过期数据，没有获取到也进行重试
                time = 1000L;
            }
            if (meta.getUpdates() == 1 && backups != null) {
                //第一次调用失败，尝试使用备份恢复的数据
                targets = backups.get(meta.getName());
                logger.warn(String.format("Error occurs while updating %s cluster %s. using backup data. retry in %d(ms)", name, meta.getName(), time), e);
            } else {
                logger.error(String.format("Error occurs while updating %s cluster %s. retry in %d(ms)", name, meta.getName(), time), e);
            }
        }
        //获取到了数据
        if (targets != null && !targets.isEmpty()) {
            try {
                List<Shard> shards = meta.getShards();
                targets = targets.stream().distinct().collect(Collectors.toList());
                meta.setShards(targets);
                //非备份数据
                if (!error && compare(meta.getName(), meta.updates, shards, targets)) {
                    //触发备份操作
                    dirty.set(true);
                }
            } catch (Exception e) {
                logger.error(String.format("Error occurs while updating %s cluster %s. retry in %d(ms)", name, meta.getName(), time), e);
            }

        }
        if (time > 0) {
            //设置下次更新时间
            meta.setExpireTime(SystemClock.now() + time);
            //重新入队
            deque.offerLast(meta);
        }
    }

    /**
     * 比较变化，进行增量更新通知
     *
     * @param name    名称
     * @param updates
     * @param olds    原有分片
     * @param targets 新分片
     * @return 是否有变化
     */
    protected boolean compare(final String name, final long updates, final List<Shard> olds, final List<Shard> targets) {
        List<ShardEvent> events = new LinkedList<>();
        ClusterEvent event = new ClusterEvent(this, null, UpdateType.UPDATE, updates, events);
        if (olds == null || olds.isEmpty()) {
            targets.forEach(o -> events.add(new ShardEvent(o, ShardEventType.ADD)));
        } else {
            //原有分片键值对
            Map<String, Shard> map = new HashMap<>(olds.size());
            olds.forEach(o -> map.put(o.getName(), o));
            //遍历现有分片，判断哪些新增或修改
            Shard old;
            for (Shard shard : targets) {
                old = map.remove(shard.getName());
                if (old == null) {
                    //新增的
                    events.add(new ShardEvent(shard, ShardEventType.ADD));
                } else if (!old.equals(shard)) {
                    //修改的
                    events.add(new ShardEvent(shard, ShardEventType.UPDATE));
                }
            }
            //剩下是删除的
            for (Shard shard : map.values()) {
                events.add(new ShardEvent(shard, ShardEventType.DELETE));
            }
        }
        //广播通知
        if (!events.isEmpty()) {
            Publisher<ClusterEvent> publisher = publishers.get(name);
            if (publisher != null) {
                publisher.offer(event);
            }
            return true;
        }
        return false;
    }

    /**
     * 恢复集群备份
     */
    protected void restore() {
        if (backup != null) {
            try {
                BackupDatum datum = backup.restore(name);
                backups = datum == null ? null : datum.toSnapshot();
                if (backups != null && !backups.isEmpty()) {
                    logger.info(String.format("Success restoring %s cluster.", name));
                }
            } catch (IOException e) {
                logger.error(String.format("Error occurs while restoring %s cluster", name), e);
            }
        }
    }

    /**
     * 备份集群信息
     */
    protected void backup() {
        Map<String, List<Shard>> snapshot = new HashMap<>(clusters.size());
        clusters.values().forEach(o -> {
            if (!o.isEmpty()) {
                snapshot.put(o.getName(), o.getShards());
            }
        });
        if (snapshot.isEmpty()) {
            try {
                backup.backup(name, new BackupDatum(snapshot));
            } catch (IOException e) {
                logger.error(String.format("Error occurs while making a backup of %s cluster.", name), e);
            }
        }
    }

    /**
     * 获取通知器
     *
     * @param url
     * @return
     */
    protected Publisher<ClusterEvent> getPublisher(final URL url) {
        return EVENT_BUS.get().getPublisher(Registar.class.getSimpleName(), "registar_" + registarId + "-" + url.getAddress());
    }


    /**
     * 集群元数据
     */
    protected static class ClusterMeta {
        /**
         * 地址
         */
        protected URL url;
        /**
         * 名称
         */
        protected String name;
        /**
         * 分片
         */
        protected List<Shard> shards;
        /**
         * 数据过期时间
         */
        protected long expireTime;
        /**
         * 更新的次数
         */
        protected long updates;

        public ClusterMeta(URL url, String name) {
            this.url = url;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public URL getUrl() {
            return url;
        }

        public List<Shard> getShards() {
            return shards;
        }

        public void setShards(List<Shard> shards) {
            this.shards = shards;
        }

        public long getExpireTime() {
            return expireTime;
        }

        public void setExpireTime(long expireTime) {
            this.expireTime = expireTime;
        }

        public long getUpdates() {
            return updates;
        }

        public void setUpdates(long updates) {
            this.updates = updates;
        }

        /**
         * 判断节点是否为空
         *
         * @return
         */
        public boolean isEmpty() {
            return shards == null || shards.isEmpty();
        }

        /**
         * 是否过期
         *
         * @return
         */
        public boolean isExpire() {
            return SystemClock.now() >= expireTime;
        }
    }


}
