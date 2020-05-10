package io.joyrpc.com.caucho.hessian.io;

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

import java.io.IOException;

/**
 * 自动注册反序列化
 */
public interface AutowiredObjectDeserializer extends Deserializer {

    @Override
    default boolean isReadResolve() {
        return false;
    }

    @Override
    default Object readList(AbstractHessianInput in, int length) throws IOException {
        return null;
    }

    @Override
    default Object readLengthList(AbstractHessianInput in, int length) throws IOException {
        return null;
    }

    @Override
    default Object readMap(AbstractHessianInput in) throws IOException {
        return null;
    }

    @Override
    default Object[] createFields(int len) {
        return new Object[0];
    }

    @Override
    default Object createField(String name) {
        return null;
    }

    @Override
    default Object readObject(AbstractHessianInput in, Object[] fields) throws IOException {
        return null;
    }

    @Override
    default Object readObject(AbstractHessianInput in, String[] fieldNames) throws IOException {
        return null;
    }

    /**
     * 注册的类型
     *
     * @return 类型
     */
    Class<?> getType();
}
