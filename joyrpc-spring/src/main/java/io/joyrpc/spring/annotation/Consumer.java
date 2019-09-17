package io.joyrpc.spring.annotation;

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

import java.lang.annotation.*;

import static io.joyrpc.constants.Constants.DEFAULT_TIMEOUT;

/**
 * 消费者
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@Inherited
public @interface Consumer {

    /**
     * 服务别名，必填
     *
     * @return the string
     */
    String alias();

    /**
     * 集群策略，选填
     *
     * @return the string
     */
    String cluster() default "";

    /**
     * 失败后重试次数，选填
     *
     * @return the int
     */
    int retries() default -1;

    /**
     * 调用超时，选填
     *
     * @return the int
     */
    int timeout() default DEFAULT_TIMEOUT;

    /**
     * 直连地址，选填
     *
     * @return the string
     */
    String url() default "";

    /**
     * 负载均衡算法，选填
     *
     * @return the string
     */
    String loadbalance() default "";

    boolean sticky() default false;

    /**
     * 序列化方式，选填
     *
     * @return the string
     */
    String serialization() default "";

    /**
     * 是否延迟加载服务端连接，选填
     *
     * @return the boolean
     */
    boolean lazy() default false;

    /**
     * 是否jvm内部调用（provider和consumer配置在同一个jvm内，则走本地jvm内部，不走远程）
     */
    boolean injvm() default true;

    /**
     * 是否强依赖（即没有服务节点就启动失败）
     */
    boolean check() default false;

    /**
     * 路由规则引用，多个用英文逗号隔开。
     */
    String router() default "";

    int initSize() default -1;

    int minSize() default -1;

    String candidature() default "";

    String failoverWhenThrowable() default "";

    String failoverPredication() default "";

    String channelFactory() default "";

    int warmupWeight() default 100;

    int warmupDuration() default 300;

    /**
     * 接口下每方法的最大可并行执行请求数，配置-1关闭并发过滤器，等于0表示开启过滤但是不限制
     */
    int concurrency() default -1;

    /**
     * 过滤器配置，多个用逗号隔开
     */
    String[] filter() default {};

    /**
     * 注册中心配置
     */
    String registry() default "";


    /**
     * 是否注册，如果是false只订阅不注册
     */
    boolean register() default true;

    /**
     * 是否订阅服务
     */
    boolean subscribe() default true;


    /**
     * 代理类型
     */
    String proxy() default "";

    /**
     * 是否启动结果缓存
     */
    boolean cache() default false;

    /**
     * 结果缓存实现类
     */
    String cacheProvider() default "";

    /**
     * cache key 生成器
     */
    String cacheKeyGenerator() default "";

    /**
     * cache过期时间
     */
    long cacheExpireTime() default -1;

    /**
     * cache最大容量
     */
    int cacheCapacity() default Integer.MAX_VALUE;

    /**
     * 缓存值是否可空
     */
    boolean cacheNullable() default false;

    /**
     * 是否开启参数验证(jsr303)
     */
    boolean validation() default false;

    String interfaceValidator() default "";

    /**
     * 压缩算法，为空则不压缩
     */
    String compress() default "";

    /**
     * {key1, value1, key2, value2}
     *
     * @return
     */
    String[] parameters() default {};

    /**
     * methods support
     *
     * @return
     */
    Method[] methods() default {};
}
