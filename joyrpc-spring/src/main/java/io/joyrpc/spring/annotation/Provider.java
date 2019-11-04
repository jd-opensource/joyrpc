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

/**
 * 服务提供者
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface Provider {

    /**
     * Bean名称
     *
     * @return
     */
    String name() default "";

    /**
     * 接口类
     *
     * @return
     */
    Class<?> interfaceClass() default void.class;

    /**
     * 服务别名，必填
     *
     * @return the string
     */
    String alias();

    /**
     * 注册到的服务端
     *
     * @return the server
     */

    String server() default "";

    /**
     * 代理类型
     *
     * @return
     */
    String proxy() default "";

    /**
     * 是否注册到注册中心
     *
     * @return the boolean
     */
    boolean register() default true;

    /**
     * 是否动态发布服务
     *
     * @return the boolean
     */
    boolean dynamic() default true;

    /**
     * 服务端权重
     *
     * @return the int
     */
    int weight() default 100;

    /**
     * 服务发布延迟,单位毫秒，默认0，配置为-1代表spring加载完毕
     */
    int delay() default -1;

    /**
     * 包含的方法
     */
    String[] include() default {};

    /**
     * 不发布的方法列表，逗号分隔
     */
    String[] exclude() default {};

    /**
     * 接口下每方法的最大可并行执行请求数，配置-1关闭并发过滤器，等于0表示开启过滤但是不限制
     */
    int concurrency() default -1;

    /**
     * 过滤器配置，多个用逗号隔开
     */
    String[] filter() default {};

    /**
     * 注册中心配置，可配置多个
     */
    String[] registry() default {};

    /**
     * 是否订阅服务
     */
    boolean subscribe() default true;

    /**
     * 远程调用超时时间(毫秒)
     */
    int timeout() default -1;

    /**
     * 压缩算法，为空则不压缩
     */
    String compress() default "";

    /**
     * 是否启动结果缓存
     */
    boolean cache() default false;

    /**
     * 结果缓存插件名称
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

    /**
     * 是否要验证
     *
     * @return
     */
    boolean enableValidator() default true;

    /**
     * 接口验证器
     *
     * @return
     */
    String interfaceValidator() default "";

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
