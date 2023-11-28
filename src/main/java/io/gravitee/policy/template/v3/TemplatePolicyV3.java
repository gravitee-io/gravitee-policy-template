/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
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
 */
package io.gravitee.policy.template.v3;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnResponse;
import io.gravitee.policy.template.configuration.TemplatePolicyConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TemplatePolicyV3 {

    public static final String TEMPLATE_POLICY_HEADER = "x-template-policy";
    public static final String TEMPLATE_POLICY_EXECUTED_HEADER = "x-template-policy-executed";
    public static final String ERROR_MESSAGE = "Invalid header";

    protected final TemplatePolicyConfiguration configuration;

    @OnRequest
    public void onRequest(ExecutionContext executionContext, Request request, PolicyChain chain) {
        if (shouldInterrupt(request.headers())) {
            chain.failWith(PolicyResult.failure(HttpStatusCode.BAD_REQUEST_400, ERROR_MESSAGE));
        }
        request.headers().add(TEMPLATE_POLICY_EXECUTED_HEADER, "ok");
        chain.doNext(request, executionContext.response());
    }

    @OnResponse
    public void onResponse(ExecutionContext executionContext, Response response, PolicyChain chain) {
        if (shouldInterrupt(response.headers())) {
            chain.failWith(PolicyResult.failure(HttpStatusCode.INTERNAL_SERVER_ERROR_500, ERROR_MESSAGE));
        }
        response.headers().add(TEMPLATE_POLICY_EXECUTED_HEADER, "ok");
        chain.doNext(executionContext.request(), response);
    }

    protected boolean shouldInterrupt(HttpHeaders headers) {
        return headers.getAll(TEMPLATE_POLICY_HEADER).stream().anyMatch(header -> header.equalsIgnoreCase(configuration.getErrorKey()));
    }
}
