package io.joyrpc.codec.compression.snappy;

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @date: 2019/8/15
 */
public class SnappyInputStream extends ByteArrayInputStream {

    public SnappyInputStream(InputStream inputStream) throws IOException {
        super(new byte[0]);
        int size = inputStream.available();
        if (size > 0) {
            byte[] source = new byte[size];
            inputStream.read(source);
            this.buf = SnappyDecompressor.uncompress(source, 0, source.length);
            this.pos = 0;
            this.count = buf.length;
        }
    }

}
