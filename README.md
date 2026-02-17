# keycloak-event-forwarder

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-17%2B-orange.svg)](pom.xml)
[![Keycloak](https://img.shields.io/badge/keycloak-26.5.3-red.svg)](pom.xml)

`keycloak-event-forwarder` is a Keycloak Event Listener SPI plugin that pushes user and admin events to external systems in near real time.

## What it solves

Keycloak stores valuable security and audit events, but many teams need them outside Keycloak immediately.
This plugin forwards those events to:

- HTTP webhook endpoints
- Kafka topics
- Both sinks at the same time

No polling. No direct database reads.

## Features

- Asynchronous, non-blocking dispatch from Keycloak request threads
- Sink modes: `HTTP`, `KAFKA`, `BOTH`
- HTTP retries with linear backoff
- Kafka producer configuration with idempotence support
- Event filtering by type/operation/resource
- Single deployable plugin artifact (fat JAR)

## Quick Start

### 1. Build

```bash
mvn clean package
```

Artifact:
- `target/keycloak-event-forwarder-0.1.0-all.jar`

### 2. Install into Keycloak

```bash
cp target/keycloak-event-forwarder-0.1.0-all.jar /opt/keycloak/providers/
```

### 3. Configure environment variables

Example (Kafka):

```bash
KEF_SINK_TYPE=KAFKA
KEF_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
KEF_KAFKA_TOPIC=keycloak.events
KEF_SEND_USER_EVENTS=true
KEF_SEND_ADMIN_EVENTS=true
```

### 4. Restart Keycloak

### 5. Enable listener per realm

In Keycloak Admin Console:

1. `Realm Settings` -> `Events`
2. Add `event-forwarder` to `Event Listeners`
3. Save

Without this step, no events are forwarded.

## Docker Example

```bash
docker run --name keycloak \
  -p 8080:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  -e KEF_SINK_TYPE=HTTP \
  -e KEF_ENDPOINT_URL=https://your-webhook.example/events \
  -e KEF_AUTH_HEADER_NAME=X-Webhook-Secret \
  -e KEF_AUTH_HEADER_VALUE=replace-with-long-random-secret \
  -v $(pwd)/target/keycloak-event-forwarder-0.1.0-all.jar:/opt/keycloak/providers/keycloak-event-forwarder-0.1.0-all.jar \
  quay.io/keycloak/keycloak:26.5.3 \
  start
```

## Kubernetes

Recommended approach: build a custom Keycloak image with the plugin baked in.

```dockerfile
FROM quay.io/keycloak/keycloak:26.5.3
COPY target/keycloak-event-forwarder-0.1.0-all.jar /opt/keycloak/providers/
```

Kubernetes examples:

- `examples/kubernetes/secret.yaml`
- `examples/kubernetes/deployment-env-patch.yaml`
- `examples/kubernetes/deployment-env-kafka-patch.yaml`

## Configuration

### Common

| Variable | Default | Description |
|---|---|---|
| `KEF_SINK_TYPE` | `HTTP` | `HTTP`, `KAFKA`, or `BOTH` |
| `KEF_SEND_USER_EVENTS` | `true` | Enable user event forwarding |
| `KEF_SEND_ADMIN_EVENTS` | `true` | Enable admin event forwarding |
| `KEF_INCLUDE_EVENTS` | `*` | Comma-separated user event types |
| `KEF_INCLUDE_ADMIN_OPERATIONS` | `*` | Comma-separated admin operations |
| `KEF_INCLUDE_ADMIN_RESOURCES` | `*` | Comma-separated admin resource types |
| `KEF_INCLUDE_REPRESENTATION` | `false` | Include admin representation payload |
| `KEF_QUEUE_CAPACITY` | `10000` | Async queue capacity |
| `KEF_ENQUEUE_TIMEOUT_MS` | `100` | Queue enqueue timeout |
| `KEF_WORKER_THREADS` | `2` | Number of dispatch workers |

### HTTP Sink

| Variable | Default | Description |
|---|---|---|
| `KEF_ENDPOINT_URL` | _empty_ | Webhook endpoint |
| `KEF_AUTH_HEADER_NAME` | `X-Webhook-Secret` | Authentication header name |
| `KEF_AUTH_HEADER_VALUE` | _empty_ | Authentication header value |
| `KEF_CONNECT_TIMEOUT_MS` | `2000` | HTTP connect timeout |
| `KEF_REQUEST_TIMEOUT_MS` | `5000` | HTTP request timeout |
| `KEF_MAX_RETRIES` | `2` | Retry count |
| `KEF_RETRY_BACKOFF_MS` | `250` | Linear retry backoff base |

HTTP retry behavior:

- No retry for `401`, `403`, `404`
- Retry for `429` and `5xx`

### Kafka Sink

| Variable | Default | Description |
|---|---|---|
| `KEF_KAFKA_BOOTSTRAP_SERVERS` | _empty_ | Kafka broker list |
| `KEF_KAFKA_TOPIC` | `keycloak.events` | Topic name |
| `KEF_KAFKA_CLIENT_ID` | `keycloak-event-forwarder` | Producer client ID |
| `KEF_KAFKA_ACKS` | `all` | Producer acks |
| `KEF_KAFKA_COMPRESSION_TYPE` | `lz4` | Compression type |
| `KEF_KAFKA_LINGER_MS` | `20` | Linger time |
| `KEF_KAFKA_BATCH_SIZE` | `32768` | Batch size |
| `KEF_KAFKA_DELIVERY_TIMEOUT_MS` | `120000` | Delivery timeout |
| `KEF_KAFKA_MAX_IN_FLIGHT_REQUESTS` | `5` | Max in-flight requests |
| `KEF_KAFKA_ENABLE_IDEMPOTENCE` | `true` | Enable idempotence |
| `KEF_KAFKA_SECURITY_PROTOCOL` | _empty_ | Optional security protocol |
| `KEF_KAFKA_SASL_MECHANISM` | _empty_ | Optional SASL mechanism |
| `KEF_KAFKA_SASL_JAAS_CONFIG` | _empty_ | Optional SASL JAAS config |
| `KEF_KAFKA_EXTRA_PROPERTIES` | _empty_ | Extra Kafka producer properties in `key=value,key2=value2` format |

Example:

```bash
KEF_KAFKA_EXTRA_PROPERTIES=request.timeout.ms=15000,retries=10,max.block.ms=5000
```

## Recommended Presets

### HTTP only

```bash
KEF_SINK_TYPE=HTTP
KEF_ENDPOINT_URL=https://events.internal.example/keycloak
KEF_AUTH_HEADER_NAME=X-Webhook-Secret
KEF_AUTH_HEADER_VALUE=<secret>
KEF_MAX_RETRIES=3
KEF_WORKER_THREADS=2
KEF_QUEUE_CAPACITY=20000
```

### Kafka only

```bash
KEF_SINK_TYPE=KAFKA
KEF_KAFKA_BOOTSTRAP_SERVERS=kafka-1:9092,kafka-2:9092,kafka-3:9092
KEF_KAFKA_TOPIC=security.keycloak.events
KEF_KAFKA_ACKS=all
KEF_KAFKA_ENABLE_IDEMPOTENCE=true
KEF_WORKER_THREADS=4
KEF_QUEUE_CAPACITY=50000
```

### Both sinks

```bash
KEF_SINK_TYPE=BOTH
KEF_ENDPOINT_URL=https://events.internal.example/keycloak
KEF_KAFKA_BOOTSTRAP_SERVERS=kafka-1:9092,kafka-2:9092,kafka-3:9092
```

## Event Payloads

### User Event

```json
{
  "eventId": "4e42d598-80f8-4b08-a56a-f1138cd72d10",
  "source": "USER_EVENT",
  "timestamp": "2026-02-17T20:00:00Z",
  "realmId": "master",
  "realmName": "master",
  "type": "LOGIN",
  "clientId": "account",
  "userId": "a1b2c3",
  "sessionId": "xyz",
  "ipAddress": "10.0.0.10",
  "error": null,
  "details": {
    "auth_method": "openid-connect"
  }
}
```

### Admin Event

```json
{
  "eventId": "f5f8461a-ab06-4f5f-b5d1-4cd7ed0adf28",
  "source": "ADMIN_EVENT",
  "timestamp": "2026-02-17T20:00:01Z",
  "realmId": "master",
  "operationType": "UPDATE",
  "resourceType": "USER",
  "resourcePath": "users/a1b2c3",
  "representationIncluded": false,
  "representation": null,
  "auth": {
    "clientId": "security-admin-console",
    "userId": "admin",
    "ipAddress": "10.0.0.11",
    "realmId": "master"
  },
  "error": null
}
```

## Delivery Semantics

- Delivery model: at-least-once
- Async queue and worker model (request threads are not blocked)
- For strict global ordering, set `KEF_WORKER_THREADS=1`

## Troubleshooting

### Events are not forwarded

- Confirm `event-forwarder` is enabled in realm event listeners
- Check startup logs for `event-forwarder initialized`
- Verify sink-specific required env vars are present

### HTTP failures

- Verify secret header name/value configuration
- Ensure endpoint returns `2xx`
- Increase `KEF_REQUEST_TIMEOUT_MS` if needed

### Kafka failures

- Verify broker addresses are reachable from Keycloak runtime/network
- Ensure topic exists
- Configure SASL/SSL settings when required

## Testing

```bash
mvn clean verify
```

## Contributing

Issues and pull requests are welcome.

## License

Apache License 2.0. See `LICENSE`.
