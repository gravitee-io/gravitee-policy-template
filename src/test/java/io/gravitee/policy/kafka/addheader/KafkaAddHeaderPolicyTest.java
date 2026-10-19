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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gateway.reactive.api.message.kafka.KafkaMessage;
import io.gravitee.policy.kafka.addheader.configuration.KafkaAddHeaderPolicyConfiguration;
import io.gravitee.policy.kafka.testing.KafkaPolicyRunner;
import io.gravitee.policy.kafka.testing.KafkaPolicyRunner.KafkaInterruptionException;
import io.gravitee.policy.kafka.testing.TestKafkaMessage;
import java.util.List;
import org.apache.kafka.common.protocol.Errors;
import org.junit.jupiter.api.Test;

class KafkaAddHeaderPolicyTest {

    private static KafkaAddHeaderPolicy policy(String condition) {
        return new KafkaAddHeaderPolicy(
            KafkaAddHeaderPolicyConfiguration.builder().condition(condition).headerName("X-Flagged").headerValue("true").build()
        );
    }

    @Test
    void adds_header_to_produced_records_matching_the_condition() {
        List<KafkaMessage> records = KafkaPolicyRunner.produce(
            policy("{#jsonPath(#message.content, '$.amount') > 1000}"),
            new TestKafkaMessage("payments", "{\"id\":1,\"amount\":5000}"),
            new TestKafkaMessage("payments", "{\"id\":2,\"amount\":10}")
        );

        assertThat(records).hasSize(2);
        assertThat(records.get(0).recordHeaders()).containsKey("X-Flagged");
        assertThat(records.get(0).recordHeaders().get("X-Flagged")).hasToString("true");
        assertThat(records.get(1).recordHeaders()).doesNotContainKey("X-Flagged");
    }

    @Test
    void adds_header_to_consumed_records_based_on_an_existing_header() {
        List<KafkaMessage> records = KafkaPolicyRunner.consume(
            policy("{#message.headersString['source'] == 'legacy'}"),
            new TestKafkaMessage("orders", "{}").withHeader("source", "legacy"),
            new TestKafkaMessage("orders", "{}").withHeader("source", "web")
        );

        assertThat(records.get(0).recordHeaders()).containsKey("X-Flagged");
        assertThat(records.get(1).recordHeaders()).doesNotContainKey("X-Flagged");
    }

    @Test
    void adds_header_to_every_record_when_no_condition_is_set() {
        List<KafkaMessage> records = KafkaPolicyRunner.produce(policy(null), new TestKafkaMessage("orders", "{}"));

        assertThat(records.get(0).recordHeaders()).containsKey("X-Flagged");
    }

    @Test
    void interrupts_the_request_when_the_condition_cannot_be_evaluated() {
        assertThatThrownBy(() -> KafkaPolicyRunner.produce(policy("{#unknownFunction()}"), new TestKafkaMessage("orders", "{}")))
            .isInstanceOf(KafkaInterruptionException.class)
            .extracting(e -> ((KafkaInterruptionException) e).errors())
            .isEqualTo(Errors.INVALID_REQUEST);
    }
}
