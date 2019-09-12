package io.joyrpc.extension;

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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日期函数
 */
public interface DateParser {

    /**
     * 把字符串转换成日期
     *
     * @param value
     * @return
     * @throws ParseException
     */
    Date parse(String value) throws ParseException;


    /**
     * 基于日期格式化进行转换
     */
    class SimpleDateParser implements DateParser {
        //日期格式化
        protected SimpleDateFormat format;

        public SimpleDateParser(SimpleDateFormat format) {
            this.format = format;
        }

        @Override
        public Date parse(String value) throws ParseException {
            return format == null || value == null || value.isEmpty() ? null : format.parse(value);
        }
    }
}
