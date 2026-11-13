package com.partidoya.platform.support;

import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

/** Guarda los eventos publicados para verificar qué anunció un servicio. */
public final class RecordingEventPublisher implements ApplicationEventPublisher {
    private final List<Object> events = new ArrayList<>();

    @Override
    public void publishEvent(Object event) {
        events.add(event);
    }

    public <T> List<T> eventsOfType(Class<T> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
