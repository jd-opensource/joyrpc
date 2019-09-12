package io.joyrpc.transport.codec;

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

/**
 * @date: 2019/3/28
 */
public interface LengthFieldFrameCodec extends Codec {

    /**
     * 获取长度字段信息
     *
     * @return
     */
    LengthFieldFrame getLengthFieldFrame();

    @Override
    default String binder() {
        return "lengthFieldFrame";
    }

    /**
     * 长度字段信息
     */
    class LengthFieldFrame {

        protected int maxFrameLength;
        protected int lengthFieldOffset;
        protected int lengthFieldLength;
        protected int lengthAdjustment;
        protected int initialBytesToStrip;

        public LengthFieldFrame() {

        }

        public LengthFieldFrame(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
            this.maxFrameLength = maxFrameLength;
            this.lengthFieldOffset = lengthFieldOffset;
            this.lengthFieldLength = lengthFieldLength;
            this.lengthAdjustment = lengthAdjustment;
            this.initialBytesToStrip = initialBytesToStrip;
        }

        public LengthFieldFrame(int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
            this.lengthFieldOffset = lengthFieldOffset;
            this.lengthFieldLength = lengthFieldLength;
            this.lengthAdjustment = lengthAdjustment;
            this.initialBytesToStrip = initialBytesToStrip;
        }

        public int getMaxFrameLength() {
            return maxFrameLength;
        }

        public void setMaxFrameLength(int maxFrameLength) {
            this.maxFrameLength = maxFrameLength;
        }

        public int getLengthFieldOffset() {
            return lengthFieldOffset;
        }

        public void setLengthFieldOffset(int lengthFieldOffset) {
            this.lengthFieldOffset = lengthFieldOffset;
        }

        public int getLengthFieldLength() {
            return lengthFieldLength;
        }

        public void setLengthFieldLength(int lengthFieldLength) {
            this.lengthFieldLength = lengthFieldLength;
        }

        public int getLengthAdjustment() {
            return lengthAdjustment;
        }

        public void setLengthAdjustment(int lengthAdjustment) {
            this.lengthAdjustment = lengthAdjustment;
        }

        public int getInitialBytesToStrip() {
            return initialBytesToStrip;
        }

        public void setInitialBytesToStrip(int initialBytesToStrip) {
            this.initialBytesToStrip = initialBytesToStrip;
        }
    }
}
