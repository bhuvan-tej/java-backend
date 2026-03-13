# 🎯 Interview Questions — Locks

---

**Q1. What are the key advantages of `ReentrantLock` over `synchronized`?**

> `ReentrantLock` provides capabilities that `synchronized` simply cannot:
>
> - **tryLock(timeout)** — attempt to acquire, give up after N ms → deadlock prevention
> - **lockInterruptibly()** — waiting thread can be cancelled via `interrupt()`
> - **Fairness** — `new ReentrantLock(true)` ensures longest-waiting thread gets lock first
> - **Condition** — multiple wait sets per lock, more precise than `wait/notify`
> - **Diagnostics** — `isLocked()`, `getQueueLength()`, `getHoldCount()`
>
> `synchronized` advantages: simpler syntax, compiler-guaranteed unlock, slightly
> lower overhead for uncontended cases.
>
> Rule of thumb: start with `synchronized`. Upgrade to `ReentrantLock` only
> when you specifically need timeout, interruption, fairness, or Condition.

---

**Q2. Why must `lock.unlock()` always be in a `finally` block?**

> If an exception is thrown inside the critical section, execution jumps
> past the unlock call — the lock is held forever. Every thread that tries
> to acquire it will block indefinitely:
>
> ```
> // ❌ Exception skips unlock — lock held forever
> lock.lock();
> riskyOperation(); // throws RuntimeException
> lock.unlock();    // never reached
>
> // ✅ finally guarantees unlock regardless of exceptions
> lock.lock();
> try {
>     riskyOperation();
> } finally {
>     lock.unlock(); // always runs
> }
> ```
>
> This is the single most important rule when using explicit locks. With
> `synchronized` the JVM handles this automatically — it is the main
> ergonomic advantage of `synchronized` over explicit locks.

---

**Q3. How does `tryLock` prevent deadlocks?**

> Deadlock requires threads to hold one lock and wait indefinitely for
> another. `tryLock(timeout)` breaks this — a thread gives up after the
> timeout and releases what it holds:
>
> ```
> // Deadlock-safe lock acquisition
> boolean acquiredA = false, acquiredB = false;
> try {
>     acquiredA = lockA.tryLock(100, TimeUnit.MILLISECONDS);
>     acquiredB = lockB.tryLock(100, TimeUnit.MILLISECONDS);
>     if (acquiredA && acquiredB) {
>         doWork();
>     } else {
>         // couldn't get both — back off and retry later
>     }
> } finally {
>     if (acquiredA) lockA.unlock();
>     if (acquiredB) lockB.unlock();
> }
> ```
>
> The timeout breaks the "hold and wait" condition — if a thread cannot
> acquire all required locks within the timeout, it releases what it has
> and retries. Add random jitter to retries to avoid livelock.

---

**Q4. What is `ReadWriteLock`? When would you use it over `ReentrantLock`?**

> `ReadWriteLock` maintains a pair of locks — one for reads, one for writes.
> The contract: multiple threads can hold the read lock simultaneously, but
> the write lock is exclusive — no readers or other writers allowed:
>
> ```
> ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
> // Many threads can read concurrently
> rwLock.readLock().lock();
> try { return cache.get(key); }
> finally { rwLock.readLock().unlock(); }
>
> // Write is exclusive — blocks all readers and writers
> rwLock.writeLock().lock();
> try { cache.put(key, value); }
> finally { rwLock.writeLock().unlock(); }
> ```
>
> Use `ReadWriteLock` when: reads are frequent, writes are rare, and read
> operations are long enough that concurrency matters (cache lookups,
> config reads, reference data).
>
> Do NOT use when: writes are frequent, read operations are very short
> (lock overhead outweighs concurrency gain), or you need optimistic reads
> (use `StampedLock` instead).

---

**Q5. What is `Condition` and how is it better than `wait/notify`?**

> `Condition` is a replacement for `Object.wait/notify` that ties waiting
> to a specific `Lock`. The key advantage: one lock can have multiple
> `Condition` objects — multiple separate wait sets:
>
> ```
> ReentrantLock lock = new ReentrantLock();
> Condition notFull  = lock.newCondition(); // producers wait here
> Condition notEmpty = lock.newCondition(); // consumers wait here
>
> // Producer only wakes consumers — not other producers
> notEmpty.signal();
>
> // Consumer only wakes producers — not other consumers
> notFull.signal();
> ```
>
> With `wait/notifyAll()`, waking all threads is wasteful — producers
> and consumers wake each other unnecessarily. `Condition` lets you
> signal only the relevant party.
>
> Equivalent pairs:
> - `object.wait()` → `condition.await()`
> - `object.notify()` → `condition.signal()`
> - `object.notifyAll()` → `condition.signalAll()`
>
> Same rule applies: always check condition in a `while` loop — spurious
> wakeups are possible.

---

**Q6. What is `StampedLock`? What is an optimistic read?**

> `StampedLock` is a lock with three modes — write, read, and optimistic read.
> It was added in Java 8 specifically for read-heavy workloads where acquiring
> even a read lock causes measurable overhead.
>
> Optimistic read acquires no lock at all — it just records a stamp. After
> reading, it validates that no write happened during the read. If validation
> fails, fall back to a real read lock:
>
> ```
> StampedLock sl = new StampedLock();
>
> long stamp = sl.tryOptimisticRead(); // no lock — just a stamp
> double x = point.x;
> double y = point.y;
> if (!sl.validate(stamp)) {         // was there a write?
>     stamp = sl.readLock();         // yes — fall back to real lock
>     try { x = point.x; y = point.y; }
>     finally { sl.unlockRead(stamp); }
> }
> ```
>
> Critical limitations of `StampedLock`:
> - **NOT reentrant** — same thread acquiring twice causes deadlock
> - No `Condition` support
> - More complex API — easy to misuse
>
> Best for: coordinates, counters, config, or any small data structure
> that is read very frequently and written rarely.

---

**Q7. What is lock fairness? What is the tradeoff?**

> A fair lock guarantees that the longest-waiting thread acquires the lock
> next. An unfair lock (default) allows any waiting thread to jump the queue:
>
> ```
> ReentrantLock fair   = new ReentrantLock(true);  // FIFO ordering
> ReentrantLock unfair = new ReentrantLock(false); // default
> ```
>
> Tradeoff:
> - **Fair lock** — prevents starvation, predictable order, but lower
    >   throughput. Each lock acquisition requires checking the queue, and
    >   threads that were just unblocked often lose to newly arriving threads
    >   in unfair mode — which is actually faster.
> - **Unfair lock** — higher throughput, risk of starvation under heavy
    >   load. A thread could theoretically wait forever if new threads keep
    >   arriving.
>
> In practice: unfair locks are almost always preferred. Starvation is
> rare in real workloads. Use fair locks only when you have a demonstrated
> starvation problem and measured that fairness overhead is acceptable.

---

**Q8. Can you downgrade a write lock to a read lock in `ReentrantReadWriteLock`?**

> Yes — downgrade is supported. A thread holding the write lock can acquire
> the read lock and then release the write lock, atomically transitioning
> to read-only mode without losing visibility of its own write:
>
> ```
> rwLock.writeLock().lock();
> try {
>     update(); // exclusive write
>     rwLock.readLock().lock(); // acquire read lock while holding write lock
> } finally {
>     rwLock.writeLock().unlock(); // release write — now only holding read
> }
> try {
>     readAfterWrite(); // read with guarantee that our write is visible
> } finally {
>     rwLock.readLock().unlock();
> }
> ```
>
> Upgrade (read → write) is NOT supported — it causes deadlock. Two threads
> both holding the read lock and both trying to upgrade will wait for each
> other to release the read lock first.
>
> For read-to-write upgrade use `StampedLock.tryConvertToWriteLock(stamp)`.