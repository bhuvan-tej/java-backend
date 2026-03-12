# 🎯 Interview Questions — Synchronization

---

**Q1. What is a race condition? Give a concrete example.**

> A race condition occurs when the correctness of a program depends on the
> relative timing of thread execution. The most common form is the
> read-modify-write race:
>
> ```
> class Counter {
>     int count = 0;
>     void increment() { count++; } // NOT thread-safe
> }
> ```
>
> `count++` compiles to three bytecode instructions: read `count`, add 1,
> write back. Two threads can interleave:
>
> ```
> Thread A reads count = 5
> Thread B reads count = 5   ← same value, A hasn't written yet
> Thread A writes count = 6
> Thread B writes count = 6  ← B's increment is lost — expected 7
> ```
>
> This is a lost update. The result is non-deterministic — depends on
> thread scheduling, CPU speed, and cache state. It may work correctly
> 999 times and fail on the 1000th run under load.

---

**Q2. What does `synchronized` guarantee? What are its three forms?**

> `synchronized` provides three guarantees:
> - **Atomicity** — only one thread executes the synchronized block at a time
> - **Visibility** — changes made inside synchronized are visible to all threads that subsequently synchronize on the same lock
> - **Ordering** — prevents reordering across the lock boundary
>
> Three forms:
>
> ```
> // 1. Instance method — lock on 'this'
> synchronized void increment() { count++; }
>
> // 2. Block — lock on specified object, finer granularity
> synchronized (lockObject) { count++; }
>
> // 3. Static method — lock on Class object
> static synchronized void increment() { count++; }
> ```
>
> Instance and static locks are independent — `synchronized void m()` and
> `static synchronized void m()` do not block each other. A common
> mistake is assuming they share the same lock.

---

**Q3. What is the difference between `synchronized` and `volatile`?**

> `synchronized` — mutual exclusion + visibility + ordering. Only one thread
> at a time can execute a synchronized block. Used for compound operations
> (read-modify-write) and critical sections:
>
> ```
> synchronized void increment() { count++; } // atomic + visible
> ```
>
> `volatile` — visibility + ordering only. No mutual exclusion. Multiple
> threads can read simultaneously. Used for simple flags and state variables
> where atomicity is not required:
>
> ```
> volatile boolean running = true; // write visible to all threads
> ```
>
> The critical distinction: `volatile int x; x++` is still NOT thread-safe.
> `volatile` makes the write visible but does not make the read-modify-write
> atomic. Use `AtomicInteger` for that:
>
> ```
> AtomicInteger count = new AtomicInteger();
> count.incrementAndGet(); // atomic + visible — no synchronized needed
> ```

---

**Q4. Why must `wait()` and `notify()` be called inside a `synchronized` block?**

> `wait()` and `notify()` are operations on a monitor. A thread must own
> the monitor (hold the lock) to call them. Calling without the lock throws
> `IllegalMonitorStateException`:
>
> ```
> // ❌ No lock — throws IllegalMonitorStateException
> lock.wait();
>
> // ✅ Must hold the lock
> synchronized (lock) {
>     lock.wait(); // releases lock + suspends
> }
> ```
>
> The design is intentional: `wait()` must atomically release the lock and
> suspend. If you could call `wait()` without holding the lock, there would
> be a window between checking the condition and calling `wait()` where
> another thread could call `notify()` — the notification would be missed
> and the waiting thread would wait forever (missed signal problem).

---

**Q5. Why should `wait()` always be called in a `while` loop, not an `if`?**

> Two reasons:
>
> **1. Spurious wakeups** — the JVM specification allows `wait()` to return
> even when no `notify()` was called. This is not a bug — it is permitted
> by the JVM spec to allow efficient OS-level implementations. Without a
> loop, the thread proceeds even though the condition is not satisfied:
>
> ```
> // ❌ if — spurious wakeup causes wrong behaviour
> synchronized (lock) {
>     if (!ready) lock.wait(); // wakes spuriously → proceeds even if not ready
>     process(); // wrong!
> }
>
> // ✅ while — re-checks condition after every wakeup
> synchronized (lock) {
>     while (!ready) lock.wait(); // loop until truly ready
>     process();
> }
> ```
>
> **2. Multiple consumers** — if `notifyAll()` wakes multiple threads, only
> one should proceed. The `while` loop causes the others to go back to
> waiting after the first thread takes the item.

---

**Q6. What is a deadlock? What are the four conditions required for it?**

> A deadlock is a situation where two or more threads are permanently blocked,
> each waiting for a resource held by the other.
>
> All four conditions must hold simultaneously:
> 1. **Mutual exclusion** — resources cannot be shared (locks)
> 2. **Hold and wait** — a thread holds one lock while waiting for another
> 3. **No preemption** — locks cannot be forcibly taken from a thread
> 4. **Circular wait** — Thread A waits for B, Thread B waits for A
>
> ```
> // Classic deadlock
> Thread 1: synchronized(A) { synchronized(B) { ... } } // holds A, wants B
> Thread 2: synchronized(B) { synchronized(A) { ... } } // holds B, wants A
> // → neither can proceed
> ```
>
> Break any one condition to prevent deadlock. The easiest in practice:
> **break circular wait** by enforcing a consistent lock acquisition order
> across all threads.

---

**Q7. What is the happens-before relationship in the Java Memory Model?**

> Happens-before is a guarantee that if action A happens-before action B,
> then the results of A are visible to B. It is the JMM's formal way of
> defining visibility across threads.
>
> Key happens-before rules:
> - **Monitor lock** — unlock of a `synchronized` block happens-before every subsequent lock of that same monitor
> - **volatile write** — a write to a volatile variable happens-before every subsequent read of that variable
> - **Thread start** — `thread.start()` happens-before any action in the started thread
> - **Thread join** — all actions in a thread happen-before `thread.join()` returns
> - **Program order** — within a single thread, each action happens-before the next
>
> ```
> // Without happens-before — Thread B may not see x = 1
> int x = 0;
> // Thread A: x = 1;
> // Thread B: System.out.println(x); // may print 0
>
> // With volatile — write happens-before read
> volatile int x = 0;
> // Thread A: x = 1;     // volatile write
> // Thread B: System.out.println(x); // guaranteed to see 1
> ```

---

**Q8. What is double-checked locking and why does it require `volatile`?**

> Double-checked locking is a pattern for lazy singleton initialisation that
> avoids synchronisation overhead after the first creation:
>
> ```
> private static volatile Singleton instance;
>
> static Singleton getInstance() {
>     if (instance == null) {               // 1st check — no lock, fast path
>         synchronized (Singleton.class) {
>             if (instance == null) {       // 2nd check — with lock, safe
>                 instance = new Singleton();
>             }
>         }
>     }
>     return instance;
> }
> ```
>
> `volatile` is required because `instance = new Singleton()` is not atomic.
> It compiles to roughly three steps:
> 1. Allocate memory
> 2. Initialise the object
> 3. Assign reference to `instance`
>
> Without `volatile`, the CPU or compiler can reorder steps 2 and 3 — another
> thread could see a non-null `instance` that points to an uninitialised
> object (partial construction). `volatile` prevents this reordering.
>
> Without `volatile`, double-checked locking is broken. This was a famous
> Java concurrency bug before Java 5 formalised the memory model.

---

**Q9. What is a livelock? How is it different from a deadlock?**

> In a **deadlock** threads are blocked and do nothing — they wait forever.
>
> In a **livelock** threads are active and responding to each other but making
> no progress — they keep changing state in response to each other without
> ever completing their work:
>
> ```
> // Livelock example — two threads politely backing off for each other
> while (true) {
>     if (otherThreadWants) {
>         myWant = false;   // back off
>         Thread.sleep(10);
>         myWant = true;    // try again
>     }
>     // both keep backing off for each other — neither proceeds
> }
> ```
>
> Common in retry logic without randomised backoff — both threads retry
> at the same interval and collide repeatedly.
>
> Fix: add random jitter to retry delays so threads don't synchronise
> their retries. `java.util.concurrent` locks use this approach internally.

---

**Q10. How do you detect a deadlock in a production JVM?**

> Three approaches:
>
> **1. Thread dump via jstack:**
> ```bash
> jstack <pid>
> # Look for: "Found one Java-level deadlock"
> # Shows circular "waiting to lock" chains with thread names and stack traces
> ```
>
> **2. ThreadMXBean programmatically:**
> ```
> ThreadMXBean bean = ManagementFactory.getThreadMXBean();
> long[] deadlocked = bean.findDeadlockedThreads();
> if (deadlocked != null) {
>     ThreadInfo[] info = bean.getThreadInfo(deadlocked);
>     for (ThreadInfo ti : info) {
>         System.out.println(ti.getThreadName() +
>             " waiting on " + ti.getLockName());
>     }
> }
> ```
>
> **3. APM tools** — Datadog, New Relic, AppDynamics automatically detect
> and alert on deadlocks in production by monitoring thread states.
>
> Once detected: identify the circular lock chain from the thread dump,
> apply consistent lock ordering or switch to `tryLock` with timeout,
> and redeploy. For immediate recovery: restart the affected JVM instance
> — deadlocked threads cannot be unblocked without intervention.