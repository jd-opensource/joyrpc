package io.protostuff;

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

import java.io.InputStream;

/**
 * Protostuf读入器
 */
public class ProtostuffReader extends AbstractProtostuffReader {

    /**
     * 构造函数
     *
     * @param schema
     * @param inputStream
     * @param buffer
     */
    public ProtostuffReader(Schema schema, InputStream inputStream, LinkedBuffer buffer) {
        super(schema, inputStream, new CodedInput(inputStream, buffer.buffer, true));
    }
}
