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

import io.joyrpc.codec.serialization.ObjectReader;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Protostuf读入器
 */
public class ProtostuffReader implements ObjectReader {
    /**
     * Schema
     */
    protected Schema schema;
    /**
     * 缓冲区
     */
    protected LinkedBuffer buffer;
    /**
     * 输入流
     */
    protected InputStream inputStream;
    /**
     * 输入
     */
    protected CodedInput input;

    /**
     * 构造函数
     *
     * @param schema
     * @param buffer
     * @param inputStream
     */
    public ProtostuffReader(Schema schema, LinkedBuffer buffer, InputStream inputStream) {
        this.schema = schema;
        this.buffer = buffer;
        this.inputStream = inputStream;
        this.input = new CodedInput(inputStream, buffer.buffer, true);
    }

    @Override
    public Object readObject() throws IOException {
        Object message = schema.newMessage();
        schema.mergeFrom(input, message);
        input.checkLastTagWas(0);
        return message;
    }

    @Override
    public String readUTF() throws IOException {
        return input.readString();
    }

    @Override
    public int read() throws IOException {
        return input.readInt32();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            return 0;
        }
        int end = off + len;
        if (off < 0 || len < 0 || end > b.length || end < 0) {
            throw new IndexOutOfBoundsException();
        }
        for (int i = 0; i < len; i++) {
            try {
                b[i] = input.readRawByte();
            } catch (EOFException e) {
                return i;
            } catch (ProtobufException e) {
                return i;
            } catch (IllegalStateException e) {
                return i;
            }
        }
        return len;
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    @Override
    public boolean readBoolean() throws IOException {
        return input.readBool();
    }

    @Override
    public byte readByte() throws IOException {
        return input.readRawByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return input.readRawByte() & 0xFFFFFFFF;
    }

    @Override
    public short readShort() throws IOException {
        return (short) input.readInt32();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return input.readUInt32();
    }

    @Override
    public char readChar() throws IOException {
        return (char) input.readRawVarint32();
    }

    @Override
    public int readInt() throws IOException {
        return input.readInt32();
    }

    @Override
    public long readLong() throws IOException {
        return input.readInt64();
    }

    @Override
    public float readFloat() throws IOException {
        return input.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return input.readDouble();
    }
}
