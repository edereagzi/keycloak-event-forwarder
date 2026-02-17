package com.github.edereagzi.keycloak.eventforwarder.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.edereagzi.keycloak.eventforwarder.config.ForwarderConfig;
import com.github.edereagzi.keycloak.eventforwarder.sink.EventSink;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;

class EventDispatcherTest {

  @Test
  void dispatchesPayloadToAllSinks() throws Exception {
    CollectingSink first = new CollectingSink("first");
    CollectingSink second = new CollectingSink("second");

    ForwarderConfig config = config(Map.of("queue-capacity", "100", "worker-threads", "1"));

    try (EventDispatcher dispatcher = new EventDispatcher(config, List.of(first, second))) {
      dispatcher.dispatch("realm-1", Map.of("eventId", "e-1", "source", "USER_EVENT"));

      long deadline = System.currentTimeMillis() + 3_000;
      while (System.currentTimeMillis() < deadline && first.events.size() < 1) {
        Thread.sleep(20);
      }
    }

    assertEquals(1, first.events.size());
    assertEquals(1, second.events.size());
    assertTrue(first.events.get(0).contains("\"eventId\":\"e-1\""));
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

  private static final class CollectingSink implements EventSink {

    private final String name;
    private final List<String> events;

    private CollectingSink(String name) {
      this.name = name;
      this.events = Collections.synchronizedList(new java.util.ArrayList<>());
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public void send(String key, String payloadJson) {
      events.add(payloadJson);
    }

    @Override
    public void close() {
      // no-op
    }
  }
}
