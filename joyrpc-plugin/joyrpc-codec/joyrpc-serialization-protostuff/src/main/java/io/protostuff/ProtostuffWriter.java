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

import io.joyrpc.codec.serialization.ObjectWriter;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Protostuff写入器
 */
public class ProtostuffWriter implements ObjectWriter {

    protected Schema schema;
    protected LinkedBuffer buffer;
    protected OutputStream outputStream;
    protected Output output;
    protected WriteSession session;
    protected WriteSink sink;

    /**
     * 构造函数
     *
     * @param schema
     * @param buffer
     * @param output
     * @param outputStream
     */
    public ProtostuffWriter(Schema schema, LinkedBuffer buffer, Output output, OutputStream outputStream) {
        this(schema, buffer, output, (WriteSession) output, outputStream);
    }

    /**
     * 构造函数
     *
     * @param schema
     * @param buffer
     * @param output
     * @param session
     * @param outputStream
     */
    public ProtostuffWriter(Schema schema, LinkedBuffer buffer, Output output, WriteSession session, OutputStream outputStream) {
        this.schema = schema;
        this.buffer = buffer;
        this.outputStream = outputStream;
        this.output = output;
        this.session = session;
        this.output = new ProtostuffOutput(buffer, outputStream);
        this.sink = session.sink;
    }

    @Override
    public void writeObject(final Object obj) throws IOException {
        schema.writeTo(output, obj);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        session.tail = sink.writeByteArray(b, off, len, session, session.tail);
    }

    @Override
    public void flush() throws IOException {
        LinkedBuffer.writeTo(outputStream, buffer);
    }

    @Override
    public void close() throws IOException {
        release();
        outputStream.close();
    }

    @Override
    public void release() {
        buffer.clear();
    }

    @Override
    public void writeBoolean(final boolean v) throws IOException {
        session.tail = sink.writeVarInt32(v ? 1 : 0, session, session.tail);
    }

    @Override
    public void writeByte(final int v) throws IOException {
        session.tail = sink.writeByte((byte) v, session, session.tail);
    }

    @Override
    public void writeShort(final int v) throws IOException {
        session.tail = sink.writeVarInt32(v, session, session.tail);
    }

    @Override
    public void writeChar(final int v) throws IOException {
        session.tail = sink.writeVarInt32(v, session, session.tail);
    }

    @Override
    public void writeInt(final int v) throws IOException {
        session.tail = sink.writeVarInt32(v, session, session.tail);
    }

    @Override
    public void writeLong(final long v) throws IOException {
        session.tail = sink.writeVarInt64(v, session, session.tail);
    }

    @Override
    public void writeFloat(final float v) throws IOException {
        session.tail = sink.writeFloat(v, session, session.tail);
    }

    @Override
    public void writeDouble(final double v) throws IOException {
        session.tail = sink.writeDouble(v, session, session.tail);
    }

    @Override
    public void writeUTF(final String s) throws IOException {
        session.tail = sink.writeStrUTF8(s, session, session.tail);
    }

}
