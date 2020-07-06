package io.joyrpc.proxy.jdk;

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

import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.proxy.JCompiler;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static javax.tools.JavaFileObject.Kind.SOURCE;

/**
 * Java运行时编译器
 */
@Extension("jdk")
@ConditionalOnClass("javax.tools.ToolProvider")
public class JdkCompiler implements JCompiler {

    /**
     * 编译
     *
     * @param className 类名
     * @param context   内容
     * @return 编译好的内
     * @throws ClassNotFoundException 类没有找到
     */
    public Class<?> compile(final String className, final CharSequence context) throws ClassNotFoundException {
        // compilation units
        JavaFileObject fileObject = new CharSequenceJavaFileObject(className, context);
        Iterable<? extends JavaFileObject> fileObjs = Collections.singletonList(fileObject);
        // compiler options
        ClassLoader classLoader = JdkCompiler.class.getClassLoader();
        String path = classLoader.getResource("").getPath();
        List<String> options = new ArrayList<>();
        options.add("-d");
        options.add(path);
        // compile the dynamic class
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return null;
        }
        StandardJavaFileManager fileMgr = compiler.getStandardFileManager(null, null, null);
        CompilationTask task = compiler.getTask(null, fileMgr, null, options, null, fileObjs);
        if (!task.call()) {
            throw new RuntimeException("Error occurs while compiling.");
        }
        return classLoader.loadClass(className);
    }

    /**
     * 源文件对象
     */
    static final class CharSequenceJavaFileObject extends SimpleJavaFileObject {
        final CharSequence content;

        public CharSequenceJavaFileObject(final String className, final CharSequence content) {
            super(URI.create("string:///" + className.replace('.', '/') + SOURCE.extension), SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }
    }
}
