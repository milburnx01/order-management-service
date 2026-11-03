package com.example.orderservice.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StoredTraceContext {

    private final Tracer tracer;
    private final Propagator propagator;
    private final ObjectMapper objectMapper;

    public String capture() {
        Span span = tracer.currentSpan();
        if (span == null) {
            return null;
        }
        Map<String, String> headers = new HashMap<>();
        propagator.inject(span.context(), headers, Map::put);
        return objectMapper.writeValueAsString(headers);
    }

    public void run(String storedContext, String operation, Runnable action) {
        Map<String, String> headers = new HashMap<>();

        if (storedContext != null && !storedContext.isBlank()) {
            objectMapper.readTree(storedContext)
                    .properties()
                    .forEach(entry ->
                            headers.put(entry.getKey(), entry.getValue().asText()));
        }

        Span span = propagator.extract(headers, Map::get)
                .name(operation)
                .start();

        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            action.run();
        } catch (RuntimeException | Error ex) {
            span.error(ex);
            throw ex;
        } finally {
            span.end();
        }
    }
}