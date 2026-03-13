# 🔀 Concurrency

> Writing correct, performant code that runs across multiple threads.
> The hardest part of Java — and the most asked about in senior interviews.

---

## 🧠 Why Concurrency is Hard

Three problems must be solved simultaneously:

- **Atomicity** — a sequence of operations must appear as one unit. `count++` is three steps — read, add, write. Two threads interleaving these steps lose an update.
- **Visibility** — a write on Thread A may sit in a CPU cache. Thread B may never see the updated value without explicit coordination.
- **Ordering** — the compiler and CPU reorder instructions for performance. What you write is not necessarily what executes, across threads.

Every concurrency tool in this module solves one or more of these three problems.

---

## 📦 Sub-Modules

### [`threads`](threads/)
The foundation. Thread lifecycle, creation, daemon threads, `join`, `sleep`,
cooperative interruption, `ThreadLocal`, `UncaughtExceptionHandler`.

### [`synchronization`](synchronization/)
The primitives. `synchronized` keyword, `volatile`, happens-before,
`wait`/`notify`, race conditions, deadlock — causes and prevention.

### [`locks`](locks/)
Explicit locks. `ReentrantLock`, `tryLock` with timeout, `lockInterruptibly`,
fairness, `ReadWriteLock`, `StampedLock`, `Condition` for multiple wait sets.

### [`executors`](executors/)
Thread pools. `ExecutorService`, `Callable`, `Future`, `ThreadPoolExecutor`
internals, rejection policies, `ScheduledExecutorService`, `ForkJoinPool`
and work stealing.

### [`concurrent`](concurrent/)
Concurrent collections. `ConcurrentHashMap` with atomic compute/merge,
`CopyOnWriteArrayList`, `BlockingQueue` variants, `ConcurrentLinkedQueue`,
producer-consumer and pipeline patterns.

### [`atomic`](atomic/)
Lock-free programming. CAS, `AtomicInteger`, `AtomicReference`, the ABA
problem, `AtomicStampedReference`, `LongAdder` vs `AtomicLong`, spin locks,
lock-free stack.

---

## 🗺️ What Solves What

| Problem | Tools |
|---------|-------|
| Atomicity | `synchronized`, `ReentrantLock`, `AtomicInteger`, `ConcurrentHashMap.compute` |
| Visibility | `synchronized`, `volatile`, `AtomicReference` |
| Ordering | `synchronized`, `volatile`, happens-before rules |
| Deadlock prevention | `tryLock(timeout)`, consistent lock ordering |
| High-throughput counter | `LongAdder` |
| Read-heavy shared state | `ReadWriteLock`, `StampedLock`, `CopyOnWriteArrayList` |
| Producer-consumer | `BlockingQueue`, `Condition` |
| Task parallelism | `ExecutorService`, `ForkJoinPool`, `CompletableFuture` |

---

## 📚 Interview Coverage

| Topic | Sub-module |
|-------|------------|
| Thread states and lifecycle | threads |
| `run()` vs `start()`, daemon threads | threads |
| Race conditions, lost updates | synchronization |
| `synchronized` vs `volatile` | synchronization |
| Deadlock — causes and fixes | synchronization |
| Happens-before, Java Memory Model | synchronization |
| Double-checked locking | synchronization |
| `ReentrantLock` vs `synchronized` | locks |
| `tryLock` deadlock prevention | locks |
| `ReadWriteLock`, `StampedLock` | locks |
| `Condition` vs `wait`/`notify` | locks |
| Thread pool sizing and rejection | executors |
| `Future`, `invokeAll`, `invokeAny` | executors |
| `ForkJoinPool`, work stealing | executors |
| `ConcurrentHashMap` internals | concurrent |
| `CopyOnWriteArrayList` snapshot | concurrent |
| `BlockingQueue` variants | concurrent |
| CAS, ABA problem | atomic |
| `LongAdder` vs `AtomicLong` | atomic |
| Lock-free data structures | atomic |

---

## 🔑 Quick Reference

```
// ── Synchronized ─────────────────────────────
synchronized (lock) { ... }                   // mutual exclusion + visibility
volatile boolean flag;                        // visibility only — no atomicity

// ── ReentrantLock ────────────────────────────
lock.lock(); try { ... } finally { lock.unlock(); }
lock.tryLock(100, MILLISECONDS)               // deadlock-safe acquisition

// ── Atomic ───────────────────────────────────
counter.incrementAndGet()                     // lock-free ++i
counter.compareAndSet(expected, update)       // CAS — core primitive
adder.increment(); adder.sum()                // high-throughput counter

// ── Collections ──────────────────────────────
map.computeIfAbsent(key, k -> load(k))        // atomic lazy load
map.merge(key, 1, Integer::sum)               // atomic accumulate
queue.put(item); queue.take()                 // blocking producer-consumer

// ── Executors ────────────────────────────────
ExecutorService pool = Executors.newFixedThreadPool(N);
Future<T> f = pool.submit(callable);
T result = f.get(timeout, MILLISECONDS);
pool.shutdown(); pool.awaitTermination(10, SECONDS);
```

---

> 💡 Each sub-module has its own `README.md` and `INTERVIEW_QUESTIONS.md`.