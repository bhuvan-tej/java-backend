# 🎯 Interview Questions — Concurrent Collections

---

**Q1. How does `ConcurrentHashMap` achieve thread safety without a single global lock?**

> In Java 8+, `ConcurrentHashMap` uses a combination of CAS (Compare-And-Swap)
> and `synchronized` on individual buckets:
>
> - **Reads** — almost entirely lock-free. Reads use `volatile` fields so
    >   writes are immediately visible without acquiring any lock.
> - **First insert into a bucket** — done with CAS. No lock needed.
> - **Subsequent inserts/updates to an existing bucket** — `synchronized`
    >   on the head node of that bucket only. Only threads hitting the same
    >   bucket contend.
>
> The result: reads never block, writes only contend at the bucket level.
> With a default of 16 buckets (grows with capacity), 16 threads can write
> to different buckets simultaneously.
>
> This is fundamentally different from `Collections.synchronizedMap()` which
> puts a single lock around every operation — one thread at a time for
> everything.

---

**Q2. What is the difference between `compute`, `computeIfAbsent`, and `merge` on `ConcurrentHashMap`?**

> All three are atomic read-modify-write operations — no external
> synchronization needed:
>
> `computeIfAbsent(key, mappingFn)` — calls `mappingFn` only if key is absent.
> Returns existing value if present. Useful for lazy initialisation:
> ```
> map.computeIfAbsent("key", k -> expensiveLoad(k)); // computed once, atomically
> ```
>
> `compute(key, remappingFn)` — always calls `remappingFn` with current value
> (null if absent). Replaces value with result. Removes key if result is null:
> ```
> map.compute("key", (k, v) -> v == null ? 1 : v + 1); // increment or init
> ```
>
> `merge(key, value, mergeFn)` — if key absent, sets value directly. If present,
> calls `mergeFn(existing, value)`. Removes key if result is null:
> ```
> map.merge("word", 1, Integer::sum); // word count — clean and atomic
> ```
>
> The critical point: these are atomic only for the single operation. A
> sequence of `get` + `compute` is NOT atomic. Use a single `compute` or
> `merge` call to keep the operation indivisible.

---

**Q3. Why does `CopyOnWriteArrayList` never throw `ConcurrentModificationException`?**

> `ArrayList` uses a `modCount` field — if it changes during iteration,
> the iterator throws `ConcurrentModificationException` as a fail-fast
> safety check.
>
> `CopyOnWriteArrayList` takes a different approach: when you call
> `iterator()`, it captures a reference to the current array. The iterator
> walks that snapshot — it is completely unaffected by subsequent writes:
>
> ```
> Iterator<String> it = cowList.iterator(); // snapshot taken here
> cowList.add("new");    // creates a NEW array — iterator still on old one
> cowList.remove("old"); // creates another new array — iterator unaffected
> while (it.hasNext()) it.next(); // walks the original snapshot safely
> ```
>
> The cost: every write (`add`, `remove`, `set`) copies the entire backing
> array. Reads are free but writes are O(n). This makes it ideal for
> read-heavy, write-rare scenarios — listener lists, observer registries —
> and a poor choice for large lists with frequent modifications.

---

**Q4. What is the difference between `put` and `offer` on a `BlockingQueue`? When does each block?**

> `put(item)` — blocks indefinitely if the queue is full. The calling thread
> suspends until space is available. Used when the producer must not lose
> items and can afford to wait:
> ```
> queue.put(item); // blocks until space available — natural backpressure
> ```
>
> `offer(item)` — non-blocking variant. Returns `false` immediately if full:
> ```
> boolean accepted = queue.offer(item); // returns false if full — never blocks
> ```
>
> `offer(item, timeout, unit)` — waits up to timeout then returns false:
> ```
> boolean accepted = queue.offer(item, 100, MILLISECONDS);
> ```
>
> Similarly on the consumer side:
> - `take()` — blocks until item available
> - `poll()` — returns null immediately if empty
> - `poll(timeout, unit)` — waits up to timeout
>
> In production pipeline design: `put`/`take` gives you automatic backpressure
> — producers slow down when consumers can't keep up. `offer`/`poll` is for
> cases where the caller must stay non-blocking (event loops, reactive code).

---

**Q5. When would you choose `LinkedBlockingQueue` over `ArrayBlockingQueue`?**

> Both are bounded blocking queues but their internal locking differs:
>
> `ArrayBlockingQueue` — single `ReentrantLock` for both head and tail.
> Producer and consumer contend on the same lock. Backed by a fixed array —
> no allocation on enqueue/dequeue.
>
> `LinkedBlockingQueue` — separate locks for head (consumer) and tail
> (producer). Producer and consumer can operate concurrently without
> contention. Backed by linked nodes — one node allocated per item.
>
> Choose `LinkedBlockingQueue` when:
> - High throughput under concurrent producer + consumer load
> - Capacity can be `Integer.MAX_VALUE` (effectively unbounded) — watch for OOM
>
> Choose `ArrayBlockingQueue` when:
> - Strict bounded capacity is required (memory control)
> - You want optional fairness (`new ArrayBlockingQueue(n, true)`)
> - Allocation pressure matters — no per-item node allocation
>
> In practice `LinkedBlockingQueue` is the default choice in
> `Executors.newFixedThreadPool()` — its separate locks give better
> throughput for thread pool task queues.

---

**Q6. What is `SynchronousQueue` and where is it used internally in the JDK?**

> `SynchronousQueue` has zero capacity — it holds no elements. A `put()`
> blocks until another thread calls `take()`, and vice versa. Every insert
> must be directly handed off to a waiting consumer:
>
> ```
> SynchronousQueue<String> sq = new SynchronousQueue<>();
>
> Thread producer = new Thread(() -> sq.put("item")); // blocks until taken
> Thread consumer = new Thread(() -> sq.take());      // blocks until given
> ```
>
> It is essentially a rendezvous point — synchronises a producer and consumer
> at the moment of exchange.
>
> JDK usage: `Executors.newCachedThreadPool()` uses `SynchronousQueue`
> as its work queue. When a task is submitted, it tries to hand it directly
> to an idle thread. If no idle thread exists, a new one is created. If a
> thread is idle for 60 seconds, it is removed. This is why cached thread
> pools create threads on demand — the queue never holds tasks, forcing
> immediate thread creation or reuse.

---

**Q7. What is the risk of using `ConcurrentHashMap` for a check-then-act pattern?**

> `ConcurrentHashMap` makes individual operations atomic but NOT sequences
> of operations. A check-then-act across two separate calls has a race
> condition:
>
> ```
> // ❌ Not atomic — another thread can insert between check and put
> if (!map.containsKey("key")) {
>     map.put("key", computeValue()); // race window here
> }
>
> // ❌ Also not atomic
> Integer val = map.get("key");
> if (val == null) {
>     map.put("key", 1);
> } else {
>     map.put("key", val + 1);
> }
> ```
>
> The fix is to use the atomic compound methods:
> ```
> // ✅ computeIfAbsent — atomic
> map.computeIfAbsent("key", k -> computeValue());
>
> // ✅ compute — atomic read-modify-write
> map.compute("key", (k, v) -> v == null ? 1 : v + 1);
>
> // ✅ merge — atomic for accumulation
> map.merge("key", 1, Integer::sum);
> ```
>
> This is a very common interview follow-up: candidates know CHM is
> thread-safe but don't know that multi-step operations still need
> the compound atomic methods.