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
import java.io.ObjectInput;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Java读入器
 */
public class ObjectInputReader implements ObjectReader {

    protected final ObjectInput input;

    public ObjectInputReader(ObjectInput input) {
        Objects.requireNonNull(input);
        this.input = input;
    }

    @Override
    public char readChar() throws IOException {
        return input.readChar();
    }

    @Override
    public void readFully(final byte[] b, final int off, final int len) throws IOException {
        input.readFully(b, off, len);
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
        return input.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        return input.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return input.readUnsignedShort();
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
    public String readLine() throws IOException {
        return input.readLine();
    }

    @Override
    public String readUTF() throws IOException {
        return input.readUTF();
    }

    @Override
    public String readString(final Charset charset, final boolean shortLength) throws IOException {
        int len = shortLength ? input.readUnsignedShort() : input.readInt();
        if (len < 0) {
            return null;
        } else if (len == 0) {
            return EMPTY_STRING;
        }
        byte[] bytes = new byte[len];
        input.read(bytes);
        return new String(bytes, charset == null ? StandardCharsets.UTF_8 : charset);
    }

    @Override
    public Object readObject() throws IOException {
        try {
            return input.readObject();
        } catch (ClassNotFoundException e) {
            return new IOException(e.getMessage(), e);
        }
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
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return input.read(b, off, len);
    }

    @Override
    public int available() throws IOException {
        return input.available();
    }

    @Override
    public int skipBytes(final int n) throws IOException {
        return input.skipBytes(n);
    }

    @Override
    public long skip(final long n) throws IOException {
        return input.skip(n);
    }

    @Override
    public void close() throws IOException {
        input.close();
    }

}
