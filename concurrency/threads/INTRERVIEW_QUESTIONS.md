# 🎯 Interview Questions — Threads

---

**Q1. What is the difference between calling `run()` and `start()` on a Thread?**

> `start()` creates a new OS thread and schedules it for execution. The JVM
> calls `run()` on the new thread when the scheduler picks it up.
>
> `run()` called directly executes synchronously on the **calling thread** —
> no new thread is created at all:
>
> ```
> Thread t = new Thread(() -> System.out.println(Thread.currentThread().getName()));
>
> t.run();   // prints "main"   — ran on main thread, no new thread
> t.start(); // prints "Thread-0" — ran on new thread
> ```
>
> This is one of the most common beginner mistakes. The code runs either way —
> it just runs in the wrong place. In production this means no parallelism
> and potential blocking of the calling thread.

---

**Q2. Explain the full Thread lifecycle and what causes each transition.**

> ```
> NEW → RUNNABLE   : start() called
> RUNNABLE → BLOCKED        : thread tries to enter a synchronized block
>                             held by another thread
> RUNNABLE → WAITING        : wait(), join() with no timeout called
> RUNNABLE → TIMED_WAITING  : sleep(ms), wait(ms), join(ms) called
> BLOCKED → RUNNABLE        : the monitor lock becomes available
> WAITING → RUNNABLE        : notify(), notifyAll() called, or join completes
> TIMED_WAITING → RUNNABLE  : timeout expires, or notify received
> any → TERMINATED          : run() returns or throws uncaught exception
> ```
>
> Key distinction at interviews: `BLOCKED` is specifically waiting for a
> **monitor lock** (synchronized). `WAITING` is waiting for an explicit
> signal (wait/notify). These are different states and different mechanisms.

---

**Q3. How does `interrupt()` work? Why is it called cooperative cancellation?**

> `interrupt()` does not stop a thread. It sets a boolean flag on the
> target thread. The thread itself is responsible for checking the flag
> and deciding to stop:
>
> ```
> // Checking the flag in a loop
> while (!Thread.currentThread().isInterrupted()) {
>     doWork();
> }
>
> // When blocked in sleep/wait/join — InterruptedException is thrown
> // AND the flag is cleared automatically
> try {
>     Thread.sleep(1000);
> } catch (InterruptedException e) {
>     Thread.currentThread().interrupt(); // restore the flag — important!
>     return; // cooperate — stop cleanly
> }
> ```
>
> It is "cooperative" because the interrupted thread must opt in — it can
> choose to ignore the flag. This is by design: the thread can finish
> current work, release locks, and clean up before stopping. Forceful
> termination (like `Thread.stop()`) was deprecated because it could leave
> shared state corrupted.
>
> The most important rule: **always restore the interrupt flag** after
> catching `InterruptedException`. `InterruptedException` clears the flag —
> if you swallow the exception, the interrupt signal is permanently lost.

---

**Q4. What is a daemon thread? How is it different from a user thread?**

> A daemon thread is a background thread that the JVM does not wait for
> before exiting. When all user (non-daemon) threads finish, the JVM
> terminates — killing all daemon threads immediately, even mid-execution.
>
> ```
> Thread daemon = new Thread(() -> {
>     while (true) { heartbeat(); sleep(1000); }
> });
> daemon.setDaemon(true); // must be set BEFORE start()
> daemon.start();
> // When main thread exits, JVM kills daemon — no cleanup runs
> ```
>
> Use daemon threads for: GC, log flushing, connection pool eviction,
> health checks, background cleanup — tasks where abrupt termination
> on JVM exit is acceptable.
>
> Do NOT use daemon threads for: DB writes, file I/O, anything requiring
> a clean shutdown — they get killed without running finally blocks.

---

**Q5. What is `ThreadLocal` and what is the memory leak risk?**

> `ThreadLocal<T>` gives each thread its own independent copy of a variable.
> Threads cannot see each other's values — no synchronisation needed:
>
> ```
> ThreadLocal<String> requestId = new ThreadLocal<>();
> requestId.set("REQ-123"); // only visible to current thread
> requestId.get();          // returns "REQ-123" on this thread, null on others
> ```
>
> Common uses: request-scoped context, MDC logging, per-thread DB connections,
> SimpleDateFormat instances (pre-Java 8).
>
> **Memory leak in thread pools**: thread pool threads are reused, not
> destroyed. If a thread sets a `ThreadLocal` value and returns to the pool
> without calling `remove()`, the value stays attached to the thread —
> the next task on the same thread sees stale data, and the object is never
> GC'd as long as the thread lives:
>
> ```
> // ❌ Leak — thread returns to pool with value attached
> threadLocal.set(largeObject);
> doWork();
> // returns to pool — largeObject held forever
>
> // ✅ Always remove in finally
> try {
>     threadLocal.set(largeObject);
>     doWork();
> } finally {
>     threadLocal.remove(); // critical in thread pools
> }
> ```

---

**Q6. What is the difference between `BLOCKED` and `WAITING` states?**

> Both mean the thread is not running, but the cause and resolution differ:
>
> `BLOCKED` — thread is waiting to **acquire a monitor lock** for a
> `synchronized` block or method. It will automatically become `RUNNABLE`
> when the lock is released by the holding thread. No explicit notification needed:
> ```
> synchronized (lock) { ... } // another thread holds lock → BLOCKED
> ```
>
> `WAITING` — thread is waiting for an **explicit signal** from another
> thread. It will stay in WAITING indefinitely until `notify()`,
> `notifyAll()`, or `LockSupport.unpark()` is called:
> ```
> synchronized (lock) {
>     lock.wait(); // → WAITING until lock.notify() called
> }
> ```
>
> Practical implication: a deadlock produces threads stuck in `BLOCKED`.
> A missed `notify()` produces threads stuck in `WAITING`. Thread dumps
> distinguish these clearly — which is why knowing the difference matters
> for diagnosing production issues.

---

**Q7. Why should you always name your threads?**

> Default thread names — `Thread-0`, `Thread-1` — are useless in production
> logs and thread dumps. When diagnosing a deadlock or performance issue,
> the thread name is the first thing you look at:
>
> ```
> // Unreadable thread dump
> "Thread-14" waiting for lock 0x000000076b880c08
>
> // Readable thread dump
> "order-processor-14" waiting for lock 0x000000076b880c08
> ```
>
> Always name threads — either directly or via a `ThreadFactory`:
> ```
> // Direct
> new Thread(task, "payment-worker-1");
>
> // ThreadFactory for thread pools
> ThreadFactory factory = r -> {
>     Thread t = new Thread(r);
>     t.setName("order-processor-" + t.getId());
>     t.setDaemon(true);
>     return t;
> };
> ExecutorService pool = Executors.newFixedThreadPool(4, factory);
> ```
>
> In Spring Boot: `ThreadPoolTaskExecutor` has `setThreadNamePrefix()`.
> In production: thread names appear in logs, APM tools, and JVM thread dumps.

---

**Q8. What happens if an exception is thrown inside a thread's `run()` method?**

> An unchecked exception thrown from `run()` terminates that thread.
> By default the stack trace is printed to `System.err` and the exception
> is silently swallowed — the rest of your application keeps running,
> and you may never know the thread died:
>
> ```
> Thread t = new Thread(() -> {
>     throw new RuntimeException("something failed");
> });
> t.start();
> // Stack trace printed to stderr — easy to miss in production logs
> ```
>
> In production, always set an `UncaughtExceptionHandler` to log properly,
> trigger alerts, or restart the thread:
> ```
> t.setUncaughtExceptionHandler((thread, ex) -> {
>     log.error("Thread {} died unexpectedly", thread.getName(), ex);
>     alerting.trigger("thread-death", thread.getName());
> });
>
> // Or set a default for all threads
> Thread.setDefaultUncaughtExceptionHandler((thread, ex) ->
>     log.error("Unhandled exception in {}", thread.getName(), ex));
> ```
>
> Note: checked exceptions cannot be thrown from `run()` — the `Runnable`
> interface declares no checked exceptions. Use `Callable` instead if
> you need to propagate checked exceptions.