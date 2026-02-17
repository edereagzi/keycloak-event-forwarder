package com.github.edereagzi.keycloak.eventforwarder.dispatch;

import java.util.Map;

record EventDispatchItem(String key, Map<String, Object> payload) {}
