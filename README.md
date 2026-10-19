# Gravitee Policy Template — Native Kafka

A starter project for writing a [Gravitee](https://www.gravitee.io) policy that runs on a **native Kafka API**:
the gateway speaks the Kafka protocol to clients and brokers, and your policy sees every record that goes through.

It ships one example policy, **Kafka Add Header**, and a small test harness that runs a policy without a gateway,
a broker or Docker.

## Requirements

- JDK 21
- Maven 3.9+

Every dependency comes from Maven Central. No Gravitee account or private repository is needed.

```bash
mvn test          # run the tests
mvn package       # build the plugin zip in target/
```

## How a policy sits in a native Kafka flow

```
 producer ──PRODUCE──▶ ┌──────────── Gravitee gateway ────────────┐ ──▶ broker
                       │  onMessageRequest(ctx)  — each record   │
 consumer ◀──FETCH──── │  onMessageResponse(ctx) — each record   │ ◀── broker
                       └──────────────────────────────────────────┘
```

A native Kafka policy implements `io.gravitee.gateway.reactive.api.policy.kafka.KafkaPolicy`.
The two phases that see records are:

| Phase               | Runs on                                   | Plugin phase |
|---------------------|-------------------------------------------|--------------|
| `onMessageRequest`  | records **produced** by a client          | `PUBLISH`    |
| `onMessageResponse` | records **fetched** by a consumer         | `SUBSCRIBE`  |

Both receive a `KafkaMessageExecutionContext`. The usual pattern is to plug a per-record function into the stream:

```java
@Override
public Completable onMessageRequest(KafkaMessageExecutionContext ctx) {
    return ctx.request().onMessage(record -> transform(ctx, record)); // Maybe<KafkaMessage>
}
```

- Return `Maybe.just(record)` to forward the record (modified or not).
- Return `Maybe.empty()` to drop it.
- Return `ctx.executionContext().interruptWith(Errors.X).toMaybe()` to reject the whole Kafka request with a Kafka error code.

The gateway is reactive (RxJava 3 on Vert.x): the function runs on an event loop and must never block.

### The record: `KafkaMessage`

| Method                                   | What it gives                         |
|------------------------------------------|---------------------------------------|
| `content()` / `content(String)`          | the record value (a `Buffer`)         |
| `key()` / `key(String)`                  | the record key                        |
| `topic()`                                | the topic name                        |
| `recordHeaders()` / `putRecordHeader(...)` / `removeRecordHeader(...)` | the record headers |

### Expression Language (EL)

Policy configuration fields can hold [Gravitee EL](https://documentation.gravitee.io/apim/guides/gravitee-expression-language)
expressions, evaluated per record with `ctx.getTemplateEngine(record).eval(expression, Type.class)`.
The record is exposed as `#message`:

| Expression                                        | Value                         |
|---------------------------------------------------|-------------------------------|
| `{#message.topic}`                                | topic name                    |
| `{#message.key}`                                  | record key                    |
| `{#message.content}`                              | record value, as a string     |
| `{#message.headersString['source']}`              | a record header               |
| `{#jsonPath(#message.content, '$.user.country')}` | a value inside a JSON payload |

## The example policy: Kafka Add Header

`src/main/java/io/gravitee/policy/kafka/addheader/KafkaAddHeaderPolicy.java`

Adds a header to each produced or consumed record for which the EL `condition` is true.

```json
{
  "condition": "{#jsonPath(#message.content, '$.amount') > 1000}",
  "headerName": "X-Flagged",
  "headerValue": "true"
}
```

| Property      | Required | Description                                                       |
|---------------|----------|-------------------------------------------------------------------|
| `condition`   | no       | EL evaluated per record. Empty means "always add the header".    |
| `headerName`  | yes      | name of the header to add                                         |
| `headerValue` | yes      | value of the header to add                                        |

If the condition cannot be evaluated, the policy rejects the Kafka request with `INVALID_REQUEST`
rather than letting the record through unchecked.

## Testing a policy

`src/test/java/io/gravitee/policy/kafka/testing/` contains everything needed to unit-test a native Kafka policy:

| Class                   | Role                                                                     |
|-------------------------|--------------------------------------------------------------------------|
| `TestKafkaMessage`      | an in-memory record: `new TestKafkaMessage("orders", "{...}").withHeader("k", "v")` |
| `KafkaPolicyRunner`     | runs a policy over records: `produce(policy, records...)` / `consume(policy, records...)` |
| `EvaluableKafkaMessage` | what the gateway exposes as `#message` to EL                             |

EL is evaluated by the real Gravitee expression-language engine. When the policy interrupts the request,
`KafkaPolicyRunner` throws a `KafkaInterruptionException` holding the Kafka `Errors` code.

```java
List<KafkaMessage> out = KafkaPolicyRunner.produce(
    policy,
    new TestKafkaMessage("payments", "{\"amount\":5000}")
);
assertThat(out.get(0).recordHeaders()).containsKey("X-Flagged");
```

See `KafkaAddHeaderPolicyTest` for complete examples.

## Making it your own

1. Rename the package, the policy class and its configuration.
2. Update `src/main/resources/plugin.properties` (`id`, `name`, `class`).
3. Describe the configuration in `src/main/resources/schemas/schema-form.json`. The Console uses it to render the form.
4. `mvn package` and drop `target/<artifact>-<version>.zip` in the gateway's `plugins` directory.

Code style is checked by Prettier at build time: run `mvn prettier:write` to format, or `-Dskip.validation` to skip the check locally.
