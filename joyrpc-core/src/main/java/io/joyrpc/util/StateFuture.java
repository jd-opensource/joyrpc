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

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 状态Future管理
 *
 * @param <T>
 */
public class StateFuture<T> {
    /**
     * 打开的结果
     */
    protected volatile CompletableFuture<T> openFuture;
    /**
     * 关闭Future
     */
    protected volatile CompletableFuture<T> closeFuture;
    /**
     * 关闭前
     */
    protected volatile CompletableFuture<Void> beforeOpenFuture;
    /**
     * 关闭前
     */
    protected volatile CompletableFuture<Void> beforeCloseFuture;
    /**
     * 关闭后
     */
    protected volatile CompletableFuture<Void> afterCloseFuture;
    /**
     * 打开前准备工作提供者
     */
    protected Supplier<CompletableFuture<Void>> beforeOpenSupplier;
    /**
     * 等待关闭提供者
     */
    protected Supplier<CompletableFuture<Void>> beforeCloseSupplier;
    /**
     * 关闭后提供者
     */
    protected Supplier<CompletableFuture<Void>> afterCloseSupplier;

    public StateFuture() {
        this(null, null, null, null, null);
    }

    public StateFuture(final Supplier<CompletableFuture<Void>> beforeOpenSupplier,
                       final Supplier<CompletableFuture<Void>> beforeCloseSupplier) {
        this(null, null, beforeOpenSupplier, beforeCloseSupplier, null);
    }

    public StateFuture(CompletableFuture<T> openFuture, CompletableFuture<T> closeFuture) {
        this(openFuture, closeFuture, null, null, null);
    }

    public StateFuture(final CompletableFuture<T> openFuture,
                       final CompletableFuture<T> closeFuture,
                       final Supplier<CompletableFuture<Void>> beforeOpenSupplier,
                       final Supplier<CompletableFuture<Void>> beforeCloseSupplier,
                       final Supplier<CompletableFuture<Void>> afterCloseSupplier) {
        this.openFuture = openFuture == null ? new CompletableFuture<>() : openFuture;
        this.closeFuture = closeFuture == null ? new CompletableFuture<>() : closeFuture;
        this.beforeOpenSupplier = beforeOpenSupplier;
        this.beforeCloseSupplier = beforeCloseSupplier;
        this.afterCloseSupplier = afterCloseSupplier;
    }

    public CompletableFuture<Void> getBeforeOpenFuture() {
        return beforeOpenFuture;
    }

    public CompletableFuture<T> getOpenFuture() {
        return openFuture;
    }

    public CompletableFuture<T> getCloseFuture() {
        return closeFuture;
    }

    public CompletableFuture<Void> getBeforeCloseFuture() {
        return beforeCloseFuture;
    }

    /**
     * 创建打开Future
     *
     * @return 新建的打开Future
     */
    public CompletableFuture<T> newOpenFuture() {
        CompletableFuture<T> result = new CompletableFuture<>();
        openFuture = result;
        beforeCloseFuture = null;
        afterCloseFuture = null;
        return result;
    }

    /**
     * 创建关闭Future
     *
     * @return 新建的关闭Future
     */
    public CompletableFuture<T> newCloseFuture() {
        CompletableFuture<T> result = new CompletableFuture<>();
        closeFuture = result;
        return result;
    }

    public CompletableFuture<Void> newBeforeOpenFuture() {
        CompletableFuture<Void> result = beforeOpenSupplier == null ? null : beforeOpenSupplier.get();
        result = result == null ? CompletableFuture.completedFuture(null) : result;
        beforeOpenFuture = result;
        return result;
    }

    public CompletableFuture<Void> newBeforeCloseFuture(final boolean gracefully) {
        CompletableFuture<Void> result = !gracefully || beforeCloseSupplier == null ? null : beforeCloseSupplier.get();
        result = result == null ? CompletableFuture.completedFuture(null) : result;
        beforeCloseFuture = result;
        return result;
    }

    public CompletableFuture<Void> newAfterCloseFuture(final boolean gracefully) {
        CompletableFuture<Void> result = !gracefully || afterCloseSupplier == null ? null : afterCloseSupplier.get();
        result = result == null ? CompletableFuture.completedFuture(null) : result;
        afterCloseFuture = result;
        return result;
    }

    /**
     * 关闭
     */

    public void close() {
        IllegalStateException ex = new IllegalStateException("state is illegal.");
        doClose(ex);
    }

    protected void doClose(final IllegalStateException ex) {
        Futures.completeExceptionally(beforeOpenFuture, ex);
        Futures.completeExceptionally(openFuture, ex);
        Futures.completeExceptionally(beforeCloseFuture, ex);
        Futures.completeExceptionally(closeFuture, ex);
        Futures.completeExceptionally(afterCloseFuture, ex);
    }

    /**
     * 增加状态Future管理
     *
     * @param <T>
     */
    public static class ExStateFuture<T> extends StateFuture<T> {

        /**
         * 打开的结果
         */
        protected volatile CompletableFuture<T> exportFuture;

        /**
         * 暴露服务Future
         */
        protected volatile CompletableFuture<Void> beforeExportFuture;
        /**
         * 暴露服务提供者
         */
        protected Supplier<CompletableFuture<Void>> beforeExportSupplier;

        public ExStateFuture(final Supplier<CompletableFuture<Void>> beforeExportSupplier,
                             final Supplier<CompletableFuture<Void>> beforeOpenSupplier,
                             final Supplier<CompletableFuture<Void>> beforeCloseSupplier,
                             final Supplier<CompletableFuture<Void>> afterCloseSupplier) {
            super(null, null, beforeOpenSupplier, beforeCloseSupplier, afterCloseSupplier);
            this.beforeExportSupplier = beforeExportSupplier;
        }

        public CompletableFuture<T> getExportFuture() {
            return exportFuture;
        }

        /**
         * 创建打开Future
         *
         * @return 新建的打开Future
         */
        public CompletableFuture<T> newExportFuture() {
            CompletableFuture<T> result = new CompletableFuture<>();
            exportFuture = result;
            beforeExportFuture = null;
            beforeOpenFuture = null;
            openFuture = null;
            beforeCloseFuture = null;
            afterCloseFuture = null;
            return result;
        }

        public CompletableFuture<Void> newBeforeExportFuture() {
            CompletableFuture<Void> result = beforeExportSupplier == null ? CompletableFuture.completedFuture(null) : beforeExportSupplier.get();
            beforeExportFuture = result;
            return result;
        }

        @Override
        protected void doClose(final IllegalStateException ex) {
            Futures.completeExceptionally(beforeExportFuture, ex);
            Futures.completeExceptionally(exportFuture, ex);
            super.doClose(ex);
        }
    }
}
