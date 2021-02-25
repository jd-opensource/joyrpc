package io.joyrpc.protocol.dubbo.serialization.protostuff;

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

import io.joyrpc.codec.serialization.ObjectWriter;
import io.protostuff.GraphIOUtil;
import io.protostuff.LinkedBuffer;
import io.protostuff.Schema;
import org.apache.dubbo.common.serialize.protostuff.Wrapper;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static io.protostuff.runtime.RuntimeSchema.getSchema;

/**
 * DubboProtostuff序列化
 */
public class DubboProtostuffWriter implements ObjectWriter {

    protected Schema schema;
    protected LinkedBuffer buffer;
    protected DataOutputStream dos;

    public DubboProtostuffWriter(Schema schema, OutputStream outputStream, LinkedBuffer buffer) {
        this.schema = schema;
        this.dos = new DataOutputStream(outputStream);
        this.buffer = buffer;
    }

    @Override
    public void writeObject(final Object obj) throws IOException {
        byte[] bytes;
        byte[] classNameBytes;
        Schema mySchema;
        try {
            if (obj == null || Wrapper.needWrapper(obj)) {
                mySchema = getSchema(Wrapper.class);
                classNameBytes = Wrapper.CLASS_NAMES;
                bytes = GraphIOUtil.toByteArray(new Wrapper(obj), mySchema, buffer);
            } else {
                Class<?> objClass = obj.getClass();
                mySchema = objClass == schema.typeClass() ? schema : getSchema(objClass);
                classNameBytes = objClass.getName().getBytes();
                bytes = GraphIOUtil.toByteArray(obj, mySchema, buffer);
            }
        } finally {
            buffer.clear();
        }
        dos.writeInt(classNameBytes.length);
        dos.writeInt(bytes.length);
        dos.write(classNameBytes);
        dos.write(bytes);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        dos.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        dos.flush();
    }

    @Override
    public void release() {
        buffer.clear();
    }

    @Override
    public void close() throws IOException {
        dos.close();
        buffer.clear();
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        dos.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        dos.writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        dos.writeShort(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        dos.writeChar(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        dos.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        dos.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        dos.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        dos.writeDouble(v);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        writeString(s, StandardCharsets.UTF_8, true, false);
    }

    @Override
    public void writeString(String v) throws IOException {
        writeString(v, StandardCharsets.UTF_8, true, false);
    }

    @Override
    public void writeString(String value, Charset charset) throws IOException {
        writeString(value, charset, true, false);
    }
}
