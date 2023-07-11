/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.template;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.gravitee.policy.template.TemplatePolicy.ERROR_MESSAGE;
import static io.gravitee.policy.template.TemplatePolicy.TEMPLATE_POLICY_EXECUTED_HEADER;
import static io.gravitee.policy.template.TemplatePolicy.TEMPLATE_POLICY_HEADER;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractPolicyTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.policy.template.configuration.TemplatePolicyConfiguration;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi({ "/apis/api.json", "/apis/api-response.json" })
class TemplatePolicyIntegrationTest extends AbstractPolicyTest<TemplatePolicy, TemplatePolicyConfiguration> {

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }

    @Test
    void should_call_api_with_policy_on_request(HttpClient httpClient) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("backend response")));

        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(HttpClientRequest::rxSend)
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(HttpStatusCode.OK_200);
                return response.body();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(Buffer.buffer("backend response"))
            .assertNoErrors();

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")).withHeader(TEMPLATE_POLICY_EXECUTED_HEADER, equalTo("ok")));
    }

    @Test
    void should_call_api_and_fail_with_policy_on_request(HttpClient httpClient) {
        wiremock.stubFor(get("/endpoint").willReturn(ok()));

        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> request.putHeader(TEMPLATE_POLICY_HEADER, "failure").rxSend())
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                return response.body();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(body -> {
                assertThat(body).hasToString(ERROR_MESSAGE);
                return true;
            })
            .assertNoErrors();

        wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
    }

    @Test
    void should_call_api_with_policy_on_response(HttpClient httpClient) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("backend response")));

        httpClient
            .rxRequest(HttpMethod.GET, "/test-response")
            .flatMap(HttpClientRequest::rxSend)
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(HttpStatusCode.OK_200);
                assertThat(response.getHeader(TEMPLATE_POLICY_EXECUTED_HEADER)).isEqualTo("ok");
                return response.body();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(Buffer.buffer("backend response"))
            .assertNoErrors();

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
    }

    @Test
    void should_call_api_and_fail_with_policy_on_response(HttpClient httpClient) {
        wiremock.stubFor(get("/endpoint").willReturn(ok().withHeader(TEMPLATE_POLICY_HEADER, "failure")));

        httpClient
            .rxRequest(HttpMethod.GET, "/test-response")
            .flatMap(HttpClientRequest::rxSend)
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
                return response.body();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(body -> {
                assertThat(body).hasToString(ERROR_MESSAGE);
                return true;
            })
            .assertNoErrors();

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
    }
}
