package io.joyrpc.cluster.distribution.selector.method.predicate;

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

import io.joyrpc.cluster.Shard;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.function.BiPredicate;

/**
 * @ClassName: MethodNameMatch
 * @Description: 方法名匹配
 * @date 2019年3月4日 下午5:23:12
 */
public class MethodNameMatcher implements BiPredicate<Shard, RequestMessage<Invocation>> {

    /**
     * 方法名称
     */
    protected String methodName;

    /**
     * 操作符
     */
    protected Operator operator;

    public MethodNameMatcher(final String methodName, final Operator operator) {
        this.methodName = methodName;
        this.operator = operator == null ? Operator.eq : operator;
    }

    @Override
    public boolean test(final Shard s, final RequestMessage<Invocation> r) {
        return operator.match(methodName, r.getPayLoad().getMethodName());
    }

}
