package io.joyrpc.transport.netty4.pipeline;

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

import io.joyrpc.extension.Extensible;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelChain;
import io.joyrpc.transport.codec.Codec;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;

import java.util.function.BiFunction;

/**
 * 管道工厂
 */
@Extensible("pipeline")
public interface PipelineFactory {

    String HANDLER = "handler";
    String DECODER = "decoder";
    String ENCODER = "encoder";
    String CODEC = "codec";
    String HTTP_AGGREGATOR = "http-aggregator";
    String HTTP_RESPONSE_CONVERTER = "http-response-converter";

    /**
     * 返回业务处理函数元数据数组
     *
     * @return 业务处理函数元数据数组
     */
    HandlerDefinition<ChannelChain>[] handlers();

    /**
     * 返回解码器元数据数组
     *
     * @return 解码器元数据数组
     */
    HandlerDefinition<Codec>[] decoders();

    /**
     * 返回编码器元数据数组
     *
     * @return 编码器元数据数组
     */
    HandlerDefinition<Codec>[] encoders();

    /**
     * 构建处理链
     *
     * @param pipeline 管道
     * @param codec    编解码
     * @param chain    处理链
     * @param channel  连接通道
     */
    default void build(final ChannelPipeline pipeline, final Codec codec, final ChannelChain chain, final Channel channel) {
        if (codec != null) {
            //解码器
            for (HandlerDefinition<Codec> meta : decoders()) {
                pipeline.addLast(meta.name, meta.function.apply(codec, channel));
            }
            //编码器
            for (HandlerDefinition<Codec> meta : encoders()) {
                pipeline.addLast(meta.name, meta.function.apply(codec, channel));
            }
        }
        //处理链
        if (chain != null) {
            for (HandlerDefinition<ChannelChain> meta : handlers()) {
                pipeline.addLast(meta.name, meta.function.apply(chain, channel));
            }
        }
    }

    /**
     * 处理器元数据
     *
     * @param <T>
     */
    class HandlerDefinition<T> {
        /**
         * 名称
         */
        protected String name;
        /**
         * 函数
         */
        protected BiFunction<T, Channel, ChannelHandler> function;

        /**
         * 构造函数
         *
         * @param name     名称
         * @param function 函数
         */
        public HandlerDefinition(String name, BiFunction<T, Channel, ChannelHandler> function) {
            this.name = name;
            this.function = function;
        }

        public String getName() {
            return name;
        }

        public BiFunction<T, Channel, ChannelHandler> getFunction() {
            return function;
        }
    }

    /**
     * 处理链元数据
     */
    class ChainDefinition extends HandlerDefinition<ChannelChain> {

        /**
         * 构造函数
         *
         * @param name     名称
         * @param function 函数
         */
        public ChainDefinition(String name, BiFunction<ChannelChain, Channel, ChannelHandler> function) {
            super(name, function);
        }
    }

    /**
     * 编解码元数据
     */
    class CodecDefinition extends HandlerDefinition<Codec> {

        /**
         * 构造函数
         *
         * @param name     名称
         * @param function 函数
         */
        public CodecDefinition(String name, BiFunction<Codec, Channel, ChannelHandler> function) {
            super(name, function);
        }
    }

}
