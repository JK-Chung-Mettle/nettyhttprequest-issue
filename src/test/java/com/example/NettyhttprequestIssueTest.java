package com.example;


import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class NettyhttprequestIssueTest {

    @Inject
    EmbeddedApplication<?> application;

    @Inject
    @Client("/")
    private HttpClient httpClient;

    @Inject
    private TestHttpServerFilter testHttpServerFilter;

    @Test
    void testApplicationRunning() {
        Assertions.assertTrue(application.isRunning());
    }

    @Test
    void canAccessGetBodyOfNettyHttpRequest() {
        final UUID requestId = UUID.randomUUID();
        final var requestBody = generateBody();

        runTest(httpClient -> {
            httpClient.exchange(HttpRequest
                    .POST(TestController.PATH, requestBody)
                    .header(TestHttpServerFilter.REQUEST_ID_HEADER, requestId.toString())
            );

            final var persistedRequestBody = testHttpServerFilter
                    .getRequestBody(requestId)
                    .orElseThrow(() -> new AssertionFailedError("Expected request body to be present for request id " + requestId));

            assertEquals(
                    requestBody,
                    persistedRequestBody,
                    """
                    Persisted request body does not match actual request body.
                    
                    Persisted Request Body:
                    %s
                    
                    Actual Request Body:
                    %s
                    """.formatted(persistedRequestBody, requestBody)
            );
        });
    }

    private Map<String, Object> generateBody() {
        return Map.of(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString()
        );
    }

    private void runTest(final Consumer<BlockingHttpClient> toRun) {
        try(final BlockingHttpClient blockingHttpClient = httpClient.toBlocking()) {
            toRun.accept(blockingHttpClient);
        }
    }

}
