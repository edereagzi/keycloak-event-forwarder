package com.github.edereagzi.keycloak.eventforwarder.sink.kafka;

import com.github.edereagzi.keycloak.eventforwarder.config.ForwarderConfig;
import com.github.edereagzi.keycloak.eventforwarder.sink.EventSink;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jboss.logging.Logger;

public final class KafkaEventSink implements EventSink {

  private static final Logger LOG = Logger.getLogger(KafkaEventSink.class);

  private final ForwarderConfig config;
  private final Producer<String, String> producer;

  public KafkaEventSink(ForwarderConfig config) {
    this(config, new KafkaProducer<>(kafkaProperties(config)));
  }

  public KafkaEventSink(ForwarderConfig config, Producer<String, String> producer) {
    this.config = config;
    this.producer = producer;
  }

  @Override
  public String name() {
    return "kafka";
  }

  @Override
  public void send(String key, String payloadJson) {
    ProducerRecord<String, String> record = new ProducerRecord<>(config.kafkaTopic, key, payloadJson);
    try {
      producer.send(record).get(config.kafkaDeliveryTimeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (Exception ex) {
      throw new RuntimeException("Kafka sink send failed", ex);
    }
  }

  @Override
  public void close() {
    producer.flush();
    producer.close();
  }

  private static Properties kafkaProperties(ForwarderConfig config) {
    Properties properties = new Properties();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.kafkaBootstrapServers);
    properties.put(ProducerConfig.CLIENT_ID_CONFIG, config.kafkaClientId);
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.ACKS_CONFIG, config.kafkaAcks);
    properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, config.kafkaCompressionType);
    properties.put(ProducerConfig.LINGER_MS_CONFIG, Long.toString(config.kafkaLinger.toMillis()));
    properties.put(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(config.kafkaBatchSize));
    properties.put(
        ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, Long.toString(config.kafkaDeliveryTimeout.toMillis()));
    properties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, Integer.toString(config.kafkaMaxInFlight));
    properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, Boolean.toString(config.kafkaEnableIdempotence));

    if (config.kafkaSecurityProtocol != null && !config.kafkaSecurityProtocol.isBlank()) {
      properties.put("security.protocol", config.kafkaSecurityProtocol);
    }
    if (config.kafkaSaslMechanism != null && !config.kafkaSaslMechanism.isBlank()) {
      properties.put("sasl.mechanism", config.kafkaSaslMechanism);
    }
    if (config.kafkaSaslJaasConfig != null && !config.kafkaSaslJaasConfig.isBlank()) {
      properties.put("sasl.jaas.config", config.kafkaSaslJaasConfig);
    }
    properties.putAll(config.kafkaExtraProperties);

    LOG.infov("Kafka sink configured for topic {0}", config.kafkaTopic);
    return properties;
  }
}
