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

import io.gravitee.gateway.reactive.api.message.kafka.KafkaMessage;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * What the gateway exposes as {@code #message} in EL expressions on a native Kafka API:
 * {@code #message.content}, {@code #message.key}, {@code #message.topic}, {@code #message.headersString['name']}...
 */
public class EvaluableKafkaMessage {

    private final KafkaMessage message;

    public EvaluableKafkaMessage(KafkaMessage message) {
        this.message = message;
    }

    public String getContent() {
        return message.content() == null ? null : message.content().toString();
    }

    public String getKey() {
        return message.key() == null ? null : message.key().toString();
    }

    public String getTopic() {
        return message.topic();
    }

    public Map<String, String> getHeadersString() {
        return message.recordHeaders().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
    }

    public Map<String, Object> getMetadata() {
        return message.metadata();
    }
}
