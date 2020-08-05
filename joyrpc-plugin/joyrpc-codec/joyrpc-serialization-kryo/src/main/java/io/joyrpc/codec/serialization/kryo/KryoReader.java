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
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import io.joyrpc.codec.serialization.ObjectReader;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Kryo数据读入器
 */
public class KryoReader implements ObjectReader {

    protected Kryo kryo;

    protected Input input;

    public KryoReader(Kryo kryo, Input input) {
        this.kryo = kryo;
        this.input = input;
    }

    @Override
    public <T> T readObject(final Class<T> clazz) throws IOException {
        try {
            return (T) kryo.readClassAndObject(input);
        } catch (KryoException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public Object readObject() throws IOException {
        try {
            return kryo.readClassAndObject(input);
        } catch (KryoException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public String readString(final Charset charset, final boolean shortLength) throws IOException {
        try {
            return input.readString();
        } catch (KryoException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public int read() throws IOException {
        try {
            return input.read();
        } catch (KryoException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public int read(final byte[] b) throws IOException {
        try {
            return input.read(b);
        } catch (KryoException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            return input.read(b, off, len);
        } catch (KryoException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public int available() throws IOException {
        try {
            return input.available();
        } catch (KryoException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            input.close();
        } catch (KryoException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public boolean readBoolean() throws IOException {
        try {
            return input.readBoolean();
        } catch (KryoException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public byte readByte() throws IOException {
        try {
            return input.readByte();
        } catch (KryoException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public int readUnsignedByte() throws IOException {
        try {
            return input.readByteUnsigned();
        } catch (KryoException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public short readShort() throws IOException {
        try {
            return input.readShort();
        } catch (KryoException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public int readUnsignedShort() throws IOException {
        try {
            return input.readShortUnsigned();
        } catch (KryoException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public char readChar() throws IOException {
        try {
            return input.readChar();
        } catch (KryoException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public int readInt() throws IOException {
        try {
            return input.readInt();
        } catch (KryoException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public long readLong() throws IOException {
        try {
            return input.readLong();
        } catch (KryoException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public float readFloat() throws IOException {
        try {
            return input.readFloat();
        } catch (KryoException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public double readDouble() throws IOException {
        try {
            return input.readDouble();
        } catch (KryoException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public long skip(final long n) throws IOException {
        try {
            return input.skip(n);
        } catch (KryoException e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
