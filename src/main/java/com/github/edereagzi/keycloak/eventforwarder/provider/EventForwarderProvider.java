package com.github.edereagzi.keycloak.eventforwarder.provider;

import com.github.edereagzi.keycloak.eventforwarder.config.ForwarderConfig;
import com.github.edereagzi.keycloak.eventforwarder.dispatch.EventDispatcher;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

final class EventForwarderProvider implements EventListenerProvider {

  private final KeycloakSession session;
  private final ForwarderConfig config;
  private final EventDispatcher dispatcher;

  EventForwarderProvider(KeycloakSession session, ForwarderConfig config, EventDispatcher dispatcher) {
    this.session = session;
    this.config = config;
    this.dispatcher = dispatcher;
  }

  @Override
  public void onEvent(Event event) {
    if (!config.sendUserEvents || !config.matchesUserEvent(event.getType())) {
      return;
    }

    Map<String, Object> payload = new LinkedHashMap<>();
    RealmModel realm = session.realms().getRealm(event.getRealmId());
    payload.put("eventId", event.getId());
    payload.put("source", "USER_EVENT");
    payload.put("timestamp", Instant.ofEpochMilli(event.getTime()).toString());
    payload.put("realmId", event.getRealmId());
    payload.put("realmName", realm != null ? realm.getName() : null);
    payload.put("type", event.getType().name());
    payload.put("clientId", event.getClientId());
    payload.put("userId", event.getUserId());
    payload.put("sessionId", event.getSessionId());
    payload.put("ipAddress", event.getIpAddress());
    payload.put("error", event.getError());
    payload.put("details", event.getDetails());

    dispatcher.dispatch(event.getRealmId(), payload);
  }

  @Override
  public void onEvent(AdminEvent event, boolean includeRepresentation) {
    if (!config.sendAdminEvents) {
      return;
    }

    String operationType = event.getOperationType() != null ? event.getOperationType().name() : "UNKNOWN";
    if (!config.matchesAdminOperation(operationType)) {
      return;
    }

    String resourceType = event.getResourceTypeAsString() != null ? event.getResourceTypeAsString() : "UNKNOWN";
    if (!config.matchesAdminResource(resourceType)) {
      return;
    }

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("eventId", event.getId());
    payload.put("source", "ADMIN_EVENT");
    payload.put("timestamp", Instant.ofEpochMilli(event.getTime()).toString());
    payload.put("realmId", event.getRealmId());
    payload.put("operationType", operationType);
    payload.put("resourceType", resourceType);
    payload.put("resourcePath", event.getResourcePath());
    payload.put("representationIncluded", includeRepresentation || config.includeRepresentation);
    payload.put(
        "representation",
        (includeRepresentation || config.includeRepresentation) ? event.getRepresentation() : null);

    if (event.getAuthDetails() != null) {
      Map<String, Object> auth = new LinkedHashMap<>();
      auth.put("clientId", event.getAuthDetails().getClientId());
      auth.put("userId", event.getAuthDetails().getUserId());
      auth.put("ipAddress", event.getAuthDetails().getIpAddress());
      auth.put("realmId", event.getAuthDetails().getRealmId());
      payload.put("auth", auth);
    }

    payload.put("error", event.getError());

    dispatcher.dispatch(event.getRealmId(), payload);
  }

  @Override
  public void close() {
    // lifecycle handled by factory
  }
}
