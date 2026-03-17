# 🎯 Interview Questions — Java 21

---

**Q1. What are virtual threads and how do they differ from platform threads?**

> Platform threads are backed one-to-one by OS threads. Each consumes
> ~1MB of stack memory, and OS scheduling limits practical count to ~10,000.
> When a platform thread blocks on I/O, it holds the OS thread — doing nothing.
>
> Virtual threads are JVM-managed. They are mounted onto a small pool of
> carrier threads (platform threads, typically sized to CPU cores). When a
> virtual thread blocks — on I/O, sleep, or lock — the JVM unmounts it
> from the carrier thread, which immediately picks up another virtual thread.
> The result: thousands of concurrent I/O operations with only a handful of
> OS threads:
>
> ```
> // 100,000 virtual threads — feasible
> for (int i = 0; i < 100_000; i++) {
>     Thread.startVirtualThread(() -> callDatabase());
> }
> // Each callDatabase() blocks on I/O → unmounts → carrier thread freed
> // All 100k DB calls happen concurrently
> ```
>
> Key differences:
> - Virtual threads are always daemon — JVM won't wait for them at exit
> - Memory: platform ~1MB, virtual ~few KB
> - Virtual threads don't improve CPU-bound tasks — only I/O-bound

---

**Q2. What is pinning in virtual threads and how do you fix it?**

> Pinning occurs when a virtual thread cannot unmount from its carrier
> thread. A pinned virtual thread holds its carrier thread even while
> blocking — negating the concurrency benefit.
>
> Two causes:
> 1. Inside a `synchronized` block or method — JVM cannot safely move
     >    the virtual thread off the carrier
> 2. Calling a native method (JNI)
>
> ```
> // ❌ Pinned — carrier blocked during sleep
> synchronized (lock) {
>     callSlowDatabase(); // carrier occupied — can't run other virtual threads
> }
> ```
>
> Fix: replace `synchronized` with `ReentrantLock`. Since `ReentrantLock`
> is pure Java, the JVM can unmount the virtual thread while it waits:
>
> ```
> // ✅ Not pinned — carrier freed during blocking call
> lock.lock();
> try {
>     callSlowDatabase(); // virtual thread unmounts — carrier is free
> } finally {
>     lock.unlock();
> }
> ```
>
> Detect pinning in production: `-Djdk.tracePinnedThreads=full` JVM flag
> prints a stack trace whenever a virtual thread is pinned.

---

**Q3. What is the difference between pattern matching switch and the old if-instanceof chain?**

> The old approach — repetitive, no compiler enforcement:
> ```
> if (obj instanceof Integer i)     { return "int: " + i; }
> else if (obj instanceof String s) { return "str: " + s; }
> else                              { return "other"; }
> // Compiler doesn't check if all types are handled
> ```
>
> Pattern matching switch — exhaustive, concise, compiler-verified:
> ```
> String result = switch (obj) {
>     case Integer i -> "int: " + i;
>     case String  s -> "str: " + s;
>     default        -> "other";
> };
> ```
>
> Key advantages:
> - **Exhaustiveness** — compiler error if cases are incomplete (for sealed types, no default needed)
> - **Guarded patterns** — `when` clause adds conditions inline
> - **Null handling** — explicit `case null` instead of NPE
> - **Record patterns** — destructure records inline: `case Point(int x, int y)`
> - **Expression** — returns a value directly, no temp variable needed

---

**Q4. What are guarded patterns and why does ordering matter?**

> A guarded pattern adds a `when` condition to a type pattern — the case
> only matches if both the type check AND the condition are true:
>
> ```
> String result = switch (value) {
>     case Integer i when i < 0 -> "negative";
>     case Integer i when i == 0 -> "zero";
>     case Integer i             -> "positive";  // catches all remaining ints
>     default                    -> "not an int";
> };
> ```
>
> Ordering matters because cases are evaluated top-to-bottom. A broader
> pattern before a narrower one makes the narrower one unreachable:
>
> ```
> // ❌ Wrong order — compiler warns "unreachable case"
> case Integer i             -> "any int";    // catches everything
> case Integer i when i < 0  -> "negative";  // never reached
>
> // ✅ Correct — specific cases first
> case Integer i when i < 0  -> "negative";
> case Integer i             -> "non-negative";
> ```
>
> The compiler detects and warns about unreachable cases when a dominating
> pattern appears before a more specific one.

---

**Q5. What are Sequenced Collections and what problem do they solve?**

> Before Java 21, accessing the first or last element of a collection
> required knowing the exact type — there was no unified API:
>
> ```
> list.get(0)               // List
> list.get(list.size() - 1) // List
> deque.peekFirst()         // Deque
> sortedSet.first()         // SortedSet
> // LinkedHashSet — no direct API at all!
> ```
>
> Java 21 introduced `SequencedCollection`, `SequencedSet`, and
> `SequencedMap` interfaces added to the existing hierarchy:
>
> ```
> // Same API for all ordered collections
> collection.getFirst()    // first element
> collection.getLast()     // last element
> collection.addFirst(e)   // insert at front
> collection.addLast(e)    // insert at end
> collection.reversed()    // reversed view — live, not a copy
>
> // Works on: List, Deque, LinkedHashSet, SortedSet, LinkedHashMap, TreeMap
> ```
>
> `reversed()` returns a live view — changes to the original are reflected.
> This is distinct from `Collections.reverse()` which mutates in place.

---

**Q6. Virtual threads are in Java 21 — should you always use them? What are the cases where they don't help?**

> Virtual threads are not a universal solution — they help specifically
> with I/O-bound, blocking workloads. Two cases where they don't help:
>
> **CPU-bound work:** hashing, sorting, encryption, image processing.
> The thread never blocks — it never unmounts — carrier threads are
> occupied continuously. Virtual threads provide no throughput improvement
> over a fixed platform thread pool sized to CPU cores:
>
> ```
> // No benefit — CPU never freed
> executor.submit(() -> computeSHA256(largeFile));
> ```
>
> **Already-async code:** if you're using reactive/async patterns
> (CompletableFuture chains, WebFlux, RxJava), virtual threads don't
> add further benefit — the async framework already doesn't block threads.
>
> Virtual threads are the right choice when: your code is written in
> simple blocking style (JDBC, RestTemplate, Files.readString) and you
> need high concurrency. They let you write readable synchronous code
> and get async-level throughput — no reactive framework needed.
>
> In Spring Boot 3.2+: `spring.threads.virtual.enabled=true` switches
> all web request handling and `@Async` to virtual threads — the easiest
> migration path.