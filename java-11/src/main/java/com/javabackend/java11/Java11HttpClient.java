package com.javabackend.java11;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

/**
 *
 * Java 11 — HttpClient
 *
 * Java 11 made HttpClient stable (was incubating in Java 9/10)
 * Replaces HttpURLConnection — modern, fluent, async-capable
 *
 * KEY FEATURES
 *  - Fluent builder API
 *  - Sync and async request execution
 *  - HTTP/1.1 and HTTP/2 support
 *  - WebSocket support
 *  - Built-in timeout handling
 *  - Immutable and thread-safe
 *
 * NOTE: These examples use httpbin.org for live HTTP calls.
 *       If no internet, examples show the API structure with
 *       expected outputs commented.
 *
 */
public class Java11HttpClient {

    // ── Shared client — reuse, don't create per request ──
    // Thread-safe, manages connection pool internally
    static final HttpClient CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)          // prefer HTTP/2, fallback to HTTP/1.1
            .connectTimeout(Duration.ofSeconds(5))        // connection timeout
            .followRedirects(HttpClient.Redirect.NORMAL)  // follow redirects
            .build();

    public static void main(String[] args) throws Exception {
        System.out.println("━━━ EXAMPLE 1 — Synchronous GET ━━━\n");
        synchronousGet();

        System.out.println("\n━━━ EXAMPLE 2 — POST with body ━━━\n");
        postWithBody();

        System.out.println("\n━━━ EXAMPLE 3 — Async GET ━━━\n");
        asyncGet();

        System.out.println("\n━━━ EXAMPLE 4 — Multiple async requests ━━━\n");
        multipleAsync();

        System.out.println("\n━━━ EXAMPLE 5 — Headers and timeout ━━━\n");
        headersAndTimeout();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — Synchronous GET
    // ─────────────────────────────────────────────
    static void synchronousGet() throws IOException, InterruptedException {
        // ── Build request ──
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://httpbin.org/get"))
                .GET() // default, can be omitted
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .build();

        // ── Send and receive — blocks until response ──
        HttpResponse<String> response = CLIENT.send(
                request,
                BodyHandlers.ofString() // response body as String
        );

        System.out.println("  status code : " + response.statusCode());
        System.out.println("  http version: " + response.version());
        System.out.println("  body length : " + response.body().length() + " chars");
        System.out.println("  content-type: " +
                response.headers().firstValue("content-type").orElse("none"));

        // ── BodyHandlers variants ──
        // BodyHandlers.ofString()       — body as String
        // BodyHandlers.ofBytes()        — body as byte[]
        // BodyHandlers.ofFile(path)     — stream directly to file
        // BodyHandlers.ofLines()        — body as Stream<String>
        // BodyHandlers.discarding()     — ignore body (for HEAD/status-only)
        System.out.println("  BodyHandlers: ofString, ofBytes, ofFile, ofLines, discarding");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — POST with body
    // ─────────────────────────────────────────────
    static void postWithBody() throws IOException, InterruptedException {
        // ── POST with JSON body ──
        String jsonBody = """
            {
              "name": "Alice",
              "role": "developer"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://httpbin.org/post"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .header("Accept",       "application/json")
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = CLIENT.send(request, BodyHandlers.ofString());
        System.out.println("  POST status : " + response.statusCode());

        // Extract just the json field from response to verify body was sent
        String body = response.body();
        int jsonStart = body.indexOf("\"json\":");
        if (jsonStart > 0) {
            System.out.println("  echoed body : (server received our JSON ✓)");
        }

        // ── BodyPublishers variants ──
        // BodyPublishers.ofString(s)         — send String
        // BodyPublishers.ofByteArray(bytes)  — send byte[]
        // BodyPublishers.ofFile(path)        — send file contents
        // BodyPublishers.noBody()            — no body (GET/HEAD)
        System.out.println("  BodyPublishers: ofString, ofByteArray, ofFile, noBody");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — Async GET
    // sendAsync returns CompletableFuture — non-blocking
    // ─────────────────────────────────────────────
    static void asyncGet() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://httpbin.org/delay/1")) // server delays 1s
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        System.out.println("  sending async — not blocking...");

        // sendAsync — returns immediately, HTTP call happens on background thread
        CompletableFuture<HttpResponse<String>> future = CLIENT.sendAsync(
                request,
                BodyHandlers.ofString()
        );

        // Chain processing — runs when response arrives
        CompletableFuture<String> result = future
                .thenApply(HttpResponse::statusCode)    // extract status
                .thenApply(status -> "status=" + status)
                .exceptionally(ex -> "failed: " + ex.getMessage());

        // Can do other work here while request is in flight
        System.out.println("  doing other work while waiting...");

        System.out.println("  async result: " + result.get(15, TimeUnit.SECONDS));
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — Multiple async requests in parallel
    // ─────────────────────────────────────────────
    static void multipleAsync() throws Exception {
        // Fire 3 requests simultaneously — total time ≈ slowest, not sum
        List<String> urls = List.of(
                "https://httpbin.org/get",
                "https://httpbin.org/ip",
                "https://httpbin.org/user-agent"
        );

        long start = System.currentTimeMillis();

        // Build all futures
        List<CompletableFuture<Integer>> futures = urls.stream()
                .map(url -> HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .build())
                .map(req -> CLIENT.sendAsync(req, BodyHandlers.ofString())
                        .thenApply(HttpResponse::statusCode))
                .toList();

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);

        long elapsed = System.currentTimeMillis() - start;
        System.out.print("  statuses: ");
        futures.forEach(f -> {
            try { System.out.print(f.get() + " "); }
            catch (Exception e) { System.out.print("err "); }
        });
        System.out.println();
        System.out.println("  elapsed : " + elapsed + "ms (parallel — not sequential)");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — Headers, timeout, error handling
    // ─────────────────────────────────────────────
    static void headersAndTimeout() throws IOException, InterruptedException {
        // ── Reading response headers ──
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://httpbin.org/response-headers?X-Custom=hello"))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = CLIENT.send(request, BodyHandlers.ofString());

        System.out.println("  all headers:");
        response.headers().map().forEach((name, values) ->
                System.out.println("    " + name + ": " + values));

        // ── Timeout handling ──
        HttpRequest slowRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://httpbin.org/delay/5")) // server delays 5s
                .timeout(Duration.ofSeconds(2))                  // but we only wait 2s
                .build();

        try {
            CLIENT.send(slowRequest, BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            System.out.println("  request timed out: HttpTimeoutException ✓");
        }

        // ── Status code based error handling ──
        HttpRequest notFound = HttpRequest.newBuilder()
                .uri(URI.create("https://httpbin.org/status/404"))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> r404 = CLIENT.send(notFound, BodyHandlers.ofString());
        if (r404.statusCode() >= 400) {
            System.out.println("  HTTP error: " + r404.statusCode());
            // HttpClient does NOT throw on 4xx/5xx — you must check statusCode()
            // This is different from some libraries (OkHttp, RestTemplate)
        }

        // ── HttpClient vs HttpURLConnection ──
        // HttpURLConnection:
        //   - Verbose, callback-based, no async
        //   - No HTTP/2, no WebSocket
        //   - Not fluent, hard to configure
        // HttpClient (Java 11):
        //   - Fluent builder, immutable, thread-safe
        //   - HTTP/1.1 + HTTP/2 + WebSocket
        //   - Sync (send) and async (sendAsync → CompletableFuture)
        //   - Built-in timeout, redirect, version negotiation
        System.out.println("  HttpClient: fluent, async, HTTP/2, thread-safe ✓");
    }

}