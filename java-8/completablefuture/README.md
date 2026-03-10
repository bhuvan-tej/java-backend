# ⚡ CompletableFuture

> Async programming in Java 8+. A pipeline for non-blocking computations
> that can be chained, combined, and recovered from — without blocking threads.

---

## 🧠 Mental Model

```
Future (Java 5)                    CompletableFuture (Java 8)
────────────────────────           ──────────────────────────────────
future.get() → BLOCKS thread       non-blocking pipeline
no chaining                        thenApply → thenApply → thenAccept
no exception handling              exceptionally / handle / whenComplete
no combining                       allOf / anyOf / thenCombine
manual only                        supplyAsync / runAsync

Pipeline stages:
  supplyAsync(() -> fetchUser())       ← start async
    .thenApply(user -> enrich(user))   ← transform
    .thenCompose(user -> fetchOrders() ← chain dependent future
    .exceptionally(ex -> fallback)     ← recover
    .thenAccept(result -> save(result))← consume
```

---

## 📄 Classes in this Module

### `CompletableFutureSamples.java`

| Example | What it covers |
|---------|----------------|
| Creating | `runAsync`, `supplyAsync`, `completedFuture`, manual `complete()`, `failedFuture` |
| Transforming | `thenApply`, `thenApplyAsync`, `thenAccept`, `thenRun`, `thenCompose` |
| Combining | `thenCombine`, `allOf`, `anyOf`, collecting results from list of futures |
| Exception Handling | `exceptionally`, `handle`, `whenComplete`, `orTimeout`, `completeOnTimeout` |
| Senior Level | Parallel service calls, sequential dependent calls, fan-out with fallback |

---

## ⚡ Key Methods

```
// ── Creating ──────────────────────────────────────────────────
CompletableFuture.runAsync(runnable)              // async, no return
CompletableFuture.runAsync(runnable, executor)    // custom thread pool
CompletableFuture.supplyAsync(supplier)           // async, returns value
CompletableFuture.supplyAsync(supplier, executor) // custom thread pool
CompletableFuture.completedFuture(value)          // already done
CompletableFuture.failedFuture(exception)         // already failed (Java 9+)

// ── Transforming (non-blocking pipeline) ─────────────────────
.thenApply(fn)           // transform result — same thread
.thenApplyAsync(fn)      // transform result — new thread
.thenAccept(consumer)    // consume result, returns CF<Void>
.thenRun(runnable)       // run after, ignores result
.thenCompose(fn)         // chain dependent CF — like flatMap

// ── Combining ─────────────────────────────────────────────────
.thenCombine(other, fn)  // combine two independent CFs
CF.allOf(cf1, cf2, cf3)  // wait for ALL — returns CF<Void>
CF.anyOf(cf1, cf2, cf3)  // first to complete wins — returns CF<Object>

// ── Exception handling ────────────────────────────────────────
.exceptionally(fn)       // recover from failure — return fallback
.handle(fn)              // runs always (success or failure), can transform
.whenComplete(fn)        // side effect only — cannot change result

// ── Timeout (Java 9+) ─────────────────────────────────────────
.orTimeout(n, unit)                    // throw TimeoutException after n
.completeOnTimeout(value, n, unit)     // use default value after n

// ── Getting result ────────────────────────────────────────────
.get()                   // blocks, throws checked exception
.join()                  // blocks, throws unchecked — use inside lambdas
.getNow(default)         // returns default if not yet complete
```

---

## ⚡ thenApply vs thenCompose

```
// thenApply — mapping returns a plain value → stays wrapped
// Result type: CompletableFuture<String>
cf.thenApply(id -> fetchUser(id))         // fetchUser returns String
                                           // → CF<String> ✅

// thenCompose — mapping returns a CF → flattens one level
// Without thenCompose: CF<CF<String>> — awkward!
cf.thenApply(id -> fetchUserAsync(id))    // fetchUserAsync returns CF<String>
                                           // → CF<CF<String>> ❌

cf.thenCompose(id -> fetchUserAsync(id))  // flattens
                                           // → CF<String> ✅

// Rule: same as map vs flatMap in streams
//   thenApply  = map      (plain value)
//   thenCompose = flatMap (wrapped value)
```

---

## ⚡ Exception Handling Comparison

```
// exceptionally — recovery only, runs on failure
.exceptionally(ex -> "fallback")          // only called if failed

// handle — runs always, can inspect both value and exception
.handle((value, ex) -> {
    if (ex != null) return "fallback";
    return value.toUpperCase();           // transform on success too
})

// whenComplete — side effect only, cannot change result
.whenComplete((value, ex) -> {
    logger.info("done: " + value);        // logging, metrics
    // return value is ignored — result passes through unchanged
})
```

---

## ⚡ allOf Pattern — Collecting Results

```
// allOf returns CF<Void> — results must be fetched from original futures
List<CompletableFuture<String>> futures = ids.stream()
    .map(id -> CompletableFuture.supplyAsync(() -> fetch(id), pool))
    .collect(Collectors.toList());

List<String> results = CompletableFuture
    .allOf(futures.toArray(new CompletableFuture[0]))
    .thenApply(v -> futures.stream()
        .map(CompletableFuture::join)   // join = get without checked exception
        .collect(Collectors.toList()))
    .get();
```

---

## 🔑 Common Mistakes

```
// ❌ Using commonPool for I/O-bound tasks — starves other tasks
CompletableFuture.supplyAsync(() -> callRemoteApi()); // uses commonPool

// ✅ Always use dedicated executor for I/O
ExecutorService pool = Executors.newFixedThreadPool(10);
CompletableFuture.supplyAsync(() -> callRemoteApi(), pool);

// ❌ Blocking inside async pipeline — defeats the purpose
cf.thenApply(id -> fetchUserAsync(id).get()); // .get() blocks the thread!

// ✅ Use thenCompose for dependent async calls
cf.thenCompose(id -> fetchUserAsync(id));

// ❌ Swallowing exceptions — no exceptionally/handle
cf.thenApply(v -> riskyOp(v)).get(); // exception lost if not handled

// ✅ Always handle exceptions at the end of the pipeline
cf.thenApply(v -> riskyOp(v))
  .exceptionally(ex -> { log(ex); return fallback; });

// ❌ join() outside lambda — use get() instead
cf.join(); // throws unchecked CompletionException — harder to catch

// ✅ join() inside stream lambdas (get() can't be used — checked exception)
futures.stream().map(CompletableFuture::join).collect(...)
```

---