# 🎯 Interview Questions — Atomic Variables

---

**Q1. What is Compare-And-Swap (CAS) and how does it enable lock-free programming?**

> CAS is a single atomic CPU instruction supported by modern hardware
> (x86: `CMPXCHG`, ARM: `LDREX/STREX`). It takes three operands —
> a memory location, an expected value, and a new value:
>
> - If current value == expected → atomically set to new value, return true
> - If current value != expected → do nothing, return false
>
> Because it is a single hardware instruction, no lock is needed —
> no thread can observe a partial state. The JVM exposes this via
> `Unsafe.compareAndSwapInt()` internally, and the atomic classes
> wrap this in a safe API:
>
> ```
> AtomicInteger counter = new AtomicInteger(0);
> counter.compareAndSet(0, 1); // hardware CAS instruction
> ```
>
> Lock-free algorithms built on CAS follow the optimistic pattern:
> read current value, compute new value, CAS. If CAS fails (another
> thread changed the value), retry. No thread is ever blocked — at
> worst it retries. This means no deadlocks, no context switches,
> no OS kernel involvement.

---

**Q2. What is the difference between `getAndIncrement()` and `incrementAndGet()`?**

> Both atomically increment the value by 1, but differ in what they return:
>
> `getAndIncrement()` — returns the value **before** increment (post-increment, like `i++`):
> ```
> AtomicInteger a = new AtomicInteger(5);
> int old = a.getAndIncrement(); // old=5, a is now 6
> ```
>
> `incrementAndGet()` — returns the value **after** increment (pre-increment, like `++i`):
> ```
> AtomicInteger a = new AtomicInteger(5);
> int newVal = a.incrementAndGet(); // newVal=6, a is now 6
> ```
>
> The same pattern applies to all paired operations:
> `getAndAdd` vs `addAndGet`, `getAndSet` vs `set` + `get`.
>
> In practice: use `incrementAndGet()` when you need the new value
> (sequence generators, counters). Use `getAndIncrement()` when you
> need the old value (array index allocation, ticket dispensers).

---

**Q3. What is the ABA problem? How does `AtomicStampedReference` solve it?**

> The ABA problem occurs when a value changes from A to B and back to A
> between a thread's read and its CAS. The CAS succeeds because the value
> looks unchanged, but the state has actually changed underneath:
>
> ```
> Thread 1: reads value = A, prepares CAS(A, newVal)
> Thread 2: A → B → A   (value cycles back)
> Thread 1: CAS(A, newVal) succeeds — value looked stable but wasn't
> ```
>
> This matters in lock-free data structures. A popped-then-reused node
> in a lock-free stack could cause Thread 1 to corrupt the stack by
> swapping in a stale next pointer.
>
> `AtomicStampedReference` solves this by pairing the reference with
> an integer stamp (version counter). Both must match for CAS to succeed:
>
> ```
> AtomicStampedReference<Node> ref = new AtomicStampedReference<>(nodeA, 0);
>
> // Thread 2 changes A→B→A but stamps increment: 0→1→2
> // Thread 1's CAS(A, newNode, stamp=0, 1) fails — stamp is now 2
> ```
>
> Every successful CAS increments the stamp, so even if the value returns
> to A, the stamp is different — Thread 1 detects the change and retries
> with fresh data.

---

**Q4. When would you use `LongAdder` instead of `AtomicLong`?**

> `AtomicLong` stores a single value. All threads CAS the same memory
> location. Under high contention, many threads fail their CAS and spin
> in retry loops — this is called a CAS storm and causes significant
> throughput degradation:
>
> ```
> 8 threads all try CAS on same location → 7 fail → retry → 6 fail → ...
> ```
>
> `LongAdder` avoids contention by striping — it maintains a base value
> plus an array of cells. Under contention, threads are distributed across
> cells and each updates its own cell. `sum()` combines all cells:
>
> ```
> LongAdder adder = new LongAdder();
> adder.increment(); // updates thread-local cell — no contention
> adder.sum();       // base + all cells — slightly stale but accurate enough
> ```
>
> Use `LongAdder` when:
> - High write throughput is needed (metrics, request counters, hit counts)
> - Exact real-time value is not required — `sum()` may be slightly stale
>
> Use `AtomicLong` when:
> - Low-to-medium contention
> - You need `compareAndSet` — `LongAdder` does not support CAS
> - You need an exact, immediately consistent value

---

**Q5. What does `updateAndGet` do and why is it better than a manual CAS loop?**

> `updateAndGet(UnaryOperator<V> fn)` atomically applies a function to the
> current value and updates it, retrying internally if CAS fails:
>
> ```
> // Manual CAS loop — verbose and error-prone
> int current, next;
> do {
>     current = counter.get();
>     next    = current * 2;
> } while (!counter.compareAndSet(current, next));
>
> // updateAndGet — same thing, one line
> counter.updateAndGet(v -> v * 2);
> ```
>
> Both are equivalent — `updateAndGet` just encapsulates the retry loop.
> It is safer because:
> - Less boilerplate — fewer chances to get the loop wrong
> - The function is called with the latest value on each retry — correct by design
> - Works for any transformation, not just arithmetic
>
> The function passed to `updateAndGet` must be side-effect-free because
> it may be called multiple times if CAS fails under contention.

---

**Q6. What is a spin lock? When is it appropriate and when is it harmful?**

> A spin lock is a lock that busy-waits — instead of blocking the thread
> (suspending it via OS), it loops in a tight CAS loop until the lock is
> acquired:
>
> ```
> class SpinLock {
>     private final AtomicBoolean locked = new AtomicBoolean(false);
>
>     void lock() {
>         while (!locked.compareAndSet(false, true)) {
>             Thread.onSpinWait(); // CPU pause hint — reduces power, improves perf
>         }
>     }
>
>     void unlock() { locked.set(false); }
> }
> ```
>
> Appropriate when:
> - Critical section is nanoseconds long — shorter than OS context switch cost (~1-10µs)
> - Lock is almost never contended
> - Running on a multi-core machine — single core spinning wastes the only CPU
>
> Harmful when:
> - Critical section does any I/O, sleeps, or calls external code
> - Many threads contend — CPUs burn doing nothing useful
> - Running on a single core — spinning prevents the lock holder from running
>
> In the JVM: `Thread.onSpinWait()` (Java 9+) emits a CPU `PAUSE` instruction
> which reduces power consumption and avoids memory order violations during
> the spin loop.

---

**Q7. What is `AtomicReference` and what is the key gotcha with its CAS?**

> `AtomicReference<T>` allows atomic read, write, and CAS operations on
> an object reference. It is used to build lock-free data structures and
> perform atomic swaps of immutable objects:
>
> ```
> AtomicReference<Config> config = new AtomicReference<>(current);
>
> // Atomic swap — safe under concurrent access
> Config expected = config.get();
> Config updated  = new Config(expected.host, newPort);
> config.compareAndSet(expected, updated); // retried if stale
> ```
>
> The key gotcha: **CAS uses reference equality (`==`), not `.equals()`**.
> Two different object instances with identical content are NOT equal
> for CAS purposes:
>
> ```
> AtomicReference<String> r = new AtomicReference<>(new String("hello"));
>
> // FAILS — different object instance even though content is same
> r.compareAndSet(new String("hello"), "world");
>
> // WORKS — same reference
> String current = r.get();
> r.compareAndSet(current, "world");
> ```
>
> This also means: never use `AtomicReference` with objects that are
> frequently re-created with equal content — CAS will fail unexpectedly.
> Use `AtomicStampedReference` or redesign to reuse references.