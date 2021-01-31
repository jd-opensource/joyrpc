package io.joyrpc.transport.channel;

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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;

/**
 * 包装处理链的处理器
 */
public class ChainChannelHandler implements ChannelHandler {
    /**
     * 业务处理链
     */
    protected ChannelHandlerChain chain;
    /**
     * 线程池
     */
    protected ThreadPoolExecutor executor;
    /**
     * 运行函数
     */
    protected Function<Runnable, Runnable> runFunc;

    public ChainChannelHandler(ChannelHandlerChain chain) {
        this(chain, null);
    }

    public ChainChannelHandler(ChannelHandlerChain chain, ThreadPoolExecutor executor) {
        this.chain = chain;
        this.executor = executor;
        if (executor != null) {
            BlockingQueue queue = executor.getQueue();
            //判断是否是优先级队列
            if (queue instanceof PriorityBlockingQueue && ((PriorityBlockingQueue) queue).comparator() == null) {
                runFunc = ComparableRunnable::new;
            } else {
                runFunc = (r) -> r;
            }
        }
    }

    @Override
    public void active(final ChannelContext context) {
        for (ChannelHandler handler : chain.handlers) {
            if (context.isEnd()) {
                break;
            }
            handler.active(context);
        }
    }

    @Override
    public void inactive(final ChannelContext context) {
        for (ChannelHandler handler : chain.handlers) {
            if (context.isEnd()) {
                break;
            }
            handler.inactive(context);
        }
    }

    @Override
    public Object received(final ChannelContext context, final Object message) {
        if (executor != null) {
            executor.execute(runFunc.apply(() -> {
                try {
                    doReceived(context, message);
                } catch (Exception e) {
                    //发生异常，触发异常事件
                    context.getChannel().fireCaught(e);
                }
            }));
            return null;
        } else {
            //在IO线程中，发生异常，有底层插件捕获
            return doReceived(context, message);
        }
    }

    /**
     * 接收消息处理
     *
     * @param context 上下文
     * @param message 消息
     * @return 转换的消息
     */
    protected Object doReceived(final ChannelContext context, final Object message) {
        Object msg = message;
        for (ChannelHandler handler : chain.handlers) {
            if (context.isEnd()) {
                return msg;
            }
            //调用处理器进行处理，可以进行类型转换
            msg = handler.received(context, msg);
        }
        return msg;
    }

    @Override
    public Object wrote(final ChannelContext context, final Object message) {
        //在IO线程中，发生异常，有底层插件捕获
        Object msg = message;
        for (ChannelHandler handler : chain.handlers) {
            if (context.isEnd()) {
                return msg;
            }
            msg = handler.wrote(context, msg);
        }
        return msg;
    }

    @Override
    public void caught(final ChannelContext context, final Throwable throwable) {
        for (ChannelHandler handler : chain.handlers) {
            if (context.isEnd()) {
                break;
            }
            handler.caught(context, throwable);
        }
    }

    /**
     * 运行
     */
    protected class ComparableRunnable implements Runnable, Comparable {

        /**
         * 包装的运行
         */
        protected Runnable runnable;

        public ComparableRunnable(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public int compareTo(Object o) {
            return 0;
        }

        @Override
        public void run() {
            runnable.run();
        }
    }

}
