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
package io.gravitee.policy.template;/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.policy.template.configuration.TemplatePolicyConfiguration;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class TemplatePolicy implements Policy {

    public static final String TEMPLATE_POLICY_HEADER = "x-template-policy";
    public static final String TEMPLATE_POLICY_EXECUTED_HEADER = "x-template-policy-executed";
    public static final String ERROR_MESSAGE = "Invalid header";

    private final TemplatePolicyConfiguration configuration;

    @Override
    public String id() {
        return "template-policy";
    }

    @Override
    public Completable onRequest(HttpExecutionContext ctx) {
        return Completable.defer(() -> {
            if (shouldInterrupt(ctx.request().headers())) {
                return ctx.interruptWith(new ExecutionFailure(HttpStatusCode.BAD_REQUEST_400).message(ERROR_MESSAGE));
            }
            ctx.request().headers().add(TEMPLATE_POLICY_EXECUTED_HEADER, "ok");
            return Completable.complete();
        });
    }

    @Override
    public Completable onResponse(HttpExecutionContext ctx) {
        if (shouldInterrupt(ctx.response().headers())) {
            return ctx.interruptWith(new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500).message(ERROR_MESSAGE));
        }
        ctx.response().headers().add(TEMPLATE_POLICY_EXECUTED_HEADER, "ok");
        return Policy.super.onRequest(ctx);
    }

    private boolean shouldInterrupt(HttpHeaders headers) {
        return headers.getAll(TEMPLATE_POLICY_HEADER).stream().anyMatch(header -> header.equalsIgnoreCase(configuration.getErrorKey()));
    }
}
