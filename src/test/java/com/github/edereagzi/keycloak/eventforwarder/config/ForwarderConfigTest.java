package com.github.edereagzi.keycloak.eventforwarder.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;
import org.keycloak.events.EventType;

class ForwarderConfigTest {

  @Test
  void parsesSinkTypeAndFilters() {
    Config.Scope scope =
        scope(
            Map.of(
                "sink-type", "both",
                "include-events", "LOGIN,REGISTER,NOT_REAL",
                "include-admin-operations", "CREATE,UPDATE",
                "include-admin-resources", "USER,REALM"));

    ForwarderConfig config = ForwarderConfig.from(scope);

    assertEquals(ForwarderConfig.SinkType.BOTH, config.sinkType);
    assertTrue(config.matchesUserEvent(EventType.LOGIN));
    assertTrue(config.matchesUserEvent(EventType.REGISTER));
    assertFalse(config.matchesUserEvent(EventType.LOGOUT));
    assertTrue(config.matchesAdminOperation("create"));
    assertFalse(config.matchesAdminOperation("delete"));
    assertTrue(config.matchesAdminResource("realm"));
  }

  @Test
  void appliesSafeDefaultsForInvalidNumericValues() {
    Config.Scope scope =
        scope(
            Map.of(
                "worker-threads", "-5",
                "queue-capacity", "0",
                "kafka-max-in-flight", "0",
                "max-retries", "-1"));

    ForwarderConfig config = ForwarderConfig.from(scope);

    assertEquals(1, config.workerThreads);
    assertEquals(1, config.queueCapacity);
    assertEquals(1, config.kafkaMaxInFlight);
    assertEquals(0, config.httpMaxRetries);
  }

  @Test
  void parsesKafkaExtraProperties() {
    Config.Scope scope =
        scope(
            Map.of(
                "kafka-extra-properties",
                "request.timeout.ms=15000,retries=10,max.block.ms=5000"));

    ForwarderConfig config = ForwarderConfig.from(scope);

    assertEquals("15000", config.kafkaExtraProperties.get("request.timeout.ms"));
    assertEquals("10", config.kafkaExtraProperties.get("retries"));
    assertEquals("5000", config.kafkaExtraProperties.get("max.block.ms"));
  }

  private static Config.Scope scope(Map<String, String> values) {
    Config.Scope scope = mock(Config.Scope.class);
    when(scope.get(anyString()))
        .thenAnswer(
            invocation -> {
              String key = invocation.getArgument(0, String.class);
              return values.get(key);
            });
    return scope;
  }
}
