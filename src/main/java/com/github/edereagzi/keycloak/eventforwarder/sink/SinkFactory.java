package com.github.edereagzi.keycloak.eventforwarder.sink;

import com.github.edereagzi.keycloak.eventforwarder.config.ForwarderConfig;
import com.github.edereagzi.keycloak.eventforwarder.sink.http.HttpEventSink;
import com.github.edereagzi.keycloak.eventforwarder.sink.kafka.KafkaEventSink;
import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;

public final class SinkFactory {

  private static final Logger LOG = Logger.getLogger(SinkFactory.class);

  private SinkFactory() {}

  public static List<EventSink> createSinks(ForwarderConfig config) {
    List<EventSink> sinks = new ArrayList<>();

    if (config.hasHttpSink()) {
      if (config.endpointUrl == null || config.endpointUrl.isBlank()) {
        throw new IllegalStateException("HTTP sink selected but KEF_ENDPOINT_URL is empty");
      }
      sinks.add(new HttpEventSink(config));
      LOG.infov("HTTP sink enabled for endpoint {0}", config.endpointUrl);
    }

    if (config.hasKafkaSink()) {
      if (config.kafkaBootstrapServers == null || config.kafkaBootstrapServers.isBlank()) {
        throw new IllegalStateException(
            "Kafka sink selected but KEF_KAFKA_BOOTSTRAP_SERVERS is empty");
      }
      if (config.kafkaTopic == null || config.kafkaTopic.isBlank()) {
        throw new IllegalStateException("Kafka sink selected but KEF_KAFKA_TOPIC is empty");
      }
      sinks.add(new KafkaEventSink(config));
      LOG.infov("Kafka sink enabled for brokers {0} topic {1}", config.kafkaBootstrapServers, config.kafkaTopic);
    }

    if (sinks.isEmpty()) {
      throw new IllegalStateException("No sink configured. Check KEF_SINK_TYPE and sink settings");
    }

    return sinks;
  }
}
