package com.github.edereagzi.keycloak.eventforwarder.dispatch;

import com.github.edereagzi.keycloak.eventforwarder.config.ForwarderConfig;
import com.github.edereagzi.keycloak.eventforwarder.sink.EventSink;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.jboss.logging.Logger;

public final class EventDispatcher implements AutoCloseable {

  private static final Logger LOG = Logger.getLogger(EventDispatcher.class);

  private final ForwarderConfig config;
  private final List<EventSink> sinks;
  private final BlockingQueue<EventDispatchItem> queue;
  private final ThreadPoolExecutor executor;
  private final ObjectMapper objectMapper;
  private final AtomicLong droppedEvents;
  private volatile boolean closing;
  private final AtomicLong threadCounter;

  public EventDispatcher(ForwarderConfig config, List<EventSink> sinks) {
    this.config = config;
    this.sinks = sinks;
    this.queue = new LinkedBlockingQueue<>(config.queueCapacity);
    this.objectMapper = new ObjectMapper();
    this.droppedEvents = new AtomicLong(0);
    this.threadCounter = new AtomicLong(0);

    ThreadFactory threadFactory =
        r -> {
          Thread thread = new Thread(r, "kef-dispatcher-" + threadCounter.getAndIncrement());
          thread.setDaemon(true);
          return thread;
        };

    this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(config.workerThreads, threadFactory);
    for (int i = 0; i < config.workerThreads; i++) {
      this.executor.submit(this::workerLoop);
    }
  }

  public void dispatch(String key, Map<String, Object> payload) {
    if (closing) {
      return;
    }

    try {
      boolean enqueued = queue.offer(new EventDispatchItem(key, payload), config.enqueueTimeout.toMillis(), TimeUnit.MILLISECONDS);
      if (!enqueued) {
        long drops = droppedEvents.incrementAndGet();
        if (drops == 1 || drops % 100 == 0) {
          LOG.warnf("Dropping events because dispatch queue is full. dropped=%d", drops);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOG.warn("Interrupted while enqueueing event", e);
    }
  }

  private void workerLoop() {
    while (!closing || !queue.isEmpty()) {
      try {
        EventDispatchItem item = queue.poll(500, TimeUnit.MILLISECONDS);
        if (item == null) {
          continue;
        }

        String json = toJson(item.payload());
        if (json == null) {
          continue;
        }

        for (EventSink sink : sinks) {
          try {
            sink.send(item.key(), json);
          } catch (RuntimeException ex) {
            LOG.warnf(ex, "Sink '%s' failed to process event", sink.name());
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  private String toJson(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      LOG.error("Failed to serialize event payload", ex);
      return null;
    }
  }

  @Override
  public void close() {
    closing = true;
    executor.shutdown();
    try {
      if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      executor.shutdownNow();
    }

    for (EventSink sink : sinks) {
      try {
        sink.close();
      } catch (Exception ex) {
        LOG.warnf(ex, "Failed to close sink '%s'", sink.name());
      }
    }
  }
}
