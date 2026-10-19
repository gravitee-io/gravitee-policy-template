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

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.reactive.api.message.kafka.KafkaMessage;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory Kafka record, standing in for the one the gateway builds from a PRODUCE or FETCH request.
 */
public class TestKafkaMessage implements KafkaMessage {

    private final String id = UUID.randomUUID().toString();
    private final String topic;
    private final Map<String, Buffer> recordHeaders = new LinkedHashMap<>();
    private final Map<String, Object> metadata = new HashMap<>();
    private final Map<String, Object> attributes = new HashMap<>();
    private final Map<String, Object> internalAttributes = new HashMap<>();
    private Buffer key;
    private Buffer content;
    private boolean error;

    public TestKafkaMessage(String topic, String content) {
        this.topic = topic;
        this.content = content == null ? null : Buffer.buffer(content);
    }

    public TestKafkaMessage withHeader(String name, String value) {
        recordHeaders.put(name, Buffer.buffer(value));
        return this;
    }

    public TestKafkaMessage withKey(String key) {
        return (TestKafkaMessage) key(key);
    }

    // --- KafkaMessage ---

    @Override
    public Map<String, Buffer> recordHeaders() {
        return recordHeaders;
    }

    @Override
    public KafkaMessage putRecordHeader(String key, Buffer value) {
        recordHeaders.put(key, value);
        return this;
    }

    @Override
    public KafkaMessage removeRecordHeader(String key) {
        recordHeaders.remove(key);
        return this;
    }

    @Override
    public Buffer key() {
        return key;
    }

    @Override
    public KafkaMessage key(Buffer key) {
        this.key = key;
        return this;
    }

    @Override
    public KafkaMessage key(String key) {
        this.key = key == null ? null : Buffer.buffer(key);
        return this;
    }

    @Override
    public long offset() {
        return 0;
    }

    @Override
    public int sequence() {
        return 0;
    }

    @Override
    public int indexPartition() {
        return 0;
    }

    @Override
    public String topic() {
        return topic;
    }

    @Override
    public int sizeInBytes() {
        return content == null ? 0 : content.length();
    }

    // --- Message ---

    @Override
    public String id() {
        return id;
    }

    @Override
    public String correlationId() {
        return null;
    }

    @Override
    public String parentCorrelationId() {
        return null;
    }

    @Override
    public long timestamp() {
        return 0;
    }

    @Override
    public boolean error() {
        return error;
    }

    @Override
    public KafkaMessage error(boolean error) {
        this.error = error;
        return this;
    }

    @Override
    public Map<String, Object> metadata() {
        return metadata;
    }

    @Override
    public Buffer content() {
        return content;
    }

    @Override
    public KafkaMessage content(Buffer content) {
        this.content = content;
        return this;
    }

    @Override
    public KafkaMessage content(String content) {
        this.content = content == null ? null : Buffer.buffer(content);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T attribute(String name) {
        return (T) attributes.get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> attributeAsList(String name) {
        Object value = attributes.get(name);
        return value == null ? null : value instanceof List<?> list ? (List<T>) list : List.of((T) value);
    }

    @Override
    public KafkaMessage attribute(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    @Override
    public KafkaMessage removeAttribute(String name) {
        attributes.remove(name);
        return this;
    }

    @Override
    public Set<String> attributeNames() {
        return attributes.keySet();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> attributes() {
        return (Map<String, T>) attributes;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T internalAttribute(String name) {
        return (T) internalAttributes.get(name);
    }

    @Override
    public KafkaMessage internalAttribute(String name, Object value) {
        internalAttributes.put(name, value);
        return this;
    }

    @Override
    public KafkaMessage removeInternalAttribute(String name) {
        internalAttributes.remove(name);
        return this;
    }

    @Override
    public Set<String> internalAttributeNames() {
        return internalAttributes.keySet();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> internalAttributes() {
        return (Map<String, T>) internalAttributes;
    }
}
