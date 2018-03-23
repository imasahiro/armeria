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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.thrift.async.AsyncMethodCallback;
import org.junit.Test;

public class AsyncMethodCallbacksTest {
    @Test
    public void toBiFunction_success() {
        InvocationCountableAsyncMethodCallback<String> callback =
                new InvocationCountableAsyncMethodCallback<String>() {
                    @Override
                    public void onComplete(String response) {
                        super.onComplete(response);
                        assertThat(response).isEqualTo("hello");
                    }
                };

        CompletableFuture.completedFuture("hello")
                         .handle(AsyncMethodCallbacks.toBiFunction(callback))
                         .join();
        assertThat(callback.completed.get()).isOne();
        assertThat(callback.error.get()).isZero();
    }

    @Test
    public void toBiFunction_error() {
        InvocationCountableAsyncMethodCallback<String> callback =
                new InvocationCountableAsyncMethodCallback<String>() {
                    @Override
                    public void onError(Exception exception) {
                        super.onError(exception);
                        assertThat(exception.getMessage()).isEqualTo("error");
                    }
                };

        ThriftFutures.<String>failedCompletedFuture(new RuntimeException("error"))
                .handle(AsyncMethodCallbacks.toBiFunction(callback))
                .join();
        assertThat(callback.completed.get()).isZero();
        assertThat(callback.error.get()).isOne();
    }

    @Test
    public void toBiConsumer_success() {
        InvocationCountableAsyncMethodCallback<String> callback =
                new InvocationCountableAsyncMethodCallback<String>() {
                    @Override
                    public void onComplete(String response) {
                        super.onComplete(response);
                        assertThat(response).isEqualTo("hello");
                    }
                };

        CompletableFuture.completedFuture("hello")
                         .whenComplete(AsyncMethodCallbacks.toBiConsumer(callback))
                         .join();
        assertThat(callback.completed.get()).isOne();
        assertThat(callback.error.get()).isZero();
    }

    @Test
    public void toBiConsumer_error() {
        InvocationCountableAsyncMethodCallback<String> callback =
                new InvocationCountableAsyncMethodCallback<String>() {
                    @Override
                    public void onError(Exception exception) {
                        super.onError(exception);
                        assertThat(exception.getMessage()).isEqualTo("error");
                    }
                };

        ThriftFutures.<String>failedCompletedFuture(new RuntimeException("error"))
                .whenComplete(AsyncMethodCallbacks.toBiConsumer(callback))
                .join();
        assertThat(callback.completed.get()).isZero();
        assertThat(callback.error.get()).isOne();
    }

    private static class InvocationCountableAsyncMethodCallback<T> implements AsyncMethodCallback<T> {
        private final AtomicInteger completed = new AtomicInteger();
        private final AtomicInteger error = new AtomicInteger();

        @Override
        public void onComplete(T response) {
            completed.incrementAndGet();
        }

        @Override
        public void onError(Exception exception) {
            error.incrementAndGet();
        }
    }
}
