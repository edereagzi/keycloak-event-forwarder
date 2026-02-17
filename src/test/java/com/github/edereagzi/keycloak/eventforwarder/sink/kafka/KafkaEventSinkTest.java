package com.github.edereagzi.keycloak.eventforwarder.sink.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.edereagzi.keycloak.eventforwarder.config.ForwarderConfig;
import java.util.Map;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;

class KafkaEventSinkTest {

  @Test
  void sendsToConfiguredTopicWithKey() {
    ForwarderConfig config =
        config(
            Map.of(
                "sink-type", "kafka",
                "kafka-bootstrap-servers", "localhost:9092",
                "kafka-topic", "keycloak.events.test"));

    MockProducer<String, String> producer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());
    KafkaEventSink sink = new KafkaEventSink(config, producer);

    sink.send("realm-x", "{\"eventId\":\"a\"}");

    assertEquals(1, producer.history().size());
    assertEquals("keycloak.events.test", producer.history().get(0).topic());
    assertEquals("realm-x", producer.history().get(0).key());
    assertEquals("{\"eventId\":\"a\"}", producer.history().get(0).value());
  }

  private static ForwarderConfig config(Map<String, String> values) {
    Config.Scope scope = mock(Config.Scope.class);
    when(scope.get(anyString()))
        .thenAnswer(
            invocation -> {
              String key = invocation.getArgument(0, String.class);
              return values.get(key);
            });
    return ForwarderConfig.from(scope);
  }

}
