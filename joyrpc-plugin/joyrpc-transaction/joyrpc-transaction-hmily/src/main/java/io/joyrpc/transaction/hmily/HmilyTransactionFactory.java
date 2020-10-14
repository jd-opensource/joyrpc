package io.joyrpc.transaction.hmily;

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

import io.joyrpc.extension.Extension;
import io.joyrpc.transaction.TransactionFactory;
import io.joyrpc.transaction.TransactionOption;
import org.dromara.hmily.annotation.Hmily;

import java.lang.reflect.Method;

/**
 * hmily事务提供者
 */
@Extension("hmily")
public class HmilyTransactionFactory implements TransactionFactory {

    @Override
    public TransactionOption create(final Class<?> clazz, final Method method) {
        if (method == null) {
            return null;
        }
        Hmily hmily = method.getAnnotation(Hmily.class);
        return hmily == null ? null : new HmilyTransactionOption(hmily);
    }
}
