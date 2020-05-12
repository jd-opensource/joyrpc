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
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import io.joyrpc.codec.serialization.CustomObjectSerializer;
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
        if (CustomObjectSerializer.class.isAssignableFrom(clazz)) {
            Registration registration = kryo.getRegistration(clazz);
            Serializer serializer = registration != null ? registration.getSerializer() : null;
            return serializer != null ? (T) serializer.read(kryo, input, clazz) : (T) kryo.readClassAndObject(input);
        } else {
            return (T) kryo.readClassAndObject(input);
        }
    }

    @Override
    public Object readObject() throws IOException {
        return kryo.readClassAndObject(input);
    }

    @Override
    public String readString(final Charset charset, final boolean shortLength) throws IOException {
        return input.readString();
    }

    @Override
    public int read() throws IOException {
        return input.read();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return input.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return input.read(b, off, len);
    }

    @Override
    public int available() throws IOException {
        return input.available();
    }

    @Override
    public void close() throws IOException {
        input.close();
    }

    @Override
    public boolean readBoolean() throws IOException {
        return input.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return input.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return input.readByteUnsigned();
    }

    @Override
    public short readShort() throws IOException {
        return input.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return input.readShortUnsigned();
    }

    @Override
    public char readChar() throws IOException {
        return input.readChar();
    }

    @Override
    public int readInt() throws IOException {
        return input.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return input.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return input.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return input.readDouble();
    }

    @Override
    public long skip(final long n) throws IOException {
        return input.skip(n);
    }
}
