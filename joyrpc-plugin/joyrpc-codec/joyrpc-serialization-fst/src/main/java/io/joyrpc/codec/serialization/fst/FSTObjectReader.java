package io.joyrpc.codec.serialization.fst;

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

import io.joyrpc.codec.serialization.ObjectInputReader;
import org.nustaq.serialization.AutowiredObjectSerializer;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectSerializer;

import java.io.IOException;

/**
 * FST对象读取器
 */
public class FSTObjectReader extends ObjectInputReader {

    protected FSTConfiguration fst;

    public FSTObjectReader(FSTObjectInput input) {
        super(input);
        this.fst = input.getConf();
    }

    @Override
    public <T> T readObject(Class<T> clazz) throws IOException {
        FSTObjectSerializer serializer = fst.getCLInfoRegistry().getSerializerRegistry().getSerializer(clazz);
        try {
            if (serializer instanceof AutowiredObjectSerializer) {
                Object instantiate = serializer.instantiate(clazz, (FSTObjectInput) input, null, null, 0);
                serializer.readObject((FSTObjectInput) input, instantiate, null, null);
                return (T) instantiate;
            } else {
                return (T) ((FSTObjectInput) input).readObject(clazz);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
