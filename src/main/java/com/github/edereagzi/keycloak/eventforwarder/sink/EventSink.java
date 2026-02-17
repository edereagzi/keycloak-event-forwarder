package com.github.edereagzi.keycloak.eventforwarder.sink;

public interface EventSink extends AutoCloseable {

  String name();

  void send(String key, String payloadJson);

  @Override
  void close();
}
