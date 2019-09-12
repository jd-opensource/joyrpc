package io.joyrpc.filter.provider;

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
import io.joyrpc.filter.AbstractCacheFilter;
import io.joyrpc.filter.ProviderFilter;

/**
 * @description: consumer结果缓存过滤器, 需要扩展实现Cache接口
 */
@Extension(value = "cache", order = ProviderFilter.CACHE_ORDER)
public class CacheFilter extends AbstractCacheFilter implements ProviderFilter {

    @Override
    public int type() {
        return SYSTEM_GLOBAL;
    }
}

