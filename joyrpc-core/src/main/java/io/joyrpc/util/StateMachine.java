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

import io.joyrpc.event.Event;
import io.joyrpc.event.EventHandler;
import io.joyrpc.exception.InitializationException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.joyrpc.util.Status.*;

/**
 * 状态机
 */
public class StateMachine<T extends StateMachine.Controller> {

    protected static final AtomicReferenceFieldUpdater<StateMachine, Status> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(StateMachine.class, Status.class, "status");

    /**
     * 控制器提供者
     */
    protected Supplier<T> supplier;
    /**
     * 事件处理器
     */
    protected EventHandler<StateEvent> handler;

    /**
     * 打开的结果
     */
    protected transient volatile CompletableFuture<Void> openFuture;
    /**
     * 关闭Future
     */
    protected transient volatile CompletableFuture<Void> closeFuture = CompletableFuture.completedFuture(null);
    /**
     * 状态
     */
    protected transient volatile Status status = Status.CLOSED;
    /**
     * 控制器
     */
    protected transient T controller;

    /**
     * 构造函数
     *
     * @param supplier
     * @param handler
     */
    public StateMachine(final Supplier<T> supplier, final EventHandler<StateEvent> handler) {
        this.supplier = supplier;
        this.handler = handler;
    }

    /**
     * 大开
     *
     * @return
     */
    public CompletableFuture<Void> open() {
        if (STATE_UPDATER.compareAndSet(this, Status.CLOSED, Status.OPENING)) {
            publish(EventType.START_OPEN);
            final CompletableFuture<Void> f = new CompletableFuture<>();
            final T cc = supplier.get();
            openFuture = f;
            controller = cc;
            cc.open().whenComplete((v, e) -> {
                if (openFuture != f || e == null && !STATE_UPDATER.compareAndSet(this, Status.OPENING, Status.OPENED)) {
                    publish(EventType.FAIL_OPEN_ILLEGAL_STATE);
                    //已经被关闭了
                    Throwable throwable = new InitializationException("state is illegal.");
                    f.completeExceptionally(throwable);
                    cc.close(false);
                } else if (e != null) {
                    publish(EventType.FAIL_OPEN);
                    f.completeExceptionally(e);
                    close(false);
                } else {
                    publish(EventType.START_OPEN);
                    f.complete(null);
                }
            });
            return f;
        } else {
            switch (status) {
                case OPENING:
                case OPENED:
                    //可重入，没有并发调用
                    return openFuture;
                default:
                    //其它状态不应该并发执行
                    return Futures.completeExceptionally(new InitializationException("state is illegal."));
            }
        }
    }

    /**
     * 关闭
     *
     * @param gracefully
     * @return
     */
    public CompletableFuture<Void> close(final boolean gracefully) {
        if (STATE_UPDATER.compareAndSet(this, OPENING, CLOSING)) {
            publish(EventType.START_CLOSE);
            CompletableFuture<Void> future = new CompletableFuture<>();
            closeFuture = future;
            controller.broken();
            openFuture.whenComplete((v, e) -> {
                //openFuture完成后会自动关闭控制器
                publish(EventType.SUCCESS_CLOSE);
                status = CLOSED;
                controller = null;
                future.complete(null);
            });
            return closeFuture;
        } else if (STATE_UPDATER.compareAndSet(this, OPENED, CLOSING)) {
            //状态从打开到关闭中，该状态只能变更为CLOSED
            publish(EventType.START_CLOSE);
            CompletableFuture<Void> future = new CompletableFuture<>();
            closeFuture = future;
            controller.close(gracefully).whenComplete((o, s) -> {
                publish(EventType.SUCCESS_CLOSE);
                status = CLOSED;
                controller = null;
                future.complete(null);
            });
            return closeFuture;
        } else {
            switch (status) {
                case CLOSING:
                case CLOSED:
                    return closeFuture;
                default:
                    return Futures.completeExceptionally(new IllegalStateException("Status is illegal."));
            }
        }
    }

    /**
     * 在打开状态下执行
     *
     * @param consumer 消费者
     */
    public void whenOpen(final Consumer<T> consumer) {
        if (consumer != null && status.isOpen()) {
            T c = controller;
            if (c != null) {
                consumer.accept(c);
            }
        }
    }

    public T getController() {
        return controller;
    }

    public Status getStatus() {
        return status;
    }

    /**
     * 发布事件
     *
     * @param type
     */
    protected void publish(final EventType type) {
        if (handler != null) {
            handler.handle(new StateEvent(type));
        }
    }

    /**
     * 控制器
     */
    public interface Controller {

        /**
         * 打开
         *
         * @return
         */
        CompletableFuture<Void> open();

        /**
         * 优雅关闭
         *
         * @param gracefully
         * @return
         */
        CompletableFuture<Void> close(boolean gracefully);

        /**
         * 关闭前中断等待
         */
        default void broken() {

        }
    }

    /**
     * 事件类型
     */
    public enum EventType {
        /**
         * 打开中
         */
        START_OPEN,
        /**
         * 打开
         */
        SUCCESS_OPEN,
        /**
         * 打开失败
         */
        FAIL_OPEN,
        /**
         * 打开状态异常
         */
        FAIL_OPEN_ILLEGAL_STATE,
        /**
         * 关闭中
         */
        START_CLOSE,
        /**
         * 关闭
         */
        SUCCESS_CLOSE;
    }

    /**
     * 状态事件
     */
    public static class StateEvent implements Event {
        /**
         * 事件类型
         */
        protected final EventType type;

        public StateEvent(EventType type) {
            this.type = type;
        }

        public EventType getType() {
            return type;
        }
    }

}
