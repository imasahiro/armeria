/*
 * Copyright 2016 LINE Corporation
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

package com.linecorp.armeria.client.thrift;

import static com.google.common.base.MoreObjects.firstNonNull;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.not;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default;
import net.bytebuddy.implementation.MethodDelegation;

import com.google.common.collect.ImmutableSet;

import com.linecorp.armeria.client.Client;
import com.linecorp.armeria.client.ClientFactory;
import com.linecorp.armeria.client.ClientOptions;
import com.linecorp.armeria.client.DecoratingClientFactory;
import com.linecorp.armeria.client.DefaultClientBuilderParams;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.RpcRequest;
import com.linecorp.armeria.common.RpcResponse;
import com.linecorp.armeria.common.Scheme;
import com.linecorp.armeria.common.SerializationFormat;
import com.linecorp.armeria.common.SessionProtocol;
import com.linecorp.armeria.common.thrift.ThriftSerializationFormats;

/**
 * A {@link DecoratingClientFactory} that creates a Thrift-over-HTTP client.
 */
public class THttpClientFactory extends DecoratingClientFactory {

    private static final Set<Scheme> SUPPORTED_SCHEMES;

    static {
        final ImmutableSet.Builder<Scheme> builder = ImmutableSet.builder();
        for (SessionProtocol p : SessionProtocol.values()) {
            for (SerializationFormat f : ThriftSerializationFormats.values()) {
                builder.add(Scheme.of(f, p));
            }
        }
        SUPPORTED_SCHEMES = builder.build();
    }

    /**
     * Creates a new instance from the specified {@link ClientFactory} that supports the "none+http" scheme.
     *
     * @throws IllegalArgumentException if the specified {@link ClientFactory} does not support HTTP
     */
    public THttpClientFactory(ClientFactory httpClientFactory) {
        super(httpClientFactory);
    }

    @Override
    public Set<Scheme> supportedSchemes() {
        return SUPPORTED_SCHEMES;
    }

    @Override
    public <T> T newClient(URI uri, Class<T> clientType, ClientOptions options) {
        final Scheme scheme = validateScheme(uri);
        final SerializationFormat serializationFormat = scheme.serializationFormat();

        final Client<RpcRequest, RpcResponse> delegate = options.decoration().decorate(
                RpcRequest.class, RpcResponse.class,
                new THttpClientDelegate(newHttpClient(uri, scheme, options),
                                        serializationFormat));

        if (clientType == THttpClient.class) {
            // Create a THttpClient with path.
            @SuppressWarnings("unchecked")
            final T client = (T) new DefaultTHttpClient(
                    new DefaultClientBuilderParams(this, uri, THttpClient.class, options),
                    delegate, scheme.sessionProtocol(), newEndpoint(uri));
            return client;
        } else {
            // Create a THttpClient without path.
            final THttpClient thriftClient = new DefaultTHttpClient(
                    new DefaultClientBuilderParams(this, pathlessUri(uri), THttpClient.class, options),
                    delegate, scheme.sessionProtocol(), newEndpoint(uri));

            final THttpClientInvocationHandler handler = new THttpClientInvocationHandler(
                    new DefaultClientBuilderParams(this, uri, clientType, options),
                    thriftClient,
                    firstNonNull(uri.getRawPath(), "/"),
                    uri.getFragment());
            try {
                return new ByteBuddy().subclass(clientType)
                                      .method(isDeclaredBy(clientType))
                                      .intercept(MethodDelegation.withDefaultConfiguration()
                                                                 .filter(not(isDeclaredBy(Object.class)))
                                                                 .to(handler, "handler"))
                                      .make()
                                      .load(clientType.getClassLoader(), Default.INJECTION)
                                      .getLoaded()
                                      .newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException("Could not generate client proxy.", e);
            }
        }
    }

    private Client<HttpRequest, HttpResponse> newHttpClient(URI uri, Scheme scheme, ClientOptions options) {
        try {
            @SuppressWarnings("unchecked")
            Client<HttpRequest, HttpResponse> client = delegate().newClient(
                    new URI(Scheme.of(SerializationFormat.NONE, scheme.sessionProtocol()).uriText(),
                            uri.getRawAuthority(), null, null, null),
                    Client.class, options);
            return client;
        } catch (URISyntaxException e) {
            throw new Error(e); // Should never happen.
        }
    }

    private static URI pathlessUri(URI uri) {
        try {
            return new URI(uri.getScheme(), uri.getRawAuthority(), null, null, null);
        } catch (URISyntaxException e) {
            throw new Error(e); // Should never happen.
        }
    }
}
