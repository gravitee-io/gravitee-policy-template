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
package io.gravitee.policy.kafka.addheader;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.reactive.api.context.kafka.KafkaMessageExecutionContext;
import io.gravitee.gateway.reactive.api.message.kafka.KafkaMessage;
import io.gravitee.gateway.reactive.api.policy.kafka.KafkaPolicy;
import io.gravitee.policy.kafka.addheader.configuration.KafkaAddHeaderPolicyConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.Errors;

/**
 * Adds a header to every Kafka record that matches an EL condition.
 *
 * <ul>
 *   <li>{@link #onMessageRequest} runs on records <b>produced</b> by a client, before they reach the broker.</li>
 *   <li>{@link #onMessageResponse} runs on records <b>fetched</b> by a consumer, before they reach the client.</li>
 * </ul>
 */
@Slf4j
public class KafkaAddHeaderPolicy implements KafkaPolicy {

    private final KafkaAddHeaderPolicyConfiguration configuration;

    public KafkaAddHeaderPolicy(KafkaAddHeaderPolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String id() {
        return "kafka-add-header";
    }

    @Override
    public Completable onMessageRequest(KafkaMessageExecutionContext ctx) {
        return ctx.request().onMessage(message -> addHeaderIfMatches(ctx, message));
    }

    @Override
    public Completable onMessageResponse(KafkaMessageExecutionContext ctx) {
        return ctx.response().onMessage(message -> addHeaderIfMatches(ctx, message));
    }

    private Maybe<KafkaMessage> addHeaderIfMatches(KafkaMessageExecutionContext ctx, KafkaMessage message) {
        return matches(ctx, message)
            .map(matches -> {
                if (matches) {
                    message.putRecordHeader(configuration.getHeaderName(), Buffer.buffer(configuration.getHeaderValue()));
                }
                return message;
            })
            .toMaybe()
            // An invalid expression must not let the record through silently: reject the whole Kafka request.
            .onErrorResumeNext(error -> {
                log.warn("Unable to evaluate condition [{}]", configuration.getCondition(), error);
                return ctx.executionContext().interruptWith(Errors.INVALID_REQUEST).toMaybe();
            });
    }

    private Single<Boolean> matches(KafkaMessageExecutionContext ctx, KafkaMessage message) {
        String condition = configuration.getCondition();
        if (condition == null || condition.isBlank()) {
            return Single.just(true);
        }
        // The template engine exposes the record as #message (content, key, topic, headersString...).
        // eval() is asynchronous: never call a blocking variant on the gateway event loop.
        return ctx.getTemplateEngine(message).eval(condition, Boolean.class).defaultIfEmpty(false);
    }
}
