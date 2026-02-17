package com.github.edereagzi.keycloak.eventforwarder.provider;

import com.github.edereagzi.keycloak.eventforwarder.config.ForwarderConfig;
import com.github.edereagzi.keycloak.eventforwarder.dispatch.EventDispatcher;
import com.github.edereagzi.keycloak.eventforwarder.sink.EventSink;
import com.github.edereagzi.keycloak.eventforwarder.sink.SinkFactory;
import java.util.List;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public final class EventForwarderProviderFactory implements EventListenerProviderFactory {

  public static final String PROVIDER_ID = "event-forwarder";

  private static final Logger LOG = Logger.getLogger(EventForwarderProviderFactory.class);

  private ForwarderConfig config;
  private EventDispatcher dispatcher;

  @Override
  public EventListenerProvider create(KeycloakSession session) {
    return new EventForwarderProvider(session, config, dispatcher);
  }

  @Override
  public void init(Config.Scope scope) {
    this.config = ForwarderConfig.from(scope);

    try {
      List<EventSink> sinks = SinkFactory.createSinks(config);
      this.dispatcher = new EventDispatcher(config, sinks);
      LOG.infov(
          "event-forwarder initialized sinkType={0} workers={1} queueCapacity={2}",
          config.sinkType,
          config.workerThreads,
          config.queueCapacity);
    } catch (RuntimeException ex) {
      LOG.error("Failed to initialize event-forwarder. Plugin will remain inactive.", ex);
      throw ex;
    }
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
    // no-op
  }

  @Override
  public void close() {
    if (dispatcher != null) {
      dispatcher.close();
    }
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }
}
