package io.joyrpc.com.caucho.hessian.io.java8;

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

import io.joyrpc.com.caucho.hessian.io.HessianHandle;

import java.io.Serializable;

/**
 * Java8时间包装器
 */
public interface Java8TimeWrapper<T> extends Serializable, HessianHandle {

    /**
     * 包装对外
     *
     * @param time
     */
    void wrap(T time);

    /**
     * 实现了HessianHandle接口，hessian会调用该方法读取
     *
     * @return
     */
    T readResolve();
}
