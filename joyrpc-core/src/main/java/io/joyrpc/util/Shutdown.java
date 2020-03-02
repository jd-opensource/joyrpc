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

import io.joyrpc.constants.Constants;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @date 6/6/2019
 */
public class Shutdown {

    private static final Logger logger = LoggerFactory.getLogger(Shutdown.class);

    /**
     * 单例
     */
    protected static final Shutdown INSTANCE = new Shutdown();
    /**
     * 默认优先级
     */
    public static final short DEFAULT_PRIORITY = Short.MAX_VALUE;
    /**
     * 系统关闭钩子
     */
    protected List<Hook> hooks = new CopyOnWriteArrayList<>();

    /**
     * 注册
     */
    protected boolean register;

    /**
     * 是否在关闭
     */
    protected boolean shutdown;

    /**
     * 构造函数
     */
    protected Shutdown() {

    }

    /**
     * 手动关闭
     */
    protected synchronized CompletableFuture<Void> doShutdown() {
        if (shutdown) {
            return CompletableFuture.completedFuture(null);
        }
        logger.info("Shutdown....");
        shutdown = true;
        CompletableFuture<Void> result = null;
        if (hooks.isEmpty()) {
            result = CompletableFuture.completedFuture(null);
        } else {
            //优先级排序
            Collections.sort(hooks, Comparator.comparingInt(Hook::priority));
            //构造链，一个个执行
            List<HookGroup> groups = new LinkedList<>();
            HookGroup lastGroup = null;

            //遍历钩子进行优先级分组
            for (Hook hook : hooks) {
                if (lastGroup == null || lastGroup.priority != hook.priority()) {
                    lastGroup = new HookGroup(hook.priority());
                    lastGroup.add(hook);
                    groups.add(lastGroup);
                } else if (lastGroup.priority == hook.priority()) {
                    lastGroup.add(hook);
                }
            }

            //按照优先级串行执行构造分组
            for (HookGroup group : groups) {
                if (result == null) {
                    result = group.run();
                } else {
                    result = result.handle((t, r) -> group.run().join());
                }
            }
        }
        return result.whenComplete((r, t) -> {
            if (t == null) {
                logger.info("Shutdown successfully.");
            } else {
                logger.error("Shutdown failed.", t);
            }
        });
    }

    /**
     * 延迟注册，如果没有监听器则不用注册
     */
    protected synchronized void register() {
        if (register) {
            return;
        }
        register = true;
        // 增加jvm关闭事件
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Parametric parametric = new MapParametric(GlobalContext.getContext());
                doShutdown().get(parametric.getPositiveLong(Constants.SHUTDOWN_TIMEOUT_OPTION),
                        TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException ignored) {
            }
        }, "Shutdown"));
    }

    /**
     * 添加系统钩子
     *
     * @param hook
     */
    public static void addHook(final Hook hook) {
        if (hook != null) {
            if (!INSTANCE.register) {
                INSTANCE.register();
            }
            INSTANCE.hooks.add(hook);
        }
    }

    /**
     * 添加系统钩子
     *
     * @param runnable
     */
    public static void addHook(final Runnable runnable) {
        if (runnable != null) {
            addHook(new HookAdapter(runnable));
        }
    }

    /**
     * 是否正在关闭
     *
     * @return
     */
    public static boolean isShutdown() {
        return INSTANCE.shutdown;
    }

    /**
     * 手动调用关闭
     */
    public static CompletableFuture<Void> shutdown() {
        return INSTANCE.doShutdown();
    }

    /**
     * 手动调用关闭
     *
     * @param timeout
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public static void shutdown(long timeout) throws InterruptedException, ExecutionException, TimeoutException {
        INSTANCE.doShutdown().get(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * 钩子
     */
    @FunctionalInterface
    public interface Hook {

        /**
         * 运行
         *
         * @return
         */
        CompletableFuture<Void> run();

        /**
         * 优先级分组，值约小优先级越高，相同优先级的钩子可以并行执行，否则串行执行，当一个执行完毕再执行下一个
         *
         * @return
         */
        default int priority() {
            return DEFAULT_PRIORITY;
        }
    }

    /**
     * 默认钩子实现
     */
    public static class HookAdapter implements Hook {
        /**
         * 运行
         */
        protected Hook hook;
        /**
         * 优先级
         */
        protected int priority;

        public HookAdapter(Runnable runnable) {
            this(runnable, DEFAULT_PRIORITY, false);
        }

        public HookAdapter(Runnable runnable, int priority) {
            this(runnable, priority, false);
        }

        public HookAdapter(Runnable runnable, int priority, boolean async) {
            this.hook = !async ? () -> {
                runnable.run();
                return CompletableFuture.completedFuture(null);
            } : () -> CompletableFuture.runAsync(runnable);
            this.priority = priority;
        }

        public HookAdapter(Hook hook, int priority) {
            this.hook = hook;
            this.priority = priority;
        }

        @Override
        public CompletableFuture<Void> run() {
            return hook.run();
        }

        @Override
        public int priority() {
            return priority;
        }
    }

    /**
     * 钩子分组实现
     */
    public static class HookGroup implements Hook {
        /**
         * 运行
         */
        protected LinkedList<Hook> hooks = new LinkedList<>();
        /**
         * 优先级
         */
        protected int priority;

        /**
         * 构造函数
         *
         * @param priority
         */
        public HookGroup(int priority) {
            this.priority = priority;
        }

        /**
         * 大小
         *
         * @return
         */
        public int size() {
            return hooks.size();
        }

        /**
         * 添加钩子
         *
         * @param hook
         */
        public void add(final Hook hook) {
            if (hook != null) {
                hooks.add(hook);
            }
        }

        @Override
        public CompletableFuture<Void> run() {
            switch (hooks.size()) {
                case 0:
                    return CompletableFuture.completedFuture(null);
                case 1:
                    return hooks.getFirst().run();
                default:
                    CompletableFuture<Void>[] futures = new CompletableFuture[hooks.size()];
                    int i = 0;
                    for (Hook hook : hooks) {
                        futures[i++] = hook.run();
                    }
                    return CompletableFuture.allOf(futures);
            }
        }

        @Override
        public int priority() {
            return priority;
        }
    }

}
