/*
 * Copyright 2017 LINE Corporation
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

package com.linecorp.armeria.server.thrift;

import static com.linecorp.armeria.common.SessionProtocol.HTTP;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import com.linecorp.armeria.benchmarks.Empty;
import com.linecorp.armeria.benchmarks.GithubService;
import com.linecorp.armeria.benchmarks.SearchResponse;
import com.linecorp.armeria.client.Clients;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServerPort;

@State(Scope.Benchmark)
public class ThriftServiceBenchmark {
    private static final Empty EMPTY = new Empty();
    private static final SearchResponse SEARCH_RESPONSE = new SearchResponse();

    private Server server;
    private GithubService.Iface client;

    private static final class GithubThriftService implements GithubService.AsyncIface {
        @Override
        public void simple(SearchResponse req,
                           AsyncMethodCallback<SearchResponse> resultHandler) throws TException {
            resultHandler.onComplete(SEARCH_RESPONSE);
        }

        @Override
        public void empty(Empty req, AsyncMethodCallback<Empty> resultHandler) throws TException {
            resultHandler.onComplete(EMPTY);
        }
    }

    @Setup
    public void startServer() throws Exception {
        Server server = new ServerBuilder()
                .port(0, HTTP)
                .service("/thrift", THttpService.of(new GithubThriftService()))
                .build();
        server.start().join();

        ServerPort httpPort = server.activePorts().values().stream()
                                    .filter(p1 -> p1.protocol() == HTTP).findAny()
                                    .get();
        client = Clients.newClient(
                "tbinary+http://127.0.0.1:" + httpPort.localAddress().getPort() + "/thrift",
                GithubService.Iface.class);
    }

    @TearDown
    public void stopServer() throws Exception {
        server.stop().join();
    }

    @Benchmark
    public void empty(Blackhole bh) throws Exception {
        bh.consume(client.empty(EMPTY));
    }

    @Benchmark
    public void simple(Blackhole bh) throws Exception {
        bh.consume(client.simple(SEARCH_RESPONSE));
    }
}
