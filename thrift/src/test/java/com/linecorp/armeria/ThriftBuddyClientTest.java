/*
 * Copyright 2017 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.armeria;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.linecorp.armeria.client.ClientFactory;
import com.linecorp.armeria.client.thrift.THttpClientFactory;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.thrift.THttpService;
import com.linecorp.armeria.service.test.thrift.main.HelloService;
import com.linecorp.armeria.service.test.thrift.main.HelloService.AsyncIface;
import com.linecorp.armeria.testing.server.ServerRule;

public class ThriftBuddyClientTest {
    @ClassRule
    public static final ServerRule server = new ServerRule() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
            sb.service("/hello", THttpService.of(
                    (AsyncIface) (name, resultHandler) -> resultHandler.onComplete(name)));
        }
    };

    private HelloService.Iface buddyClient;

    @Before
    public void startServer() {
        server.start();
        buddyClient = new THttpClientFactory(ClientFactory.DEFAULT).newClient(
                "tbinary+http://127.0.0.1:" + server.httpPort() + "/hello", HelloService.Iface.class);
    }

    @After
    public void stopServer() throws Exception {
        server.stop().join();
    }

    @Test
    public void buddy() throws Exception {
        assertThat(buddyClient.hello("hello")).isEqualTo("hello");
    }
}
