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
 * @date 5/7/2019
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
@Inherited
public @interface Method {
    String name();

    /**
     * {key1, value1, key2, value2}
     *
     * @return
     */
    String[] parameters() default {};

    int timeout() default -1;

    int retries() default -1;

    boolean validation() default false;

    int concurrency() default -1;

    boolean cache() default false;

    String cacheProvider() default "";

    String cacheKeyGenerator() default "";

    long cacheExpireTime() default -1;

    int cacheCapacity() default Integer.MAX_VALUE;

    boolean cacheNullable() default false;

    String compress() default "";

    int dstParam() default -1;


}
