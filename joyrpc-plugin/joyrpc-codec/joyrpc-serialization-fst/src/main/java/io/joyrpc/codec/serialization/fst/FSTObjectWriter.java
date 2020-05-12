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

import io.joyrpc.codec.serialization.ObjectOutputWriter;
import org.nustaq.serialization.AutowiredObjectSerializer;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectOutput;
import org.nustaq.serialization.FSTObjectSerializer;

import java.io.IOException;

/**
 * FST对象写入器
 */
public class FSTObjectWriter extends ObjectOutputWriter {

    protected FSTConfiguration fst;

    public FSTObjectWriter(FSTObjectOutput output) {
        super(output);
        fst = output.getConf();
    }

    @Override
    public void writeObject(final Object v) throws IOException {
        Class clazz = v == null ? null : v.getClass();
        FSTObjectSerializer serializer = clazz == null ? null : fst.getCLInfoRegistry().getSerializerRegistry().getSerializer(clazz);
        try {
            if (serializer instanceof AutowiredObjectSerializer) {
                serializer.writeObject((FSTObjectOutput) output, v, null, null, 0);
            } else {
                output.writeObject(v);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
