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
 * @ClassName: MethodArgMatch
 * @Description: 方法参数匹配
 * @date 2019年3月5日 上午10:53:24
 */
public class ParameterMatcher implements BiPredicate<Shard, RequestMessage<Invocation>> {

    protected int paramIndex;
    protected String leftValue;
    protected Operator operator;

    public ParameterMatcher(final int paramIndex, final String leftValue, final Operator operator) {
        this.paramIndex = paramIndex;
        this.leftValue = leftValue;
        this.operator = operator == null ? Operator.eq : operator;
    }

    @Override
    public boolean test(final Shard s, final RequestMessage<Invocation> r) {
        Object[] args = r.getPayLoad().getArgs();
        if (paramIndex >= args.length || paramIndex < 0) {
            return false;
        }
        return operator.match(args[paramIndex].toString(), leftValue);
    }

}
