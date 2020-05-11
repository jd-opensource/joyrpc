package io.joyrpc.protocol.dubbo.serialization.fst;

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
import io.joyrpc.codec.serialization.ObjectOutputWriter;
import io.joyrpc.protocol.dubbo.message.DubboResponsePayload;
import org.nustaq.serialization.AutowiredObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.IOException;

/**
 * DubboResponsePayload序列化
 */
public class DubboResponsePayloadSerializer implements AutowiredObjectSerializer {

    @Override
    public Class<?> getType() {
        return DubboResponsePayload.class;
    }

    @Override
    public void writeObject(final FSTObjectOutput out,
                            final Object toWrite,
                            final FSTClazzInfo clzInfo,
                            final FSTClazzInfo.FSTFieldInfo referencedBy,
                            final int streamPosition) throws IOException {
        ((DubboResponsePayload) toWrite).write(new ObjectOutputWriter(out));
    }

    @Override
    public void readObject(final FSTObjectInput in,
                           final Object toRead,
                           final FSTClazzInfo clzInfo,
                           final FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
        ((DubboResponsePayload) toRead).read(new ObjectInputReader(in));
    }

    @Override
    public boolean willHandleClass(final Class cl) {
        return false;
    }

    @Override
    public boolean alwaysCopy() {
        return false;
    }

    @Override
    public Object instantiate(final Class objectClass, final FSTObjectInput fstObjectInput,
                              final FSTClazzInfo serializationInfo,
                              final FSTClazzInfo.FSTFieldInfo referencee,
                              final int streamPosition) throws Exception {
        return new DubboResponsePayload();
    }

}
