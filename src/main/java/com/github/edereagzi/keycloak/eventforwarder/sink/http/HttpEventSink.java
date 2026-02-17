package com.github.edereagzi.keycloak.eventforwarder.sink.http;

import com.github.edereagzi.keycloak.eventforwarder.config.ForwarderConfig;
import com.github.edereagzi.keycloak.eventforwarder.sink.EventSink;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.jboss.logging.Logger;

public final class HttpEventSink implements EventSink {

  private static final Logger LOG = Logger.getLogger(HttpEventSink.class);

  private final ForwarderConfig config;
  private final HttpClient client;

  public HttpEventSink(ForwarderConfig config) {
    this.config = config;
    this.client = HttpClient.newBuilder().connectTimeout(config.connectTimeout).build();
  }

  @Override
  public String name() {
    return "http";
  }

  @Override
  public void send(String key, String payloadJson) {
    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder()
            .uri(URI.create(config.endpointUrl))
            .timeout(config.requestTimeout)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payloadJson));

    if (config.authHeaderValue != null && !config.authHeaderValue.isBlank()) {
      requestBuilder.header(config.authHeaderName, config.authHeaderValue);
    }

    HttpRequest request = requestBuilder.build();

    int attempts = config.httpMaxRetries + 1;
    for (int attempt = 1; attempt <= attempts; attempt++) {
      try {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
          return;
        }

        if (isNonRetryableStatus(status)) {
          LOG.warnf(
              "HTTP sink got non-retryable status %d, not retrying. body=%s",
              status,
              response.body());
          return;
        }

        if (attempt == attempts) {
          LOG.warnf(
              "HTTP sink failed after %d attempt(s). status=%d body=%s",
              attempts,
              status,
              response.body());
          return;
        }
      } catch (IOException ex) {
        if (attempt == attempts) {
          LOG.warnf(ex, "HTTP sink failed after %d attempt(s)", attempts);
          return;
        }
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        LOG.warn("HTTP sink interrupted", ex);
        return;
      }

      try {
        Thread.sleep(config.httpRetryBackoff.toMillis() * attempt);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  @Override
  public void close() {
    // no-op
  }

  private static boolean isNonRetryableStatus(int statusCode) {
    return statusCode == 401 || statusCode == 403 || statusCode == 404;
  }
}
