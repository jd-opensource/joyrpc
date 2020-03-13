package io.joyrpc.cache.spring;

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

import io.joyrpc.cache.AbstractExpressionCacheKeyGenerator;
import io.joyrpc.expression.Expression;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;

import static io.joyrpc.Plugin.EXPRESSION_PROVIDER;


/**
 * Spel表达式缓存键生成器
 */
@Extension("spel")
@ConditionalOnClass({"org.springframework.expression.Expression"})
public class SpelCacheKeyGenerator extends AbstractExpressionCacheKeyGenerator {

    @Override
    protected Expression create(final String el) {
        return EXPRESSION_PROVIDER.get("spel").build(el);
    }
}
