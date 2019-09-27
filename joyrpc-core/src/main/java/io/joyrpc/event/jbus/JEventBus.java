package io.joyrpc.event.jbus;

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

import io.joyrpc.event.*;
import io.joyrpc.extension.Extension;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 事件总线
 */
@Extension("jbus")
public class JEventBus implements EventBus {

    protected Map<String, PublisherGroup> publishers = new ConcurrentHashMap<>();

    @Override
    public <E extends Event> Publisher<E> getPublisher(final String group, final String name, final PublisherConfig config) {
        if (group == null || name == null) {
            return null;
        }
        return publishers.computeIfAbsent(group, o -> new PublisherGroup(group, config)).getPublisher(name);
    }

    /**
     * 事件发布器
     *
     * @param <E>
     */
    protected static class JPublisher<E extends Event> implements Publisher<E> {
        //名称
        protected final String name;
        //发布组
        protected final PublisherGroup group;
        //发布器
        protected volatile Dispatcher polling;
        //处理器
        protected final Set<EventHandler<E>> handlers = new CopyOnWriteArraySet<>();
        /**
         * 消费者
         */
        protected Consumer<E> consumer = this::publish;

        /**
         * 构造函数
         *
         * @param name
         * @param group
         */
        public JPublisher(final String name, final PublisherGroup group) {
            this.name = name;
            this.group = group;
            this.polling = group.dispatcher;
        }

        @Override
        public boolean addHandler(final EventHandler<E> handler) {
            return handler == null ? false : handlers.add(handler);
        }

        @Override
        public boolean removeHandler(final EventHandler<E> handler) {
            return handler == null ? false : handlers.remove(handler);
        }

        @Override
        public int size() {
            return handlers.size();
        }

        /**
         * 发布事件
         */
        protected void publish(final E event) {
            if (event != null) {
                //指定处理器
                Recipient recipient = event instanceof Recipient ? (Recipient) event : null;
                Object target;
                for (EventHandler<E> handler : handlers) {
                    if (recipient == null) {
                        handler.handle(event);
                    } else {
                        target = recipient.getTarget();
                        if (target == null || target == handler) {
                            handler.handle(event);
                        }
                    }
                }
            }
        }

        @Override
        public void start() {
            if (polling == null) {
                //如果再次打开，尝试重新绑定
                if (!group.contains(name)) {
                    group.publishers.putIfAbsent(name, this);
                }
                polling = group.dispatcher;
            }
            polling.start();
        }

        @Override
        public void close() {
            //移除，防止保留大量的无用的发布器
            if (group.remove(name) != null) {
                polling = null;
            }
        }

        @Override
        public boolean offer(final E event) {
            return event == null || polling == null ? false : polling.offer(new Message<>(event, consumer));
        }

        @Override
        public boolean offer(final E event, final long timeout, final TimeUnit timeUnit) {
            return event == null || polling == null ? false : polling.offer(new Message<>(event, consumer), timeout, timeUnit);
        }
    }

    /**
     * 消息
     *
     * @param <T>
     */
    protected static class Message<T extends Event> {

        protected T event;

        protected Consumer<T> consumer;

        public Message(final T event, final Consumer<T> consumer) {
            this.event = event;
            this.consumer = consumer;
        }

        public void publish() {
            consumer.accept(event);
        }
    }

    /**
     * 发布线程
     */
    protected static class Dispatcher<E extends Event> {

        protected String name;
        protected BlockingQueue<Message<E>> queue;
        protected Thread thread;
        protected AtomicBoolean started = new AtomicBoolean();

        public Dispatcher(String name, BlockingQueue<Message<E>> queue) {
            this.name = name;
            this.queue = queue;
        }

        public boolean offer(final Message<E> message) {
            return message == null ? false : queue.offer(message);
        }

        public boolean offer(final Message<E> message, final long timeout, final TimeUnit timeUnit) {
            if (message != null) {
                try {
                    return queue.offer(message, timeout, timeUnit == null ? TimeUnit.MILLISECONDS : timeUnit);
                } catch (InterruptedException e) {
                }
            }
            return false;
        }

        public void start() {
            if (started.compareAndSet(false, true)) {
                thread = new Thread(() -> {
                    Message message;
                    while (started.get() && !Thread.currentThread().isInterrupted()) {
                        try {
                            message = queue.poll(5000, TimeUnit.MILLISECONDS);
                            if (message != null) {
                                message.publish();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
                thread.setDaemon(true);
                thread.setName(name);
                thread.start();
            }
        }

        public void stop() {
            if (started.compareAndSet(true, false)) {
                if (thread != null) {
                    thread.interrupt();
                    thread = null;
                }
            }
        }
    }

    /**
     * 发布器分组
     *
     * @param <E>
     */
    protected static class PublisherGroup<E extends Event> {
        /**
         * 分组名称
         */
        protected String name;
        /**
         * 派发器配置
         */
        protected PublisherConfig config;
        /**
         * 线程
         */
        protected Dispatcher<E> dispatcher;

        protected Map<String, JPublisher<E>> publishers = new ConcurrentHashMap<>();

        /**
         * 构造函数
         *
         * @param name
         * @param config
         */
        public PublisherGroup(final String name, final PublisherConfig config) {
            this.name = name;
            this.config = config == null ? new PublisherConfig() : config;
            this.dispatcher = new Dispatcher("JEventBus-" + name,
                    this.config.getCapacity() > 0 ? new LinkedBlockingQueue<>(this.config.getCapacity()) : new LinkedTransferQueue<>());
        }

        protected boolean contains(final String name) {
            return name == null ? false : publishers.containsKey(name);
        }

        /**
         * 移除
         *
         * @param name
         */
        protected JPublisher<E> remove(final String name) {
            return publishers.remove(name);
        }

        /**
         * 获取发布器
         *
         * @param name
         * @return
         */
        public JPublisher<E> getPublisher(final String name) {
            return publishers.computeIfAbsent(name, o -> new JPublisher(name, this));
        }
    }

}
