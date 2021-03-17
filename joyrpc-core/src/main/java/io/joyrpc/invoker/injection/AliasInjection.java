package io.joyrpc.invoker.injection;

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

import io.joyrpc.cluster.Node;
import io.joyrpc.invoker.injection.NodeReqInjection;
import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.Objects;

import static io.joyrpc.constants.Constants.INTERNAL_KEY_CONSUMERALIAS;

/**
 * 注入别名
 */
@Extension("alias")
public class AliasInjection implements NodeReqInjection {

    @Override
    public boolean test() {
        return true;
    }

    @Override
    public void inject(final RequestMessage<Invocation> request, final Node node) {
        //兼容老的注册中心，如果是动态别名的话，注册中心推给consumer的是原别名
        // 按照当前配置的别名调用服务会报别名不一致的错误，需要替换成node真实别名
        String alias = node.getAlias();
        // 传递用户配置别名
        if (alias != null && !alias.isEmpty()) {
            Invocation invocation = request.getPayLoad();
            String value = invocation.getAlias();
            if (!Objects.equals(alias, value)) {
                //模拟新分组调用
                invocation.setAlias(alias);
                //上下文设置原有的分组
                invocation.addAttachment(INTERNAL_KEY_CONSUMERALIAS, value);
            }
        }
    }
}
