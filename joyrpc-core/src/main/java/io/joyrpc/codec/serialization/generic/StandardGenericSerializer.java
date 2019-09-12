package io.joyrpc.codec.serialization.generic;

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

import io.joyrpc.codec.serialization.GenericSerializer;
import io.joyrpc.exception.CodecException;
import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.util.PojoUtils;

/**
 * 默认泛型序列化器
 */
@Extension("standard")
public class StandardGenericSerializer implements GenericSerializer {

    @Override
    public Object serialize(final Object object) throws CodecException {
        return PojoUtils.generalize(object);
    }

    @Override
    public Object[] deserialize(final Invocation invocation) throws CodecException {
        Object[] genericArgs = invocation.getArgs();
        Object[] paramArgs = genericArgs == null || genericArgs.length < 3 ? new Object[0] : (Object[]) genericArgs[2];
        return PojoUtils.realize(paramArgs, invocation.getArgClasses(), invocation.getMethod().getGenericParameterTypes());
    }
}
