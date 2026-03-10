# 🎯 Interview Questions — CompletableFuture

---

**Q1. What problem does `CompletableFuture` solve that `Future` does not?**

> `Future` (Java 5) has three major limitations:
>
> 1. **Blocking** — `future.get()` blocks the calling thread until done. No way
     >    to attach a callback and be notified on completion.
>
> 2. **No chaining** — cannot compose multiple async operations. You must block
     >    after each step to get the result before starting the next.
>
> 3. **No exception handling** — no built-in way to recover from failure in the
     >    pipeline. Exceptions surface only when `get()` is called.
>
> `CompletableFuture` solves all three:
> ```
> // Future — blocks at every step
> String user   = fetchUserFuture.get();   // blocks
> String orders = fetchOrdersFuture(user).get(); // blocks again
>
> // CompletableFuture — non-blocking pipeline
> CompletableFuture
>     .supplyAsync(() -> fetchUser())
>     .thenCompose(user -> fetchOrdersAsync(user))
>     .thenAccept(orders -> display(orders))
>     .exceptionally(ex -> { log(ex); return null; });
> // Main thread free to do other work
> ```

---

**Q2. What is the difference between `thenApply()` and `thenCompose()`?**

> `thenApply(fn)` — the function returns a plain value. Result is wrapped in CF:
> ```
> // fetchUser returns String → result is CF<String>
> CF<String> cf = cf.thenApply(id -> fetchUser(id));
> ```
>
> `thenCompose(fn)` — the function returns a `CompletableFuture`. Result is
> flattened — avoids `CF<CF<String>>`:
> ```
> // fetchUserAsync returns CF<String>
> CF<CF<String>> bad  = cf.thenApply(id -> fetchUserAsync(id));   // nested!
> CF<String>     good = cf.thenCompose(id -> fetchUserAsync(id)); // flat ✅
> ```
>
> The relationship mirrors `map` vs `flatMap` in streams — use `thenCompose`
> whenever the next step is itself an async operation that returns a
> `CompletableFuture`.

---

**Q3. What is the difference between `exceptionally()`, `handle()`, and `whenComplete()`?**

> All three deal with pipeline completion but serve different purposes:
>
> `exceptionally(fn)` — recovery only. Runs only on failure. Returns a fallback value:
> ```
> .exceptionally(ex -> "fallback") // skipped on success
> ```
>
> `handle(fn)` — runs always (success or failure). Receives both the value and
> the exception. Can transform the result:
> ```
> .handle((value, ex) -> {
>     if (ex != null) return "fallback";
>     return value.toUpperCase(); // transform on success
> })
> ```
>
> `whenComplete(fn)` — side effect only. Runs always but cannot change the result.
> Used for logging, metrics, cleanup:
> ```
> .whenComplete((value, ex) -> logger.info("done: " + value))
> // result passes through unchanged
> ```
>
> Choosing: use `exceptionally` for simple recovery, `handle` when you need
> to transform both success and failure paths, `whenComplete` for side effects
> like logging that should never affect the result.

---

**Q4. What is the difference between `allOf()` and `anyOf()`?**

> `allOf(cf1, cf2, cf3)` — waits for ALL futures to complete. Returns
> `CF<Void>` — results must be fetched individually from original futures:
> ```
> CompletableFuture.allOf(f1, f2, f3)
>     .thenApply(v -> List.of(f1.join(), f2.join(), f3.join()))
>     .get();
> // Total time = max(f1, f2, f3) — parallel, not sequential
> ```
>
> `anyOf(cf1, cf2, cf3)` — completes when the FIRST future completes. Returns
> `CF<Object>` with the first result:
> ```
> Object first = CompletableFuture.anyOf(fast, slow, medium).get();
> // Returns result of whichever finishes first
> ```
>
> Production use cases:
> - `allOf` — fetch user + orders + recommendations in parallel, render dashboard
    >   when all are ready
> - `anyOf` — fan-out to multiple data sources (primary DB, replica, cache),
    >   take whichever responds first

---

**Q5. Why should you use a custom `Executor` instead of the default `ForkJoinPool.commonPool()`?**

> `commonPool` is shared across the entire JVM — parallel streams,
> CompletableFuture, and any other ForkJoin task all compete for the same
> threads. In a web server this causes two problems:
>
> 1. **I/O-bound tasks block CPU threads** — `commonPool` threads are designed
     >    for CPU work. A DB call or HTTP request that blocks for 200ms ties up a
     >    thread that could be doing real work.
>
> 2. **Starvation** — a burst of parallel stream usage can exhaust `commonPool`,
     >    causing CompletableFuture tasks to queue — adding latency to unrelated requests.
>
> ```
> // BAD — I/O on commonPool
> CompletableFuture.supplyAsync(() -> dbQuery()); // blocks commonPool thread
>
> // GOOD — dedicated I/O pool
> ExecutorService ioPool = Executors.newFixedThreadPool(20);
> CompletableFuture.supplyAsync(() -> dbQuery(), ioPool);
> ```
>
> In Spring Boot: inject `@Async` with a `ThreadPoolTaskExecutor` configured
> for your workload. For I/O-bound: more threads (20-100). For CPU-bound:
> threads ≈ CPU cores.

---

**Q6. What is the difference between `get()` and `join()`?**

> Both block until the future completes and return the result. The difference
> is in exception handling:
>
> `get()` — throws checked exceptions: `InterruptedException` and `ExecutionException`.
> Must be caught or declared:
> ```
> try {
>     String result = cf.get();
> } catch (InterruptedException | ExecutionException e) { ... }
> ```
>
> `join()` — throws unchecked `CompletionException`. No try-catch required:
> ```
> String result = cf.join(); // CompletionException wraps the original
> ```
>
> Practical rule:
> - Use `get()` at the boundary of async code — where you must handle the exception
> - Use `join()` inside stream lambdas — `get()` cannot be used there because
    >   checked exceptions are not allowed in lambdas without wrapping:
> ```
> // join() is required here — get() would need try-catch
> futures.stream()
>     .map(CompletableFuture::join)
>     .collect(Collectors.toList());
> ```

---

**Q7. How do you implement a timeout for a `CompletableFuture`?**

> Java 9 introduced two timeout methods:
>
> `orTimeout(n, unit)` — completes exceptionally with `TimeoutException` after n:
> ```
> CompletableFuture
>     .supplyAsync(() -> slowService())
>     .orTimeout(500, TimeUnit.MILLISECONDS)
>     .exceptionally(ex -> "timed out");
> ```
>
> `completeOnTimeout(value, n, unit)` — completes normally with a default value:
> ```
> String result = CompletableFuture
>     .supplyAsync(() -> slowService())
>     .completeOnTimeout("default", 500, TimeUnit.MILLISECONDS)
>     .get();
> ```
>
> For Java 8 (no built-in timeout), use a `ScheduledExecutorService`:
> ```
> ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
> CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> slowService());
> scheduler.schedule(
>     () -> cf.completeExceptionally(new TimeoutException()),
>     500, TimeUnit.MILLISECONDS);
> ```

---

**Q8. How do you run three independent API calls in parallel and combine their results?**

> ```
> ExecutorService pool = Executors.newFixedThreadPool(3);
>
> CompletableFuture<User>   userCF   = CompletableFuture.supplyAsync(() -> fetchUser(id),   pool);
> CompletableFuture<Orders> ordersCF = CompletableFuture.supplyAsync(() -> fetchOrders(id), pool);
> CompletableFuture<Recs>   recsCF   = CompletableFuture.supplyAsync(() -> fetchRecs(id),   pool);
>
> Dashboard dashboard = CompletableFuture
>     .allOf(userCF, ordersCF, recsCF)
>     .thenApply(v -> new Dashboard(
>         userCF.join(),
>         ordersCF.join(),
>         recsCF.join()))
>     .get();
> ```
>
> Total time = max(fetchUser, fetchOrders, fetchRecs) — roughly the slowest
> call, not the sum. For three 100ms calls this is ~100ms instead of 300ms.
>
> This pattern is the primary reason to use CompletableFuture in a service
> layer — any time a request requires data from multiple independent sources,
> parallelising the calls cuts latency to the slowest call.

---

**Q9. What happens if an exception occurs in the middle of a pipeline and there is no exception handler?**

> The exception propagates through all subsequent stages — each stage
> completes exceptionally and skips its function. The final `get()` or `join()`
> throws the exception wrapped in `ExecutionException` (for `get()`) or
> `CompletionException` (for `join()`):
>
> ```
> CompletableFuture
>     .supplyAsync(() -> { throw new RuntimeException("step 1 failed"); })
>     .thenApply(v -> v.toUpperCase())    // skipped — previous stage failed
>     .thenAccept(v -> save(v))           // skipped
>     .get();                             // throws ExecutionException
> ```
>
> This means unhandled exceptions are silent until `get()` is called —
> which may be much later, making the source hard to find.
>
> Best practice: always attach an `exceptionally` or `handle` at the end
> of every pipeline, even if just for logging:
> ```
> .exceptionally(ex -> {
>     log.error("pipeline failed", ex);
>     return fallback;
> });
> ```

---

**Q10. How would you process a list of 100 user IDs asynchronously and collect all results?**

> ```
> ExecutorService pool = Executors.newFixedThreadPool(10);
>
> List<String> userIds = fetchAllIds(); // 100 IDs
>
> // Fan out — one CF per ID, all run concurrently
> List<CompletableFuture<User>> futures = userIds.stream()
>     .map(id -> CompletableFuture
>         .supplyAsync(() -> fetchUser(id), pool)
>         .exceptionally(ex -> User.fallback(id))) // per-future recovery
>     .collect(Collectors.toList());
>
> // Wait for all, collect results
> List<User> users = CompletableFuture
>     .allOf(futures.toArray(new CompletableFuture[0]))
>     .thenApply(v -> futures.stream()
>         .map(CompletableFuture::join)
>         .collect(Collectors.toList()))
>     .get();
>
> pool.shutdown();
> ```
>
> Key points:
> - Pool size controls max concurrency — 10 threads means 10 calls at a time
> - `exceptionally` per future means one failure doesn't abort all others
> - `allOf` + `thenApply` is the standard pattern for collecting from a list of futures
> - `join()` inside the lambda is correct — `get()` would need try-catch
>
> For truly large fan-outs (thousands of IDs), consider batching or using
> a reactive framework (Project Reactor, RxJava) instead.