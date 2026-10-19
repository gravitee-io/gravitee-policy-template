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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.reactive.api.message.kafka.KafkaMessage;
import io.gravitee.policy.kafka.anonymize.configuration.KafkaAnonymizePolicyConfiguration;
import io.gravitee.policy.kafka.testing.KafkaPolicyRunner;
import io.gravitee.policy.kafka.testing.TestKafkaMessage;
import java.util.List;
import org.junit.jupiter.api.Test;

class KafkaAnonymizePolicyTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void anonymizes_selected_fields_of_an_inline_json_record_when_the_condition_matches() throws Exception {
        KafkaAnonymizePolicy policy = new KafkaAnonymizePolicy(
            KafkaAnonymizePolicyConfiguration.builder()
                .condition("{#message.headersString['pii'] == 'true'}")
                .fields(List.of("$.customer.email", "$.customer.address.street", "$.card.number"))
                .build()
        );

        String record =
            "{\"orderId\":\"A-1001\",\"customer\":{\"name\":\"Jane Doe\",\"email\":\"jane@example.com\",\"address\":{\"street\":\"1 Main St\",\"city\":\"Denver\"}},\"card\":{\"number\":\"4111111111111111\",\"expiry\":\"12/29\"},\"amount\":42.5}";

        List<KafkaMessage> out = KafkaPolicyRunner.produce(
            policy,
            new TestKafkaMessage("orders", record).withHeader("pii", "true"),
            new TestKafkaMessage("orders", record).withHeader("pii", "false")
        );

        assertThat(out).hasSize(2);
        assertThat(json(out.get(0))).isEqualTo(
            MAPPER.readTree(
                "{\"orderId\":\"A-1001\",\"customer\":{\"name\":\"Jane Doe\",\"email\":\"****\",\"address\":{\"street\":\"****\",\"city\":\"Denver\"}},\"card\":{\"number\":\"****\",\"expiry\":\"12/29\"},\"amount\":42.5}"
            )
        );
        // condition not met: the record goes through untouched
        assertThat(out.get(1).content()).hasToString(record);
    }

    @Test
    void anonymizes_selected_fields_of_a_pretty_printed_json_record_when_the_condition_matches() throws Exception {
        KafkaAnonymizePolicy policy = new KafkaAnonymizePolicy(
            KafkaAnonymizePolicyConfiguration.builder()
                .condition("{#jsonPath(#message.content, '$.patient.country') == 'US'}")
                .fields(List.of("$.patient.ssn", "$.patient.contacts[*].phone", "$.doctor.license"))
                .build()
        );

        String usRecord = """
            {
              "visitId": "V-2001",
              "patient": {
                "name": "John Smith",
                "ssn": "123-45-6789",
                "country": "US",
                "contacts": [
                  { "type": "home", "phone": "+1 303 555 0100" },
                  { "type": "work", "phone": "+1 303 555 0199" }
                ]
              },
              "doctor": {
                "name": "Dr. House",
                "license": "CO-998877"
              }
            }
            """;
        String frRecord = usRecord.replace("\"US\"", "\"FR\"");

        List<KafkaMessage> out = KafkaPolicyRunner.consume(
            policy,
            new TestKafkaMessage("visits", usRecord),
            new TestKafkaMessage("visits", frRecord)
        );

        assertThat(out).hasSize(2);
        assertThat(json(out.get(0))).isEqualTo(
            MAPPER.readTree(
                """
                {
                  "visitId": "V-2001",
                  "patient": {
                    "name": "John Smith",
                    "ssn": "****",
                    "country": "US",
                    "contacts": [
                      { "type": "home", "phone": "****" },
                      { "type": "work", "phone": "****" }
                    ]
                  },
                  "doctor": {
                    "name": "Dr. House",
                    "license": "****"
                  }
                }
                """
            )
        );
        // condition not met: the record goes through untouched
        assertThat(out.get(1).content()).hasToString(frRecord);
    }

    private static JsonNode json(KafkaMessage message) throws Exception {
        return MAPPER.readTree(message.content().toString());
    }
}
