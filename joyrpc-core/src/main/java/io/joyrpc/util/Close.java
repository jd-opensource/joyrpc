/**
 *
 */
package io.joyrpc.util;

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

import javax.sql.DataSource;
import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class Close {

    private static Close instance = new Close();

    /**
     * 关闭资源，不抛出IO异常
     *
     * @param io IO对象
     */
    public static Close close(final AutoCloseable io) {
        if (io != null) {
            try {
                io.close();
            } catch (Exception ignored) {
            }
        }
        return instance;
    }

    /**
     * 关闭资源，不抛出异常
     *
     * @param resources 资源对象
     */
    public static Close close(final AutoCloseable... resources) {
        if (resources != null) {
            for (AutoCloseable resource : resources) {
                if (resource != null) {
                    try {
                        resource.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return instance;
    }

    /**
     * 关闭连接
     *
     * @param connection 连接
     * @param statement  声明
     * @param resultSet  结果集合
     */
    public static Close close(final Connection connection, final Statement statement, final ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException ignored) {
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ignored) {
            }
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
        return instance;
    }

    /**
     * 关闭时钟
     *
     * @param timer 时钟
     * @return
     */
    public static Close close(final Timer timer) {
        if (timer != null) {
            try {
                timer.cancel();
            } catch (Throwable e) {
            }
        }
        return instance;
    }

    /**
     * 关闭文件锁
     *
     * @param thread 文件锁
     * @return
     */
    public static Close close(final Thread thread) {
        if (thread != null) {
            thread.interrupt();
        }
        return instance;
    }

    /**
     * 关闭连接池
     *
     * @param dataSource 连接池
     */
    public static Close close(final DataSource dataSource) {
        if (dataSource == null) {
            return instance;
        }
        if (dataSource instanceof Closeable) {
            return close((Closeable) dataSource);
        }
        // 兼容DBCP,反射调用close方法
        Class<?> clazz = dataSource.getClass();
        try {
            Method method = clazz.getDeclaredMethod("close");
            method.invoke(dataSource);
        } catch (NoSuchMethodException ignored) {
        } catch (InvocationTargetException ignored) {
        } catch (IllegalAccessException ignored) {
        }
        return instance;
    }

    /**
     * 立即关闭线程池
     *
     * @param executor 线程池
     */
    public static Close close(final ExecutorService executor) {
        if (executor != null) {
            executor.shutdownNow();
        }
        return instance;
    }

    /**
     * 关闭线程池
     *
     * @param executor 线程池
     * @param timeout  超时时间(毫秒)
     */
    public static Close close(final ExecutorService executor, final long timeout) {
        if (executor != null) {
            if (timeout <= 0) {
                executor.shutdownNow();
            } else {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException ignored) {
                }
            }
        }
        return instance;
    }

}
