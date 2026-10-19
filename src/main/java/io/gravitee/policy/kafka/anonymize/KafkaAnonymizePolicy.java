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
package io.gravitee.policy.kafka.anonymize;

import io.gravitee.gateway.reactive.api.context.kafka.KafkaMessageExecutionContext;
import io.gravitee.gateway.reactive.api.policy.kafka.KafkaPolicy;
import io.gravitee.policy.kafka.anonymize.configuration.KafkaAnonymizePolicyConfiguration;
import io.reactivex.rxjava3.core.Completable;

/**
 * Anonymizes fields of JSON Kafka records: every field selected by the configuration gets the value {@value #MASK}.
 *
 * <p>See the README for the full description of the exercise.
 */
public class KafkaAnonymizePolicy implements KafkaPolicy {

    public static final String MASK = "****";

    private final KafkaAnonymizePolicyConfiguration configuration;

    public KafkaAnonymizePolicy(KafkaAnonymizePolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String id() {
        return "kafka-anonymize";
    }

    @Override
    public Completable onMessageRequest(KafkaMessageExecutionContext ctx) {
        // TODO: anonymize produced records
        return Completable.complete();
    }

    @Override
    public Completable onMessageResponse(KafkaMessageExecutionContext ctx) {
        // TODO: anonymize consumed records
        return Completable.complete();
    }
}
