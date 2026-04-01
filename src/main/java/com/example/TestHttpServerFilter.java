package com.example;

import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.server.netty.NettyHttpRequest;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Filter(Filter.MATCH_ALL_PATTERN)
public class TestHttpServerFilter implements HttpServerFilter {

    private static final Logger log = LoggerFactory.getLogger(TestHttpServerFilter.class);

    public static String REQUEST_ID_HEADER = "X-Request-Id";

    private final Map<UUID, Object> requestBodyTracker = new HashMap<>();

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        assert request instanceof NettyHttpRequest<?>: "Expected request to be an instance of NettyHttpRequest";
        final NettyHttpRequest<?> nettyHttpRequest = (NettyHttpRequest<?>) request;

        persistRequestBody(nettyHttpRequest);
        return chain.proceed(nettyHttpRequest);
    }

    public Optional<Object> getRequestBody(final UUID requestId) {
        return Optional.ofNullable(requestBodyTracker.get(requestId));
    }

    private void persistRequestBody(final HttpRequest<?> request) {
        final UUID requestId = extractRequestId(request);
        final Object requestBody = request
                .getBody()
                .orElseGet(() -> {
                    log.warn("No request body found for {}", requestId);
                    return null;
                });

        log.info("[{}] Body: {}", requestId, requestBody);
        requestBodyTracker.put(requestId, requestBody);
    }

    private UUID extractRequestId(final HttpRequest<?> request) {
        return Optional.ofNullable(request)
                .map(HttpRequest::getHeaders)
                .map(headers -> headers.get(REQUEST_ID_HEADER))
                .map(requestIdString -> {
                    try {
                        return UUID.fromString(requestIdString);
                    } catch (IllegalArgumentException e) {
                        log.info("Invalid UUID string in {} header: {}", REQUEST_ID_HEADER, requestIdString);
                        return null;
                    }
                })
                .orElseThrow(() -> new HttpStatusException(HttpStatus.BAD_REQUEST, "Missing or invalid " + REQUEST_ID_HEADER + " header"));
    }
}
