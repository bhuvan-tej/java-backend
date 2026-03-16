package com.javabackend.java21;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.*;

/**
 *
 * Java 21 — Virtual Threads
 *
 * Java 19: Virtual threads preview
 * Java 20: Virtual threads second preview
 * Java 21: Virtual threads stable ✅
 *
 * THE PROBLEM WITH PLATFORM THREADS
 *  Each platform thread = one OS thread = ~1MB stack memory
 *  A server with 500 concurrent requests needs 500 OS threads
 *  OS has limits — too many threads → context switching overhead → slow
 *  Traditional fix: async/reactive code (CompletableFuture, WebFlux)
 *  Downside: complex code, hard to debug, callback hell
 *
 * VIRTUAL THREADS SOLUTION
 *  Virtual thread = JVM-managed, ~few KB overhead
 *  Can create millions — JVM schedules them on a small pool of carrier threads
 *  When virtual thread blocks on I/O → JVM unmounts it, carrier thread is freed
 *  Write simple blocking code → get async-level throughput
 *
 * KEY CONCEPTS
 *  Platform thread  — backed by OS thread, expensive (~1MB stack)
 *  Virtual thread   — backed by JVM, cheap (~few KB), mounted on carrier
 *  Carrier thread   — platform thread from ForkJoinPool that runs virtual threads
 *  Mounting         — virtual thread scheduled onto a carrier to execute
 *  Unmounting       — virtual thread suspended (e.g. waiting for I/O), carrier freed
 *  Pinning          — virtual thread CANNOT unmount (inside synchronized / native)
 *
 */
public class VirtualThreads {

    public static void main(String[] args) throws Exception {
        System.out.println("━━━ EXAMPLE 1 — Creating Virtual Threads ━━━\n");
        creatingVirtualThreads();

        System.out.println("\n━━━ EXAMPLE 2 — Virtual Thread Executor ━━━\n");
        virtualThreadExecutor();

        System.out.println("\n━━━ EXAMPLE 3 — Throughput Comparison ━━━\n");
        throughputComparison();

        System.out.println("\n━━━ EXAMPLE 4 — Structured Concurrency ━━━\n");
        structuredConcurrency();

        System.out.println("\n━━━ EXAMPLE 5 — Pinning and Best Practices ━━━\n");
        pinningAndBestPractices();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — Creating Virtual Threads
    // Three ways to create, key properties
    // ─────────────────────────────────────────────
    static void creatingVirtualThreads() throws InterruptedException {

        // ── Way 1: Thread.ofVirtual() builder ──
        // Fluent API — name, priority, uncaughtExceptionHandler, then start/unstarted
        Thread vt = Thread.ofVirtual()
                .name("my-virtual-thread")   // name for debugging — shows in thread dumps
                .start(() -> System.out.println(
                        "  virtual: " + Thread.currentThread().getName()
                                + " | isVirtual=" + Thread.currentThread().isVirtual()
                        // isVirtual() — new in Java 21, distinguishes virtual from platform
                ));
        vt.join(); // join works exactly the same as with platform threads

        // ── Way 2: Thread.startVirtualThread() ──
        // Shorthand — equivalent to Thread.ofVirtual().start(task)
        // Use when you don't need to configure the thread further
        Thread vt2 = Thread.startVirtualThread(() ->
                System.out.println("  startVirtual: isVirtual="
                        + Thread.currentThread().isVirtual()));
        vt2.join();

        // ── Way 3: Thread.ofPlatform() — for comparison ──
        // Same API as ofVirtual() but creates an OS-backed platform thread
        Thread platform = Thread.ofPlatform().name("platform").start(() ->
                System.out.println("  platform: isVirtual="
                        + Thread.currentThread().isVirtual())); // always false
        platform.join();

        // ── Key property: virtual threads are ALWAYS daemon ──
        // This means the JVM will NOT wait for virtual threads before exiting
        // Platform threads default to non-daemon — JVM waits for them
        Thread vt3 = Thread.ofVirtual().unstarted(() -> {});
        System.out.println("  isVirtual()  : " + vt3.isVirtual()); // true
        System.out.println("  isDaemon()   : " + vt3.isDaemon());  // always true

        // ── Creating 100,000 virtual threads — feasible ──
        // Doing this with platform threads would exhaust OS thread limit
        // With virtual threads — JVM handles scheduling on ~N carrier threads
        // where N = number of CPU cores (ForkJoinPool.commonPool parallelism)
        int count = 100_000;
        var latch   = new CountDownLatch(count);
        var counter = new AtomicInteger(0);
        long start  = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            Thread.startVirtualThread(() -> {
                counter.incrementAndGet(); // each thread does tiny work
                latch.countDown();         // signal completion
            });
        }

        latch.await(); // wait for all 100k threads to finish
        System.out.println("  " + count + " virtual threads in "
                + (System.currentTimeMillis() - start) + "ms"
                + " | count=" + counter.get());
        // Typical output: 100000 virtual threads in ~200ms
        // Same with platform threads would be impossible — OS thread limit ~10k
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — Virtual Thread Executor
    // Production pattern — use with ExecutorService
    // ─────────────────────────────────────────────
    static void virtualThreadExecutor() throws Exception {

        // ── newVirtualThreadPerTaskExecutor ──
        // Creates ONE new virtual thread for EACH submitted task
        // No pooling — virtual threads are so cheap, pooling is unnecessary
        // This is the recommended production pattern for I/O-bound work
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<String>> futures = new ArrayList<>();

            for (int i = 1; i <= 5; i++) {
                int taskId = i;
                futures.add(executor.submit(() -> {
                    // Simulate I/O — virtual thread unmounts during sleep
                    // Carrier thread is freed to run other virtual threads
                    Thread.sleep(Duration.ofMillis(50));
                    return "task-" + taskId
                            + " on " + Thread.currentThread().getName()
                            + " (virtual=" + Thread.currentThread().isVirtual() + ")";
                }));
            }

            // Collect results — all 5 tasks ran concurrently on virtual threads
            for (var f : futures) {
                System.out.println("  " + f.get());
            }

        } // try-with-resources calls executor.close()
        // close() = shutdown() + awaitTermination() — waits for all tasks

        // ── Named virtual thread factory ──
        // Thread.ofVirtual().name(prefix, start) creates sequentially named threads
        // "worker-0", "worker-1", "worker-2" etc.
        // Crucial for debugging — unnamed virtual threads are hard to identify
        var factory = Thread.ofVirtual().name("worker-", 0).factory();
        try (ExecutorService executor = Executors.newThreadPerTaskExecutor(factory)) {
            executor.submit(() ->
                            System.out.println("  named: " + Thread.currentThread().getName()))
                    .get();
        }

        // ── Spring Boot 3.2+ — one config line ──
        // spring.threads.virtual.enabled=true
        // Replaces all internal thread pools with virtual thread executors
        // Tomcat, @Async, scheduled tasks — all become virtual
        System.out.println("  Spring Boot 3.2+: spring.threads.virtual.enabled=true ✓");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — Throughput Comparison
    // I/O-bound workload — where virtual threads shine
    // ─────────────────────────────────────────────
    static void throughputComparison() throws Exception {
        int tasks      = 200;    // number of concurrent tasks
        long ioDelayMs = 50;     // simulated I/O wait per task (DB call, HTTP, etc.)

        // ── Platform thread pool — 10 threads ──
        // Only 10 tasks run at a time — rest wait in queue
        // Total time ≈ (tasks / poolSize) × ioDelay = (200/10) × 50ms = 1000ms
        long platformStart = System.currentTimeMillis();
        try (ExecutorService pool = Executors.newFixedThreadPool(10)) {
            var futs = IntStream.range(0, tasks)
                    .mapToObj(i -> pool.submit(() -> {
                        Thread.sleep(Duration.ofMillis(ioDelayMs)); // blocks platform thread
                        return i;
                    }))
                    .toList();
            for (var f : futs) f.get(); // wait for all to complete
        }
        long platformTime = System.currentTimeMillis() - platformStart;

        // ── Virtual thread executor ──
        // All 200 tasks start immediately on virtual threads
        // When each hits sleep (I/O simulation) → unmounts → carrier freed
        // All 200 I/O waits overlap → total time ≈ just the I/O delay = ~50ms
        long virtualStart = System.currentTimeMillis();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futs = IntStream.range(0, tasks)
                    .mapToObj(i -> executor.submit(() -> {
                        Thread.sleep(Duration.ofMillis(ioDelayMs)); // unmounts virtual thread
                        return i;
                    }))
                    .toList();
            for (var f : futs) f.get();
        }
        long virtualTime = System.currentTimeMillis() - virtualStart;

        System.out.println("  tasks           : " + tasks + " (each with " + ioDelayMs + "ms I/O)");
        System.out.println("  platform (10)   : " + platformTime + "ms");
        System.out.println("  virtual threads : " + virtualTime + "ms");
        System.out.printf("  speedup         : ~%.1fx%n",
                (double) platformTime / Math.max(virtualTime, 1));

        // Result: platform ~1000ms, virtual ~50ms → ~20x faster
        // The more I/O-bound and the more concurrency, the bigger the gap
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — Structured Concurrency (Preview Java 21)
    // Subtasks have bounded lifetime — clean cancellation
    // ─────────────────────────────────────────────
    static void structuredConcurrency() throws Exception {

        // THE PROBLEM WITH UNSTRUCTURED CONCURRENCY
        // When you submit multiple tasks, their lifetimes are unbound:
        // - If one fails, others keep running — wasted resources
        // - Cancellation is manual and error-prone
        // - Hard to reason about which thread is responsible for what

        // ── Manual approach — shows the problem ──
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var userFuture  = executor.submit(() -> fetchUser("U1"));
            var orderFuture = executor.submit(() -> fetchOrders("U1"));

            try {
                String user   = userFuture.get(2, TimeUnit.SECONDS);
                String orders = orderFuture.get(2, TimeUnit.SECONDS);
                System.out.println("  manual user  : " + user);
                System.out.println("  manual orders: " + orders);
            } catch (Exception e) {
                // Manual cancellation — easy to forget or get wrong
                userFuture.cancel(true);
                orderFuture.cancel(true);
                System.out.println("  manual cancel: " + e.getClass().getSimpleName());
            }
        }

        // ── StructuredTaskScope (preview — available with --enable-preview) ──
        // Key guarantee: scope.close() ensures ALL forked tasks have completed
        // or been cancelled before returning. No leaked threads.
        //
        // ShutdownOnFailure — if ANY task fails, cancel ALL others immediately
        // ShutdownOnSuccess — as soon as ONE task succeeds, cancel the rest
        //
        // Uncomment if running with --enable-preview flag:
        /*
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // fork() submits task on a virtual thread, returns a handle
            var userTask  = scope.fork(() -> fetchUser("U1"));
            var orderTask = scope.fork(() -> fetchOrders("U1"));

            scope.join();           // wait for both — or until one fails
            scope.throwIfFailed();  // propagate exception if any task failed

            // Both guaranteed complete here — safe to call .get()
            System.out.println("  scoped user  : " + userTask.get());
            System.out.println("  scoped orders: " + orderTask.get());
        } // scope.close() — cancels any still-running tasks, waits for cleanup
        */

        System.out.println("  StructuredTaskScope: preview Java 21, stable Java 23");
        System.out.println("  ShutdownOnFailure  : cancel all if one fails");
        System.out.println("  ShutdownOnSuccess  : cancel rest when first succeeds");
        System.out.println("  Benefit: no leaked threads, clean cancellation ✓");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — Pinning and Best Practices
    // When virtual threads lose their advantage
    // ─────────────────────────────────────────────
    static void pinningAndBestPractices() throws InterruptedException {

        // ── PINNING — when virtual thread cannot unmount ──
        // Normally: virtual thread blocks on I/O → unmounts → carrier thread freed
        // Pinned:   virtual thread is stuck to carrier → carrier blocked too
        //
        // Two causes of pinning:
        // 1. Inside a synchronized block or method
        //    (JVM cannot safely move the virtual thread off the carrier)
        // 2. Calling a native method (JNI)
        //
        // Pinning is not a crash — code still works
        // But: if all carrier threads are pinned → no progress → throughput collapses
        // Detect with: -Djdk.tracePinnedThreads=full JVM flag

        System.out.println("  PINNING — virtual thread stuck to carrier:");
        System.out.println("  Cause 1: synchronized block with blocking I/O inside");
        System.out.println("  Cause 2: native method call (JNI)");
        System.out.println("  Detect : -Djdk.tracePinnedThreads=full");

        // ── Fix: replace synchronized with ReentrantLock ──
        // ReentrantLock is implemented in Java — JVM CAN unmount during lock.lock()
        // synchronized is a JVM primitive — JVM CANNOT unmount
        var lock = new ReentrantLock();
        Thread vt = Thread.startVirtualThread(() -> {
            lock.lock(); // virtual thread CAN unmount while waiting for this lock
            try {
                // Simulated I/O inside the lock — carrier is FREE during sleep
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });
        vt.join();
        System.out.println("  ReentrantLock: carrier freed during blocking ✓");

        // ── DO NOT pool virtual threads ──
        // Thread pools exist to LIMIT resource consumption
        // Virtual threads are ~few KB each — no need to limit them
        // Pooling them defeats the purpose AND adds overhead
        System.out.println("\n  ❌ DON'T pool virtual threads:");
        System.out.println("     Executors.newFixedThreadPool(N, virtualFactory) — wrong");
        System.out.println("  ✅ DO use newVirtualThreadPerTaskExecutor()");

        // ── DO NOT use ThreadLocal for heavy objects with virtual threads ──
        // ThreadLocal is per-thread storage — fine with ~200 platform threads
        // With millions of virtual threads: millions of ThreadLocal copies = OOM
        // Solution: ScopedValue (preview in Java 21) — immutable, scoped to task
        System.out.println("\n  ❌ DON'T use ThreadLocal<HeavyObject> at scale:");
        System.out.println("     millions of virtual threads × heavy object = OOM");
        System.out.println("  ✅ ScopedValue (preview Java 21) — immutable, no copy per thread");

        // ── Virtual threads are NOT faster for CPU-bound work ──
        // CPU-bound: hashing, encryption, sorting, computation
        // → thread never blocks → never unmounts → no benefit over platform threads
        // → carrier threads still limited to CPU core count
        //
        // Virtual threads excel at: HTTP calls, DB queries, file I/O
        // — anything where the thread spends most of its time WAITING
        System.out.println("\n  Virtual threads benefit: I/O-bound (HTTP, DB, files)");
        System.out.println("  Virtual threads no help : CPU-bound (crypto, sorting, math)");
    }

    // ── Simulated service calls ──
    static String fetchUser(String id) throws InterruptedException {
        Thread.sleep(50);  // simulate DB or HTTP call
        return "User(" + id + ")";
    }

    static String fetchOrders(String id) throws InterruptedException {
        Thread.sleep(60); // simulate DB or HTTP call
        return "Orders(" + id + ")";
    }

}