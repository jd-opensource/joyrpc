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
import io.joyrpc.util.network.Lan;

import java.util.function.BiPredicate;

/**
 * 网段匹配
 *
 * @ClassName: IpPatternMatch
 * @date 2019年3月5日 上午11:39:26
 */
public class LanMatcher implements BiPredicate<Shard, RequestMessage<Invocation>> {

    protected Lan lan;

    public LanMatcher(final String pattern) {
        this.lan = new Lan(pattern, true);
    }

    @Override
    public boolean test(final Shard s, final RequestMessage<Invocation> request) {
        return lan.contains(s.getUrl().getHost());
    }

}
