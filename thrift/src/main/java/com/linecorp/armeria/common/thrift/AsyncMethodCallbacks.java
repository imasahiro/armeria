/*
 * Copyright 2018 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.armeria.common.thrift;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.apache.thrift.async.AsyncMethodCallback;

/**
 * Utility methods related with {@link AsyncMethodCallback} to functions for delegating the result of
 * {@link CompletableFuture} to {@link AsyncMethodCallback}.
 */
public final class AsyncMethodCallbacks {
    private AsyncMethodCallbacks() {}

    private static <T> void handle(T value, Throwable thrown, AsyncMethodCallback<T> resultHandler) {
        if (thrown != null) {
            if (thrown instanceof Exception) {
                resultHandler.onError((Exception) thrown);
            } else {
                resultHandler.onError(new Exception(thrown));
            }
        } else {
            resultHandler.onComplete(value);
        }
    }

    /**
     * Converts the specified {@link AsyncMethodCallback} into a {@link BiFunction} that returns {@code null}.
     */
    public static <T> BiFunction<T, Throwable, Void> toBiFunction(AsyncMethodCallback<T> resultHandler) {
        return (value, thrown) -> {
            handle(value, thrown, resultHandler);
            return null;
        };
    }

    /**
     * Converts the specified {@link AsyncMethodCallback} into a {@link BiConsumer}.
     */
    public static <T> BiConsumer<T, Throwable> toBiConsumer(AsyncMethodCallback<T> resultHandler) {
        return (value, thrown) -> handle(value, thrown, resultHandler);
    }
}
