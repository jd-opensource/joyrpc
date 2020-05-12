package io.joyrpc.codec.serialization.kryo;

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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import io.joyrpc.codec.serialization.ObjectWriter;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * kryo写入器
 */
public class KryoWriter implements ObjectWriter {

    protected Kryo kryo;
    protected Output output;

    public KryoWriter(Kryo kryo, Output output) {
        this.kryo = kryo;
        this.output = output;
    }

    @Override
    public void writeObject(final Object obj) throws IOException {
        kryo.writeClassAndObject(output, obj);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        output.writeBytes(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        output.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        output.flush();
    }

    @Override
    public void close() throws IOException {
        output.close();
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
        output.writeChar((char) v);
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
    public void writeChars(final String s) throws IOException {
        if (s == null) {
            return;
        }
        int len = s.length();
        for (int i = 0; i < len; i++) {
            output.writeChar(s.charAt(i));
        }
    }

    @Override
    public void writeString(final String value, final Charset charset, final boolean zeroNull, final boolean shortLength) throws IOException {
        output.writeString(value);
    }
}
