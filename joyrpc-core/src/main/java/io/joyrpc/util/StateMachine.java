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

import io.joyrpc.event.EventHandler;
import io.joyrpc.util.StateController.ExStateController;
import io.joyrpc.util.StateFuture.ExStateFuture;
import io.joyrpc.util.StateInt.ExStateInt;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.joyrpc.util.StateTransition.*;

/**
 * 状态机
 */
public class StateMachine<T, S extends StateTransition, M extends StateController<T>> {

    /**
     * 名称
     */
    protected String name;
    /**
     * 控制器提供者
     */
    protected Supplier<M> controllerSupplier;
    /**
     * 异常提供者
     */
    protected Function<String, Throwable> errorFunc;
    /**
     * 事件处理器
     */
    protected EventHandler<StateEvent> handler;
    /**
     * 打开的结果
     */
    protected StateFuture<T> stateFuture;
    /**
     * 状态
     */
    protected S state;
    /**
     * 控制器
     */
    protected M controller;

    public StateMachine(final String name,
                        final Supplier<M> controllerSupplier,
                        final Function<String, Throwable> errorFunc,
                        final S state,
                        final StateFuture<T> stateFuture,
                        final EventHandler<StateEvent> handler) {
        this.name = name;
        this.controllerSupplier = controllerSupplier;
        this.errorFunc = errorFunc == null ? s -> new IllegalStateException(s) : errorFunc;
        this.state = state;
        this.stateFuture = stateFuture == null ? new StateFuture<>(null, null, null, null, null) : stateFuture;
        this.handler = handler;
    }

    /**
     * 大开
     *
     * @return Future
     */
    public CompletableFuture<T> open() {
        return open(null, handler);
    }

    /**
     * 大开
     *
     * @param runnable 执行块
     * @return Future
     */
    public CompletableFuture<T> open(final Runnable runnable) {
        return open(runnable, handler);
    }

    /**
     * 大开
     *
     * @param runnable 执行块
     * @param handler  事件处理器
     * @return Future
     */
    public CompletableFuture<T> open(final Runnable runnable, final EventHandler<StateEvent> handler) {
        if (state.tryOpening() == SUCCESS) {
            final CompletableFuture<T> future = stateFuture.newOpenFuture();
            final M cc = getOpenController();
            //在controller赋值后再触发事件
            publish(StateEvent.START_OPEN, null, handler);
            //在赋值controller之后执行
            if (runnable != null) {
                runnable.run();
            }
            //延迟加载
            stateFuture.newBeforeOpenFuture().whenComplete((d, t) -> {
                if (stateFuture.getOpenFuture() != future || !state.isOpening()) {
                    //状态异常，关闭服务
                    onIllegalStateOpen(cc, handler, future);
                } else if (t != null) {
                    //加载异常先关闭服务，防止事件触发判断状态还是OPENED
                    onFailedOpen(t, handler, future);
                } else {
                    //打开
                    try {
                        cc.open().whenComplete((v, e) -> {
                            if (stateFuture.getOpenFuture() != future || e == null && state.tryOpened() != SUCCESS) {
                                //先关闭
                                onIllegalStateOpen(cc, handler, future);
                            } else if (e != null) {
                                //先关闭，防止事件触发判断状态还是OPENED
                                onFailedOpen(e, handler, future);
                            } else {
                                onSuccessOpen(handler, future);
                            }
                        });
                    } catch (Throwable e) {
                        onFailedOpen(e, handler, future);
                    }
                }
            });

            return future;
        } else {
            //等待打开就绪
            CompletableFuture<T> result;
            while ((result = checkOpen()) == null) {
                //并发问题，这个时候可能还没有创建好Future，等待一下
                LockSupport.parkNanos(1);
            }
            return result;
        }
    }

    /**
     * @param handler 事件处理器
     * @param future  CompletableFuture
     */
    private void onSuccessOpen(final EventHandler<StateEvent> handler, final CompletableFuture<T> future) {
        publish(StateEvent.SUCCESS_OPEN, null, handler);
        future.complete(null);
    }

    /**
     * 打开失败
     *
     * @param throwable 异常
     * @param handler   事件处理器
     * @param future    CompletableFuture
     */
    protected void onFailedOpen(final Throwable throwable, final EventHandler<StateEvent> handler, final CompletableFuture<T> future) {
        close(false);
        publish(StateEvent.FAIL_OPEN, throwable, handler);
        future.completeExceptionally(throwable);
    }

    /**
     * 打开后状态异常
     *
     * @param controller 控制器
     * @param handler    事件处理器
     * @param future     CompletableFuture
     */
    protected void onIllegalStateOpen(final M controller, final EventHandler<StateEvent> handler, final CompletableFuture<T> future) {
        controller.close(false);
        Throwable ex = errorFunc.apply(name == null || name.isEmpty() ? "state is illegal." :
                String.format("the state of %s is illegal.", name));
        publish(StateEvent.FAIL_OPEN_ILLEGAL_STATE, ex, handler);
        future.completeExceptionally(ex);
    }

    /**
     * 打开的适合获取控制器
     *
     * @return 控制器
     */
    protected M getOpenController() {
        return newController();
    }

    /**
     * 创建新的控制器
     *
     * @return 控制器
     */
    protected M newController() {
        M cc = controllerSupplier.get();
        controller = cc;
        return cc;
    }

    /**
     * 检查打开就绪
     *
     * @return CompletableFuture
     */
    protected CompletableFuture<T> checkOpen() {
        return state.isOpen() ? stateFuture.getOpenFuture() : Futures.completeExceptionally(errorFunc.apply(
                name == null || name.isEmpty() ? "state is illegal." :
                        String.format("the state of %s is illegal.", name)));
    }

    /**
     * 关闭
     *
     * @param gracefully 优雅关闭标识
     * @return Future
     */
    public CompletableFuture<T> close(final boolean gracefully) {
        return close(gracefully, null, handler);
    }

    /**
     * 关闭
     *
     * @param gracefully 优雅关闭标识
     * @param runnable   额外关闭操作
     * @return Future
     */
    public CompletableFuture<T> close(final boolean gracefully, final Runnable runnable) {
        return close(gracefully, runnable, handler);
    }

    /**
     * 关闭
     *
     * @param gracefully 优雅关闭标识
     * @param runnable   额外关闭操作
     * @param handler    事件处理器
     * @return Future
     */
    public CompletableFuture<T> close(final boolean gracefully, final Runnable runnable, final EventHandler<StateEvent> handler) {
        switch (state.tryClosing()) {
            case SUCCESS_OPENING_TO_CLOSING:
                return onOpening2Closing(runnable, handler);
            case SUCCESS_OPENED_TO_CLOSING:
                return onOpened2Closing(gracefully, runnable, handler);
            default:
                return waitCloseFuture();
        }
    }

    /**
     * 循环等待直到拿到关闭的CompletableFuture
     *
     * @return CompletableFuture
     */
    protected CompletableFuture<T> waitCloseFuture() {
        CompletableFuture<T> future;
        while ((future = checkClose()) == null) {
            //并发问题，这个时候可能还没有创建好Future，等待一下
            LockSupport.parkNanos(1);
        }
        return future;
    }

    /**
     * 从已打开到关闭
     *
     * @param gracefully 优雅关闭标识
     * @param runnable   额外关闭操作
     * @param handler    事件处理器
     * @return CompletableFuture
     */
    protected CompletableFuture<T> onOpened2Closing(final boolean gracefully, final Runnable runnable, final EventHandler<StateEvent> handler) {
        //状态从打开到关闭中，该状态只能变更为CLOSED
        CompletableFuture<T> closeFuture = stateFuture.newCloseFuture();
        publish(StateEvent.START_CLOSE, handler);
        if (runnable != null) {
            runnable.run();
        }
        //关闭栅栏准备好了
        stateFuture.newBeforeCloseFuture(gracefully).whenComplete(
                (v, t) -> controller.close(gracefully).whenComplete(
                        (o, s) -> stateFuture.newAfterCloseFuture(gracefully).whenComplete((a, e) -> {
                            state.toClosed();
                            publish(StateEvent.SUCCESS_CLOSE, handler);
                            //控制器在事件通知之后清空，因为事件通知会用到controller
                            controller = null;
                            closeFuture.complete(o);
                        })));
        return closeFuture;
    }

    /**
     * 从打开中到关闭中
     *
     * @param runnable 执行块
     * @param handler  事件处理器
     * @return CompletableFuture
     */
    protected CompletableFuture<T> onOpening2Closing(Runnable runnable, EventHandler<StateEvent> handler) {
        CompletableFuture<T> future = stateFuture.newCloseFuture();
        //触发控制器中断等待
        controller.fireClose();
        publish(StateEvent.START_CLOSE, handler);
        stateFuture.getOpenFuture().whenComplete((v, e) -> {
            if (runnable != null) {
                runnable.run();
            }
            state.toClosed();
            //openFuture完成后会自动关闭控制器
            publish(StateEvent.SUCCESS_CLOSE, handler);
            //控制器在事件通知之后清空，因为事件通知会用到controller
            controller = null;
            future.complete(v);
        });
        return future;
    }

    /**
     * 构建不合法状态异常
     *
     * @return 异常
     */
    protected Throwable createIllegalStateError() {
        return errorFunc.apply(name == null || name.isEmpty() ? "state is illegal." :
                String.format("the state of %s is illegal.", name));
    }

    /**
     * 检查关闭就绪
     *
     * @return CompletableFuture
     */
    protected CompletableFuture<T> checkClose() {
        return state.isClose() ? stateFuture.getCloseFuture() : Futures.completeExceptionally(createIllegalStateError());
    }

    /**
     * 打开关闭的栅栏
     */
    public void pass() {
        if (state.isClose()) {
            CompletableFuture<Void> future = stateFuture.getBeforeCloseFuture();
            if (future != null) {
                future.complete(null);
            }
        }
    }

    /**
     * 在指定状态下执行
     *
     * @param predicate 条件
     * @param consumer  消费者
     * @return 状态是否匹配
     */
    public boolean when(final Predicate<State> predicate, final Consumer<M> consumer) {
        if (predicate == null || predicate.test(state)) {
            M c = controller;
            if (c != null) {
                if (consumer != null) {
                    consumer.accept(c);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 在打开状态下执行
     *
     * @param consumer 消费者
     * @return 没有关闭标识
     */
    public boolean whenOpen(final Consumer<M> consumer) {
        return when(State::isOpen, consumer);
    }

    /**
     * 在打开状态下执行
     *
     * @param consumer 消费者
     * @return 打开标识
     */
    public boolean whenOpened(final Consumer<M> consumer) {
        return when(s -> s.isOpened(), consumer);
    }

    public M getController() {
        return controller;
    }

    /**
     * 获取控制器
     *
     * @param predicate 条件
     * @return 控制器
     */
    public M getController(final Predicate<S> predicate) {
        return predicate == null || predicate.test(state) ? controller : null;
    }

    public S getState() {
        return state;
    }

    public boolean isOpen(final M controller) {
        return controller == this.controller && state.isOpen();
    }

    public boolean isOpened(final M controller) {
        return controller == this.controller && state.isOpened();
    }

    public boolean isOpened() {
        return state.isOpened();
    }

    public boolean test(final Predicate<S> predicate, final M controller) {
        return (predicate == null || predicate.test(state)) && controller == this.controller;
    }

    /**
     * 判断控制器是否需要关闭
     *
     * @param controller 控制器
     * @return 需要关闭标识
     */
    public boolean isClose(final M controller) {
        return controller != null && controller != this.controller || state.isClose();
    }

    /**
     * 发布事件
     *
     * @param type 类型
     */
    protected void publish(final int type) {
        publish(type, null, handler);
    }

    /**
     * 发布事件
     *
     * @param type    类型
     * @param handler 处理器
     */
    protected void publish(final int type, final EventHandler<StateEvent> handler) {
        publish(type, null, handler);
    }

    /**
     * 发布事件
     *
     * @param type      类型
     * @param throwable 异常
     * @param handler   处理器
     */
    protected void publish(final int type, final Throwable throwable, final EventHandler<StateEvent> handler) {
        StateEvent event = new StateEvent(type, throwable);
        if (handler != null) {
            handler.handle(event);
        }
        if (controller instanceof EventHandler) {
            ((EventHandler) controller).handle(event);
        }
    }

    /**
     * 基于整数的状态机
     *
     * @param <T>
     * @param <M>
     */
    public static class IntStateMachine<T, M extends StateController<T>> extends StateMachine<T, StateInt, M> {

        public IntStateMachine(Supplier<M> controllerSupplier) {
            this(controllerSupplier, null, null, null, null);
        }

        public IntStateMachine(Supplier<M> controllerSupplier, Function<String, Throwable> errorFunc) {
            this(controllerSupplier, errorFunc, null, null, null);
        }

        public IntStateMachine(Supplier<M> controllerSupplier, StateFuture<T> stateFuture) {
            this(controllerSupplier, null, null, stateFuture, null);
        }

        public IntStateMachine(Supplier<M> controllerSupplier, Function<String, Throwable> errorFunc, StateFuture<T> stateFuture) {
            this(controllerSupplier, errorFunc, null, stateFuture, null);
        }

        public IntStateMachine(Supplier<M> controllerSupplier, Function<String, Throwable> errorFunc, StateInt state, StateFuture<T> stateFuture, EventHandler<StateEvent> handler) {
            super(null, controllerSupplier, errorFunc, state == null ? new StateInt() : state, stateFuture, handler);
        }

        public IntStateMachine(String name, Supplier<M> controllerSupplier) {
            this(name, controllerSupplier, null, null, null, null);
        }

        public IntStateMachine(String name, Supplier<M> controllerSupplier, Function<String, Throwable> errorFunc) {
            this(name, controllerSupplier, errorFunc, null, null, null);
        }

        public IntStateMachine(String name, Supplier<M> controllerSupplier, StateFuture<T> stateFuture) {
            this(name, controllerSupplier, null, null, stateFuture, null);
        }

        public IntStateMachine(String name, Supplier<M> controllerSupplier, Function<String, Throwable> errorFunc, StateFuture<T> stateFuture) {
            this(name, controllerSupplier, errorFunc, null, stateFuture, null);
        }

        public IntStateMachine(String name, Supplier<M> controllerSupplier, Function<String, Throwable> errorFunc, StateInt state, StateFuture<T> stateFuture, EventHandler<StateEvent> handler) {
            super(name, controllerSupplier, errorFunc, state == null ? new StateInt() : state, stateFuture, handler);
        }
    }

    /**
     * 增强服务提供者状态机
     */
    public static class ExStateMachine<T, M extends ExStateController<T>> extends StateMachine<T, StateInt, M> {

        public ExStateMachine(final Supplier<M> controllerSupplier,
                              final Function<String, Throwable> errorFunc,
                              final ExStateFuture<T> stateFuture) {
            this(null, controllerSupplier, errorFunc, stateFuture, null);
        }

        public ExStateMachine(final Supplier<M> controllerSupplier,
                              final Function<String, Throwable> errorFunc,
                              final ExStateFuture<T> stateFuture,
                              final EventHandler<StateEvent> handler) {
            this(null, controllerSupplier, errorFunc, stateFuture, handler);
        }

        public ExStateMachine(final String name, final Supplier<M> controllerSupplier,
                              final Function<String, Throwable> errorFunc,
                              final ExStateFuture<T> stateFuture) {
            this(name, controllerSupplier, errorFunc, stateFuture, null);
        }

        public ExStateMachine(final String name,
                              final Supplier<M> controllerSupplier,
                              final Function<String, Throwable> errorFunc,
                              final ExStateFuture<T> stateFuture,
                              final EventHandler<StateEvent> handler) {
            super(name, controllerSupplier, errorFunc, new ExStateInt(), stateFuture, handler);
        }

        /**
         * 导出
         *
         * @return CompletableFuture
         */
        public CompletableFuture<T> export() {
            final ExStateInt exState = (ExStateInt) state;
            final ExStateFuture<T> exStateFuture = (ExStateFuture<T>) stateFuture;
            if (exState.tryExporting() == SUCCESS) {
                final CompletableFuture<T> future = exStateFuture.newExportFuture();
                final M cc = getExportController();
                //在controller赋值后再触发事件
                publish(StateEvent.START_EXPORT, null, handler);
                //延迟加载
                exStateFuture.newBeforeExportFuture().whenComplete((d, t) -> {
                    if (exStateFuture.getExportFuture() != future || !exState.isExporting()) {
                        onIllegalStateExport(cc, future);
                    } else if (t != null) {
                        onFailedExport(t, future);
                    } else {
                        //打开
                        cc.export().whenComplete((v, e) -> {
                            if (exStateFuture.getExportFuture() != future || e == null && exState.tryExported() != SUCCESS) {
                                onIllegalStateExport(cc, future);
                            } else if (e != null) {
                                onFailedExport(e, future);
                            } else {
                                onSuccessExport(future);
                            }
                        });
                    }
                });

                return future;
            } else {
                //等待导出就绪
                CompletableFuture<T> result;
                while ((result = checkExport()) == null) {
                    //并发问题，这个时候可能还没有创建好Future，等待一下
                    LockSupport.parkNanos(1);
                }
                return result;
            }
        }

        @Override
        public CompletableFuture<T> close(boolean gracefully, Runnable runnable, EventHandler<StateEvent> handler) {
            switch (state.tryClosing()) {
                case SUCCESS_EXPORTING_TO_CLOSING:
                    return onExporting2Closing(runnable, handler);
                case SUCCESS_EXPORTED_TO_CLOSING:
                    return onExported2Closing(gracefully, runnable, handler);
                case SUCCESS_OPENING_TO_CLOSING:
                    return onOpening2Closing(runnable, handler);
                case SUCCESS_OPENED_TO_CLOSING:
                    return onOpened2Closing(gracefully, runnable, handler);
                default:
                    return waitCloseFuture();
            }
        }

        /**
         * 从已打开到关闭中
         *
         * @param gracefully 优雅关闭标识
         * @param runnable   额外关闭操作
         * @param handler    事件处理器
         * @return CompletableFuture
         */
        protected CompletableFuture<T> onExported2Closing(final boolean gracefully, final Runnable runnable, final EventHandler<StateEvent> handler) {
            CompletableFuture<T> closeFuture = stateFuture.newCloseFuture();
            publish(StateEvent.START_CLOSE, handler);
            if (runnable != null) {
                runnable.run();
            }
            //关闭栅栏准备好了
            stateFuture.newBeforeCloseFuture(gracefully).whenComplete(
                    (v, t) -> controller.close(gracefully).whenComplete(
                            (o, s) -> stateFuture.newAfterCloseFuture(gracefully).whenComplete((a, e) -> {
                                state.toClosed();
                                publish(StateEvent.SUCCESS_CLOSE, handler);
                                //控制器在事件通知之后清空，因为事件通知会用到controller
                                controller = null;
                                closeFuture.complete(o);
                            })));
            return closeFuture;
        }

        /**
         * 从打开中到关闭中
         *
         * @param runnable 执行块
         * @param handler  事件处理器
         * @return CompletableFuture
         */
        protected CompletableFuture<T> onExporting2Closing(Runnable runnable, EventHandler<StateEvent> handler) {
            ExStateFuture<T> exStateFuture = (ExStateFuture<T>) stateFuture;
            CompletableFuture<T> future = stateFuture.newCloseFuture();
            //触发控制器中断等待
            controller.fireClose();
            publish(StateEvent.START_CLOSE, handler);
            exStateFuture.getExportFuture().whenComplete((v, e) -> {
                if (runnable != null) {
                    runnable.run();
                }
                state.toClosed();
                //openFuture完成后会自动关闭控制器
                publish(StateEvent.SUCCESS_CLOSE, handler);
                //控制器在事件通知之后清空，因为事件通知会用到controller
                controller = null;
                future.complete(v);
            });
            return future;
        }

        /**
         * 获取导出的控制器
         *
         * @return 控制器
         */
        protected M getExportController() {
            return newController();
        }

        /**
         * 成功导出
         *
         * @param future CompletableFuture
         */
        protected void onSuccessExport(final CompletableFuture<T> future) {
            publish(StateEvent.SUCCESS_EXPORT, null, handler);
            future.complete(null);
        }

        /**
         * 导出失败
         *
         * @param throwable 异常
         * @param future    CompletableFuture
         */
        protected void onFailedExport(final Throwable throwable, final CompletableFuture<T> future) {
            //加载异常先关闭服务，防止事件触发判断状态还是OPENED
            close(false);
            publish(StateEvent.FAIL_EXPORT, throwable, handler);
            future.completeExceptionally(throwable);
        }

        /**
         * 导出结束时状态异常
         *
         * @param controller 控制器
         * @param future     CompletableFuture
         */
        protected void onIllegalStateExport(M controller, CompletableFuture<T> future) {
            //状态异常，关闭服务
            controller.close(false);
            Throwable ex = createIllegalStateError();
            publish(StateEvent.FAIL_EXPORT_ILLEGAL_STATE, ex, handler);
            future.completeExceptionally(ex);
        }

        @Override
        protected M getOpenController() {
            //直接使用导出产生的控制器
            return controller;
        }

        @Override
        protected void onFailedOpen(final Throwable throwable, final EventHandler<StateEvent> handler, final CompletableFuture<T> future) {
            //状态从打开中回滚到导出成功
            ((ExStateInt) state).translate(ExStateInt.OPENING, ExStateInt.EXPORTED);
            publish(StateEvent.FAIL_OPEN, throwable, handler);
            future.completeExceptionally(throwable);
        }

        /**
         * 检查导出就绪
         *
         * @return CompletableFuture
         */
        protected CompletableFuture<T> checkExport() {
            ExStateInt exState = (ExStateInt) state;
            ExStateFuture<T> exStateFuture = (ExStateFuture) stateFuture;
            return exState.isExport() ? exStateFuture.getExportFuture() : Futures.completeExceptionally(createIllegalStateError());
        }

    }

}
