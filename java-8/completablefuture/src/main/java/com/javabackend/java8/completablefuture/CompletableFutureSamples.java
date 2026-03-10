package com.javabackend.java8.completablefuture;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 *
 * CompletableFuture
 *
 * CompletableFuture = Future + completion stage pipeline
 *
 * Future (Java 5)             CompletableFuture (Java 8)
 * ─────────────────────       ──────────────────────────────
 * get() blocks thread         non-blocking pipeline
 * no chaining                 thenApply / thenCompose / thenAccept
 * no exception handling       exceptionally / handle / whenComplete
 * no combining                allOf / anyOf
 * manual completion only      supplyAsync / runAsync
 *
 * THREAD POOLS
 *   Default: ForkJoinPool.commonPool()
 *   Custom:  pass Executor as second argument to async methods
 *
 */
public class CompletableFutureSamples {

    // Dedicated thread pool — don't use commonPool for I/O bound tasks
    static final ExecutorService POOL =
            Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r);
                t.setDaemon(true); // don't block JVM shutdown
                return t;
            });

    public static void main(String[] args) throws Exception {
        System.out.println("━━━ EXAMPLE 1 — Creating CompletableFutures ━━━\n");
        creating();

        System.out.println("\n━━━ EXAMPLE 2 — Transforming Results ━━━\n");
        transforming();

        System.out.println("\n━━━ EXAMPLE 3 — Combining Futures ━━━\n");
        combining();

        System.out.println("\n━━━ EXAMPLE 4 — Exception Handling ━━━\n");
        exceptionHandling();

        System.out.println("\n━━━ EXAMPLE 5 — Senior Level ━━━\n");
        seniorLevel();

        POOL.shutdown();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — Creating
    // ─────────────────────────────────────────────
    static void creating() throws Exception {

        // ── runAsync — no return value ──
        CompletableFuture<Void> run = CompletableFuture.runAsync(
                () -> System.out.println("  runAsync: " +
                        Thread.currentThread().getName()), POOL);
        run.get(); // wait for completion

        // ── supplyAsync — returns a value ──
        CompletableFuture<String> supply = CompletableFuture.supplyAsync(
                () -> {
                    sleep(100);
                    return "result from async task";
                }, POOL);
        System.out.println("  supplyAsync: " + supply.get());

        // ── completedFuture — already-done future ──
        // Useful in tests or when result is immediately available
        CompletableFuture<String> done = CompletableFuture.completedFuture("immediate");
        System.out.println("  completedFuture: " + done.get());

        // ── Manual completion ──
        CompletableFuture<String> manual = new CompletableFuture<>();
        POOL.submit(() -> {
            sleep(50);
            manual.complete("manually completed");
        });
        System.out.println("  manual: " + manual.get());

        // ── failedFuture (Java 9+) ──
        CompletableFuture<String> failed = CompletableFuture.failedFuture(
                new RuntimeException("already failed"));
        System.out.println("  failedFuture isDone: " + failed.isDone());
        System.out.println("  failedFuture isCompletedExceptionally: "
                + failed.isCompletedExceptionally());
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — Transforming Results
    // ─────────────────────────────────────────────
    static void transforming() throws Exception {

        // ── thenApply — transform result (Function) ──
        // Runs in same thread as previous stage (or calling thread if already done)
        String result = CompletableFuture
                .supplyAsync(() -> "hello", POOL)
                .thenApply(String::toUpperCase)         // HELLO
                .thenApply(s -> s + " WORLD")           // HELLO WORLD
                .get();
        System.out.println("  thenApply: " + result);

        // ── thenApplyAsync — transform on different thread ──
        String asyncResult = CompletableFuture
                .supplyAsync(() -> fetchUser("U1"), POOL)
                .thenApplyAsync(user -> enrichUser(user), POOL) // different thread
                .get();
        System.out.println("  thenApplyAsync: " + asyncResult);

        // ── thenAccept — consume result, returns CompletableFuture<Void> ──
        CompletableFuture
                .supplyAsync(() -> "Alice", POOL)
                .thenAccept(name -> System.out.println("  thenAccept: Hello " + name))
                .get();

        // ── thenRun — run after completion, ignores result ──
        CompletableFuture
                .supplyAsync(() -> saveToDb("data"), POOL)
                .thenRun(() -> System.out.println("  thenRun: save complete, sending event"))
                .get();

        // ── thenCompose — chain dependent futures (flatMap for CF) ──
        // thenApply would give CompletableFuture<CompletableFuture<User>>
        // thenCompose flattens it to CompletableFuture<User>
        String composed = CompletableFuture
                .supplyAsync(() -> "U1", POOL)
                .thenCompose(id -> CompletableFuture.supplyAsync(
                        () -> fetchUser(id), POOL))         // returns CF — compose flattens
                .thenCompose(user -> CompletableFuture.supplyAsync(
                        () -> enrichUser(user), POOL))
                .get();
        System.out.println("  thenCompose: " + composed);
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — Combining Futures
    // ─────────────────────────────────────────────
    static void combining() throws Exception {

        // ── thenCombine — combine two independent futures ──
        CompletableFuture<String> userFuture  =
                CompletableFuture.supplyAsync(() -> fetchUser("U1"), POOL);
        CompletableFuture<String> orderFuture =
                CompletableFuture.supplyAsync(() -> fetchOrders("U1"), POOL);

        String combined = userFuture.thenCombine(orderFuture,
                (user, orders) -> user + " | " + orders).get();
        System.out.println("  thenCombine: " + combined);

        // ── allOf — wait for ALL to complete ──
        // allOf returns CompletableFuture<Void> — results must be fetched individually
        CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
            sleep(100); return "service-A";
        }, POOL);
        CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> {
            sleep(150); return "service-B";
        }, POOL);
        CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> {
            sleep(80);  return "service-C";
        }, POOL);

        long start = System.currentTimeMillis();
        CompletableFuture.allOf(f1, f2, f3).get(); // waits for all — ~150ms not 330ms
        long elapsed = System.currentTimeMillis() - start;

        List<String> results = List.of(f1.get(), f2.get(), f3.get());
        System.out.println("  allOf results : " + results);
        System.out.println("  allOf elapsed : " + elapsed + "ms (parallel, not sequential)");

        // ── anyOf — first to complete wins ──
        CompletableFuture<String> fast  = CompletableFuture.supplyAsync(() -> {
            sleep(50); return "fast";
        }, POOL);
        CompletableFuture<String> slow  = CompletableFuture.supplyAsync(() -> {
            sleep(500); return "slow";
        }, POOL);

        Object first = CompletableFuture.anyOf(fast, slow).get();
        System.out.println("  anyOf winner  : " + first); // always "fast"

        // ── Collecting results from list of futures ──
        List<String> userIds = List.of("U1", "U2", "U3");
        List<CompletableFuture<String>> futures = userIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> fetchUser(id), POOL))
                .toList();

        List<String> users = CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join) // join() = get() without checked exception
                        .collect(Collectors.toList()))
                .get();
        System.out.println("  all users     : " + users);
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — Exception Handling
    // ─────────────────────────────────────────────
    static void exceptionHandling() throws Exception {

        // ── exceptionally — recover from failure ──
        String result = CompletableFuture
                .supplyAsync(() -> {
                    if (true) throw new RuntimeException("fetch failed");
                    return "data";
                }, POOL)
                .exceptionally(ex -> {
                    System.out.println("  exceptionally: " + ex.getMessage());
                    return "fallback data"; // recovery value
                })
                .get();
        System.out.println("  exceptionally result: " + result);

        // ── handle — runs always (success or failure) ──
        // Like exceptionally but also receives the result on success
        String handled = CompletableFuture
                .supplyAsync(() -> "success value", POOL)
                .handle((value, ex) -> {
                    if (ex != null) {
                        System.out.println("  handle error: " + ex.getMessage());
                        return "fallback";
                    }
                    return value.toUpperCase(); // transform on success
                })
                .get();
        System.out.println("  handle result: " + handled);

        // ── whenComplete — side effect, does NOT transform result ──
        String whenResult = CompletableFuture
                .supplyAsync(() -> "original", POOL)
                .whenComplete((value, ex) -> {
                    // Logging, metrics — cannot change the result
                    System.out.println("  whenComplete: value=" + value + " ex=" + ex);
                })
                .get();
        System.out.println("  whenComplete result: " + whenResult); // still "original"

        // ── Timeout (Java 9+) ──
        CompletableFuture<String> timeout = CompletableFuture
                .supplyAsync(() -> { sleep(2000); return "too slow"; }, POOL)
                .orTimeout(500, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> "timed out: " + ex.getClass().getSimpleName());
        System.out.println("  timeout: " + timeout.get());

        // ── completeOnTimeout (Java 9+) — use default instead of throwing ──
        String withDefault = CompletableFuture
                .supplyAsync(() -> { sleep(2000); return "too slow"; }, POOL)
                .completeOnTimeout("default value", 300, TimeUnit.MILLISECONDS)
                .get();
        System.out.println("  completeOnTimeout: " + withDefault);
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — Senior Level
    //   Parallel API calls, async service layer
    // ─────────────────────────────────────────────
    static void seniorLevel() throws Exception {

        // ── Pattern 1: Parallel independent service calls ──
        // Fetch user + orders + recommendations in parallel
        // Total time = max(individual times), not sum
        System.out.println("── Parallel service calls ──");
        long start = System.currentTimeMillis();

        CompletableFuture<String> userCF    = CompletableFuture
                .supplyAsync(() -> { sleep(100); return "User:Alice"; }, POOL);
        CompletableFuture<String> ordersCF  = CompletableFuture
                .supplyAsync(() -> { sleep(150); return "Orders:[O1,O2]"; }, POOL);
        CompletableFuture<String> recCF     = CompletableFuture
                .supplyAsync(() -> { sleep(120); return "Recs:[P1,P2,P3]"; }, POOL);

        String dashboard = CompletableFuture
                .allOf(userCF, ordersCF, recCF)
                .thenApply(v -> userCF.join() + " | " + ordersCF.join() + " | " + recCF.join())
                .get();

        System.out.println("  result  : " + dashboard);
        System.out.println("  elapsed : " + (System.currentTimeMillis()-start) + "ms (~150ms not 370ms)");

        // ── Pattern 2: Sequential dependent calls ──
        // login → fetch profile → fetch permissions (each depends on previous)
        System.out.println("── Sequential dependent calls ──");
        String session = CompletableFuture
                .supplyAsync(() -> login("alice", "pass"), POOL)        // step 1
                .thenCompose(token ->
                        CompletableFuture.supplyAsync(
                                () -> fetchProfile(token), POOL))               // step 2
                .thenCompose(profile ->
                        CompletableFuture.supplyAsync(
                                () -> fetchPermissions(profile), POOL))         // step 3
                .get();
        System.out.println("  session : " + session);

        // ── Pattern 3: Fan-out with timeout + fallback ──
        // Call multiple services, take first result, fall back if all timeout
        System.out.println("── Fan-out with fallback ──");
        List<CompletableFuture<String>> sources = List.of(
                CompletableFuture.supplyAsync(() -> { sleep(300); return "primary"; },   POOL),
                CompletableFuture.supplyAsync(() -> { sleep(100); return "secondary"; }, POOL),
                CompletableFuture.supplyAsync(() -> { sleep(500); return "tertiary"; },  POOL)
        );

        String fanOut = CompletableFuture
                .anyOf(sources.toArray(new CompletableFuture[0]))
                .thenApply(Object::toString)
                .orTimeout(200, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> "all sources failed")
                .get();
        System.out.println("  fan-out : " + fanOut); // "secondary" wins
    }

    // ── Simulated services ──
    static String fetchUser(String id) {
        sleep(50);
        return "User(" + id + ")";
    }
    static String fetchOrders(String id) {
        sleep(60);
        return "Orders(" + id + ")";
    }
    static String enrichUser(String user) {
        sleep(30);
        return user + "+enriched";
    }
    static String saveToDb(String data) {
        sleep(40);
        return "saved:" + data;
    }
    static String login(String u, String p) {
        sleep(50); return "token-xyz";
    }
    static String fetchProfile(String token) {
        sleep(50); return "profile-alice";
    }
    static String fetchPermissions(String profile) {
        sleep(50); return profile + "+permissions[READ,WRITE]";
    }
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); }
    }

}