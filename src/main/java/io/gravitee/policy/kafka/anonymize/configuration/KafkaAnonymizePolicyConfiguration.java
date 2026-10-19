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
package io.gravitee.policy.kafka.anonymize.configuration;

import io.gravitee.policy.api.PolicyConfiguration;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaAnonymizePolicyConfiguration implements PolicyConfiguration {

    /**
     * Gravitee EL expression evaluated against each record, e.g. {@code {#message.headersString['pii'] == 'true'}}.
     * Fields are anonymized only when it evaluates to {@code true}. A blank condition always matches.
     */
    private String condition;

    /**
     * JSONPath expressions selecting the fields to anonymize, e.g. {@code $.customer.email}.
     */
    private List<String> fields;
}
