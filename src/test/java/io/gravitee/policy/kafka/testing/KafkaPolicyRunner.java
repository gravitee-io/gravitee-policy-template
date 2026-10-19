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
package io.gravitee.policy.kafka.testing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.reactive.api.context.kafka.KafkaExecutionContext;
import io.gravitee.gateway.reactive.api.context.kafka.KafkaMessageExecutionContext;
import io.gravitee.gateway.reactive.api.context.kafka.KafkaMessageRequest;
import io.gravitee.gateway.reactive.api.context.kafka.KafkaMessageResponse;
import io.gravitee.gateway.reactive.api.message.kafka.KafkaMessage;
import io.gravitee.gateway.reactive.api.policy.kafka.KafkaPolicy;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Maybe;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.apache.kafka.common.protocol.Errors;

/**
 * Runs a {@link KafkaPolicy} over a list of records, the way the gateway does, without starting a gateway or a broker.
 *
 * <pre>{@code
 * List<KafkaMessage> out = KafkaPolicyRunner.produce(policy, new TestKafkaMessage("orders", "{...}"));
 * }</pre>
 *
 * <p>EL expressions are evaluated by the real Gravitee expression language, with the record exposed as {@code #message}.
 * When a policy calls {@code ctx.executionContext().interruptWith(errors)}, the run fails with a
 * {@link KafkaInterruptionException} carrying those errors.
 */
public final class KafkaPolicyRunner {

    private KafkaPolicyRunner() {}

    /** Records produced by a client, going through {@link KafkaPolicy#onMessageRequest}. */
    public static List<KafkaMessage> produce(KafkaPolicy policy, KafkaMessage... messages) {
        return run(policy, true, messages);
    }

    /** Records fetched by a consumer, going through {@link KafkaPolicy#onMessageResponse}. */
    public static List<KafkaMessage> consume(KafkaPolicy policy, KafkaMessage... messages) {
        return run(policy, false, messages);
    }

    private static List<KafkaMessage> run(KafkaPolicy policy, boolean produce, KafkaMessage... messages) {
        InMemoryMessageFlow flow = new InMemoryMessageFlow(Flowable.fromArray(messages));

        KafkaExecutionContext executionContext = mock(KafkaExecutionContext.class);
        when(executionContext.interruptWith(any(Errors.class)))
            .thenAnswer(inv -> Completable.error(new KafkaInterruptionException(inv.getArgument(0))));

        KafkaMessageExecutionContext ctx = mock(KafkaMessageExecutionContext.class);
        when(ctx.executionContext()).thenReturn(executionContext);
        when(ctx.request()).thenReturn(produce ? flow : null);
        when(ctx.response()).thenReturn(produce ? null : flow);
        when(ctx.getTemplateEngine(any()))
            .thenAnswer(inv -> {
                TemplateEngine engine = TemplateEngine.templateEngine();
                engine.getTemplateContext().setVariable("message", new EvaluableKafkaMessage(inv.getArgument(0)));
                return engine;
            });

        Completable phase = produce ? policy.onMessageRequest(ctx) : policy.onMessageResponse(ctx);
        // defer: the record stream must be read only once the policy has plugged its transformation into it
        return phase.andThen(Flowable.defer(flow::messages)).toList().timeout(5, TimeUnit.SECONDS).blockingGet();
    }

    /** The policy chain swaps the record stream; the gateway then forwards whatever comes out of it. */
    private static final class InMemoryMessageFlow implements KafkaMessageRequest, KafkaMessageResponse {

        private Flowable<KafkaMessage> messages;
        private String compression;

        private InMemoryMessageFlow(Flowable<KafkaMessage> messages) {
            this.messages = messages;
        }

        @Override
        public Flowable<KafkaMessage> messages() {
            return messages;
        }

        @Override
        public Completable onMessage(Function<KafkaMessage, Maybe<KafkaMessage>> onMessage) {
            return KafkaMessageRequest.super.onMessage(onMessage);
        }

        @Override
        public void messages(Flowable<KafkaMessage> messages) {
            this.messages = messages;
        }

        @Override
        public Completable onMessages(FlowableTransformer<KafkaMessage, KafkaMessage> onMessages) {
            return Completable.fromRunnable(() -> messages = messages.compose(onMessages));
        }

        @Override
        public String compression() {
            return compression;
        }

        @Override
        public void compression(String compressionType) {
            this.compression = compressionType;
        }
    }

    /** Raised when a policy interrupts the Kafka request, e.g. with {@code Errors.INVALID_RECORD}. */
    public static final class KafkaInterruptionException extends RuntimeException {

        private final Errors errors;

        public KafkaInterruptionException(Errors errors) {
            super("Kafka request interrupted with " + errors);
            this.errors = errors;
        }

        public Errors errors() {
            return errors;
        }
    }
}
