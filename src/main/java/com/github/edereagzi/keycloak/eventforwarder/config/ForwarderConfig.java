package com.github.edereagzi.keycloak.eventforwarder.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventType;

public final class ForwarderConfig {

  public enum SinkType {
    HTTP,
    KAFKA,
    BOTH
  }

  private static final Logger LOG = Logger.getLogger(ForwarderConfig.class);

  public final SinkType sinkType;

  public final boolean sendUserEvents;
  public final boolean sendAdminEvents;
  public final boolean includeRepresentation;
  public final Set<EventType> includedUserEventTypes;
  public final Set<String> includedAdminOperations;
  public final Set<String> includedAdminResources;

  public final int queueCapacity;
  public final Duration enqueueTimeout;
  public final int workerThreads;

  public final String endpointUrl;
  public final String authHeaderName;
  public final String authHeaderValue;
  public final Duration connectTimeout;
  public final Duration requestTimeout;
  public final int httpMaxRetries;
  public final Duration httpRetryBackoff;

  public final String kafkaBootstrapServers;
  public final String kafkaTopic;
  public final String kafkaClientId;
  public final String kafkaAcks;
  public final String kafkaCompressionType;
  public final Duration kafkaLinger;
  public final int kafkaBatchSize;
  public final Duration kafkaDeliveryTimeout;
  public final int kafkaMaxInFlight;
  public final boolean kafkaEnableIdempotence;
  public final String kafkaSecurityProtocol;
  public final String kafkaSaslMechanism;
  public final String kafkaSaslJaasConfig;
  public final Map<String, String> kafkaExtraProperties;

  private ForwarderConfig(
      SinkType sinkType,
      boolean sendUserEvents,
      boolean sendAdminEvents,
      boolean includeRepresentation,
      Set<EventType> includedUserEventTypes,
      Set<String> includedAdminOperations,
      Set<String> includedAdminResources,
      int queueCapacity,
      Duration enqueueTimeout,
      int workerThreads,
      String endpointUrl,
      String authHeaderName,
      String authHeaderValue,
      Duration connectTimeout,
      Duration requestTimeout,
      int httpMaxRetries,
      Duration httpRetryBackoff,
      String kafkaBootstrapServers,
      String kafkaTopic,
      String kafkaClientId,
      String kafkaAcks,
      String kafkaCompressionType,
      Duration kafkaLinger,
      int kafkaBatchSize,
      Duration kafkaDeliveryTimeout,
      int kafkaMaxInFlight,
      boolean kafkaEnableIdempotence,
      String kafkaSecurityProtocol,
      String kafkaSaslMechanism,
      String kafkaSaslJaasConfig,
      Map<String, String> kafkaExtraProperties) {
    this.sinkType = sinkType;
    this.sendUserEvents = sendUserEvents;
    this.sendAdminEvents = sendAdminEvents;
    this.includeRepresentation = includeRepresentation;
    this.includedUserEventTypes = includedUserEventTypes;
    this.includedAdminOperations = includedAdminOperations;
    this.includedAdminResources = includedAdminResources;
    this.queueCapacity = queueCapacity;
    this.enqueueTimeout = enqueueTimeout;
    this.workerThreads = workerThreads;
    this.endpointUrl = endpointUrl;
    this.authHeaderName = authHeaderName;
    this.authHeaderValue = authHeaderValue;
    this.connectTimeout = connectTimeout;
    this.requestTimeout = requestTimeout;
    this.httpMaxRetries = httpMaxRetries;
    this.httpRetryBackoff = httpRetryBackoff;
    this.kafkaBootstrapServers = kafkaBootstrapServers;
    this.kafkaTopic = kafkaTopic;
    this.kafkaClientId = kafkaClientId;
    this.kafkaAcks = kafkaAcks;
    this.kafkaCompressionType = kafkaCompressionType;
    this.kafkaLinger = kafkaLinger;
    this.kafkaBatchSize = kafkaBatchSize;
    this.kafkaDeliveryTimeout = kafkaDeliveryTimeout;
    this.kafkaMaxInFlight = kafkaMaxInFlight;
    this.kafkaEnableIdempotence = kafkaEnableIdempotence;
    this.kafkaSecurityProtocol = kafkaSecurityProtocol;
    this.kafkaSaslMechanism = kafkaSaslMechanism;
    this.kafkaSaslJaasConfig = kafkaSaslJaasConfig;
    this.kafkaExtraProperties = kafkaExtraProperties;
  }

  public static ForwarderConfig from(Config.Scope scope) {
    SinkType sinkType = sinkType(value(scope, "sink-type", "KEF_SINK_TYPE", "HTTP"));

    boolean sendUserEvents = boolValue(scope, "send-user-events", "KEF_SEND_USER_EVENTS", true);
    boolean sendAdminEvents = boolValue(scope, "send-admin-events", "KEF_SEND_ADMIN_EVENTS", true);
    boolean includeRepresentation =
        boolValue(scope, "include-representation", "KEF_INCLUDE_REPRESENTATION", false);

    Set<EventType> includedUserEventTypes =
        parseEventTypes(value(scope, "include-events", "KEF_INCLUDE_EVENTS", "*"));
    Set<String> includedAdminOperations =
        parseStringSet(value(scope, "include-admin-operations", "KEF_INCLUDE_ADMIN_OPERATIONS", "*"));
    Set<String> includedAdminResources =
        parseStringSet(value(scope, "include-admin-resources", "KEF_INCLUDE_ADMIN_RESOURCES", "*"));

    int queueCapacity = intValue(scope, "queue-capacity", "KEF_QUEUE_CAPACITY", 10000);
    Duration enqueueTimeout =
        Duration.ofMillis(intValue(scope, "enqueue-timeout-ms", "KEF_ENQUEUE_TIMEOUT_MS", 100));
    int workerThreads = intValue(scope, "worker-threads", "KEF_WORKER_THREADS", 2);

    String endpointUrl = value(scope, "endpoint-url", "KEF_ENDPOINT_URL", "");
    String authHeaderName =
        value(scope, "auth-header-name", "KEF_AUTH_HEADER_NAME", "X-Webhook-Secret");
    String authHeaderValue = value(scope, "auth-header-value", "KEF_AUTH_HEADER_VALUE", "");
    Duration connectTimeout =
        Duration.ofMillis(intValue(scope, "connect-timeout-ms", "KEF_CONNECT_TIMEOUT_MS", 2000));
    Duration requestTimeout =
        Duration.ofMillis(intValue(scope, "request-timeout-ms", "KEF_REQUEST_TIMEOUT_MS", 5000));
    int httpMaxRetries = intValue(scope, "max-retries", "KEF_MAX_RETRIES", 2);
    Duration httpRetryBackoff =
        Duration.ofMillis(intValue(scope, "retry-backoff-ms", "KEF_RETRY_BACKOFF_MS", 250));

    String kafkaBootstrapServers =
        value(scope, "kafka-bootstrap-servers", "KEF_KAFKA_BOOTSTRAP_SERVERS", "");
    String kafkaTopic = value(scope, "kafka-topic", "KEF_KAFKA_TOPIC", "keycloak.events");
    String kafkaClientId = value(scope, "kafka-client-id", "KEF_KAFKA_CLIENT_ID", "keycloak-event-forwarder");
    String kafkaAcks = value(scope, "kafka-acks", "KEF_KAFKA_ACKS", "all");
    String kafkaCompressionType =
        value(scope, "kafka-compression-type", "KEF_KAFKA_COMPRESSION_TYPE", "lz4");
    Duration kafkaLinger = Duration.ofMillis(intValue(scope, "kafka-linger-ms", "KEF_KAFKA_LINGER_MS", 20));
    int kafkaBatchSize = intValue(scope, "kafka-batch-size", "KEF_KAFKA_BATCH_SIZE", 32768);
    Duration kafkaDeliveryTimeout =
        Duration.ofMillis(
            intValue(scope, "kafka-delivery-timeout-ms", "KEF_KAFKA_DELIVERY_TIMEOUT_MS", 120000));
    int kafkaMaxInFlight =
        intValue(scope, "kafka-max-in-flight", "KEF_KAFKA_MAX_IN_FLIGHT_REQUESTS", 5);
    boolean kafkaEnableIdempotence =
        boolValue(scope, "kafka-enable-idempotence", "KEF_KAFKA_ENABLE_IDEMPOTENCE", true);
    String kafkaSecurityProtocol =
        value(scope, "kafka-security-protocol", "KEF_KAFKA_SECURITY_PROTOCOL", "");
    String kafkaSaslMechanism = value(scope, "kafka-sasl-mechanism", "KEF_KAFKA_SASL_MECHANISM", "");
    String kafkaSaslJaasConfig = value(scope, "kafka-sasl-jaas-config", "KEF_KAFKA_SASL_JAAS_CONFIG", "");
    Map<String, String> kafkaExtraProperties =
        parseKeyValuePairs(
            value(scope, "kafka-extra-properties", "KEF_KAFKA_EXTRA_PROPERTIES", ""));

    return new ForwarderConfig(
        sinkType,
        sendUserEvents,
        sendAdminEvents,
        includeRepresentation,
        includedUserEventTypes,
        includedAdminOperations,
        includedAdminResources,
        Math.max(queueCapacity, 1),
        enqueueTimeout.isNegative() ? Duration.ZERO : enqueueTimeout,
        Math.max(workerThreads, 1),
        endpointUrl,
        authHeaderName,
        authHeaderValue,
        connectTimeout.isNegative() ? Duration.ofMillis(2000) : connectTimeout,
        requestTimeout.isNegative() ? Duration.ofMillis(5000) : requestTimeout,
        Math.max(httpMaxRetries, 0),
        httpRetryBackoff.isNegative() ? Duration.ZERO : httpRetryBackoff,
        kafkaBootstrapServers,
        kafkaTopic,
        kafkaClientId,
        kafkaAcks,
        kafkaCompressionType,
        kafkaLinger.isNegative() ? Duration.ZERO : kafkaLinger,
        Math.max(kafkaBatchSize, 0),
        kafkaDeliveryTimeout.isNegative() ? Duration.ofMillis(120000) : kafkaDeliveryTimeout,
        Math.max(kafkaMaxInFlight, 1),
        kafkaEnableIdempotence,
        kafkaSecurityProtocol,
        kafkaSaslMechanism,
        kafkaSaslJaasConfig,
        kafkaExtraProperties);
  }

  public boolean hasHttpSink() {
    return sinkType == SinkType.HTTP || sinkType == SinkType.BOTH;
  }

  public boolean hasKafkaSink() {
    return sinkType == SinkType.KAFKA || sinkType == SinkType.BOTH;
  }

  public boolean matchesUserEvent(EventType eventType) {
    return includedUserEventTypes.isEmpty() || includedUserEventTypes.contains(eventType);
  }

  public boolean matchesAdminOperation(String operationType) {
    return includedAdminOperations.isEmpty()
        || includedAdminOperations.contains(operationType.toUpperCase(Locale.ROOT));
  }

  public boolean matchesAdminResource(String resourceType) {
    return includedAdminResources.isEmpty()
        || includedAdminResources.contains(resourceType.toUpperCase(Locale.ROOT));
  }

  private static SinkType sinkType(String raw) {
    try {
      return SinkType.valueOf(raw.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      LOG.warnf("Unknown sink type '%s'. Falling back to HTTP", raw);
      return SinkType.HTTP;
    }
  }

  private static String value(Config.Scope scope, String key, String envKey, String fallback) {
    String env = System.getenv(envKey);
    if (env != null && !env.isBlank()) {
      return env.trim();
    }

    String configured = scope != null ? scope.get(key) : null;
    if (configured != null && !configured.isBlank()) {
      return configured.trim();
    }

    return fallback;
  }

  private static boolean boolValue(Config.Scope scope, String key, String envKey, boolean fallback) {
    String value = value(scope, key, envKey, Boolean.toString(fallback));
    return Boolean.parseBoolean(value);
  }

  private static int intValue(Config.Scope scope, String key, String envKey, int fallback) {
    String value = value(scope, key, envKey, Integer.toString(fallback));
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ignored) {
      return fallback;
    }
  }

  private static Set<EventType> parseEventTypes(String raw) {
    if (raw == null || raw.isBlank() || "*".equals(raw.trim())) {
      return Collections.emptySet();
    }

    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .map(s -> s.toUpperCase(Locale.ROOT))
        .map(
            s -> {
              try {
                return EventType.valueOf(s);
              } catch (IllegalArgumentException ex) {
                LOG.warnf("Ignoring unknown user event type '%s' in KEF_INCLUDE_EVENTS", s);
                return null;
              }
            })
        .filter(e -> e != null)
        .collect(Collectors.toSet());
  }

  private static Set<String> parseStringSet(String raw) {
    if (raw == null || raw.isBlank() || "*".equals(raw.trim())) {
      return Collections.emptySet();
    }

    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .map(s -> s.toUpperCase(Locale.ROOT))
        .collect(Collectors.toSet());
  }

  private static Map<String, String> parseKeyValuePairs(String raw) {
    if (raw == null || raw.isBlank()) {
      return Collections.emptyMap();
    }

    Map<String, String> values = new LinkedHashMap<>();
    for (String token : raw.split(",")) {
      String pair = token.trim();
      if (pair.isBlank()) {
        continue;
      }

      int splitAt = pair.indexOf('=');
      if (splitAt <= 0 || splitAt == pair.length() - 1) {
        LOG.warnf("Ignoring invalid KEF_KAFKA_EXTRA_PROPERTIES item '%s'", pair);
        continue;
      }

      String key = pair.substring(0, splitAt).trim();
      String value = pair.substring(splitAt + 1).trim();
      if (key.isBlank() || value.isBlank()) {
        LOG.warnf("Ignoring invalid KEF_KAFKA_EXTRA_PROPERTIES item '%s'", pair);
        continue;
      }
      values.put(key, value);
    }

    return values;
  }
}
