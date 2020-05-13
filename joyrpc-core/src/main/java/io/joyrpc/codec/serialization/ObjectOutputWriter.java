package io.joyrpc.codec.serialization;

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
import java.io.ObjectOutput;
import java.util.Objects;

/**
 * ObjectOutput写入器
 */
public class ObjectOutputWriter implements ObjectWriter {

    protected final ObjectOutput output;

    public ObjectOutputWriter(final ObjectOutput output) {
        Objects.requireNonNull(output);
        this.output = output;
    }

    @Override
    public void writeObject(final Object v) throws IOException {
        //保持和原有一样
        if (v == null) {
            output.writeByte(0);
        } else {
            output.writeByte(1);
            output.writeObject(v);
        }
    }

    @Override
    public void write(final int b) throws IOException {
        //java的内置write，会block直到写入
        output.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        if (b != null) {
            output.write(b, off, len);
        }
    }

    @Override
    public void writeBoolean(final boolean v) throws IOException {
        output.writeBoolean(v);
    }

    @Override
    public void writeByte(final int v) throws IOException {
        output.writeByte(v);
    }

    @Override
    public void writeShort(final int v) throws IOException {
        output.writeShort(v);
    }

    @Override
    public void writeChar(final int v) throws IOException {
        output.writeChar(v);
    }

    @Override
    public void writeInt(final int v) throws IOException {
        output.writeInt(v);
    }

    @Override
    public void writeLong(final long v) throws IOException {
        output.writeLong(v);
    }

    @Override
    public void writeFloat(final float v) throws IOException {
        output.writeFloat(v);
    }

    @Override
    public void writeDouble(final double v) throws IOException {
        output.writeDouble(v);
    }

    @Override
    public void writeBytes(final String s) throws IOException {
        //保持和原有一样
        output.writeBytes(s);
    }

    @Override
    public void writeChars(final String s) throws IOException {
        output.writeChars(s);
    }

    @Override
    public void writeUTF(final String s) throws IOException {
        //保持和原有一样
        if (s == null) {
            output.writeInt(-1);
        } else {
            output.writeInt(s.length());
            output.writeUTF(s);
        }
    }

    @Override
    public void flush() throws IOException {
        output.flush();
    }

    @Override
    public void close() throws IOException {
        output.close();
    }

}
