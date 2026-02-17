package com.github.edereagzi.keycloak.eventforwarder.sink.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.edereagzi.keycloak.eventforwarder.config.ForwarderConfig;
import java.io.IOException;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;

class HttpEventSinkTest {

  @Test
  void postsJsonWithAuthHeader() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(new MockResponse().setResponseCode(200));
      server.start();

      ForwarderConfig config =
          config(
              Map.of(
                  "endpoint-url", server.url("/events").toString(),
                  "auth-header-name", "X-Webhook-Secret",
                  "auth-header-value", "test-secret"));

      HttpEventSink sink = new HttpEventSink(config);
      sink.send("realm-a", "{\"eventId\":\"1\"}");

      RecordedRequest request = server.takeRequest();
      assertEquals("/events", request.getPath());
      assertEquals("test-secret", request.getHeader("X-Webhook-Secret"));
      assertEquals("{\"eventId\":\"1\"}", request.getBody().readUtf8());
    }
  }

  @Test
  void doesNotRetryOnUnauthorized() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(new MockResponse().setResponseCode(401).setBody("unauthorized"));
      server.start();

      ForwarderConfig config = config(Map.of("endpoint-url", server.url("/events").toString()));
      HttpEventSink sink = new HttpEventSink(config);
      sink.send("realm-a", "{\"eventId\":\"1\"}");

      assertEquals(1, server.getRequestCount());
      assertNotNull(server.takeRequest());
      assertNull(server.takeRequest(200, java.util.concurrent.TimeUnit.MILLISECONDS));
    }
  }

  @Test
  void retriesOnTooManyRequests() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(new MockResponse().setResponseCode(429).setBody("rate-limited"));
      server.enqueue(new MockResponse().setResponseCode(429).setBody("rate-limited"));
      server.enqueue(new MockResponse().setResponseCode(200));
      server.start();

      ForwarderConfig config = config(Map.of("endpoint-url", server.url("/events").toString()));
      HttpEventSink sink = new HttpEventSink(config);
      sink.send("realm-a", "{\"eventId\":\"1\"}");

      assertEquals(3, server.getRequestCount());
    }
  }

  private static ForwarderConfig config(Map<String, String> values) throws IOException {
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
