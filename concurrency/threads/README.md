# 🧵 Threads

> The smallest unit of execution in the JVM. Every Java program starts
> with one thread — the main thread. Everything else is built on top.

---

## 🧠 Thread Lifecycle

**States:**

- **NEW** — Thread object created with `new Thread()`. Not yet scheduled.
- **RUNNABLE** — `start()` called. Eligible for CPU. May or may not be actively executing at any moment.
- **BLOCKED** — Trying to enter a `synchronized` block/method held by another thread. Waits automatically until the lock is free.
- **WAITING** — Waiting indefinitely for a signal. Entered via `wait()`, `join()`, or `LockSupport.park()`. Exits only on `notify()` / `notifyAll()` / join completion.
- **TIMED_WAITING** — Same as WAITING but with a timeout. Entered via `sleep(ms)`, `wait(ms)`, `join(ms)`. Exits when timeout expires or notified.
- **TERMINATED** — `run()` returned or threw an uncaught exception. Cannot be restarted.

**Transitions:**

- `new Thread()` → **NEW**
- `thread.start()` → **NEW** becomes **RUNNABLE**
- Tries locked `synchronized` block → **RUNNABLE** becomes **BLOCKED**
- Lock becomes available → **BLOCKED** back to **RUNNABLE**
- `object.wait()` / `thread.join()` → **RUNNABLE** becomes **WAITING**
- `notify()` / `notifyAll()` / join completes → **WAITING** back to **RUNNABLE**
- `Thread.sleep(ms)` / `wait(ms)` / `join(ms)` → **RUNNABLE** becomes **TIMED_WAITING**
- Timeout expires or `notify()` → **TIMED_WAITING** back to **RUNNABLE**
- `run()` returns or throws → **RUNNABLE** becomes **TERMINATED**

**Key distinction:** `BLOCKED` is waiting for a **monitor lock** (synchronized).
`WAITING` is waiting for an **explicit signal** (wait/notify). These are different
states — important for reading thread dumps.

---

## 📄 Classes in this Module

### `ThreadSamples.java`

| Example | What it covers |
|---------|----------------|
| Creating Threads | Extend Thread, Runnable, Callable + FutureTask |
| Thread Lifecycle | State transitions, thread properties |
| join / sleep / interrupt | Waiting for threads, cooperative cancellation |
| Daemon Threads | Background threads, JVM exit behaviour |
| Senior Level | UncaughtExceptionHandler, ThreadLocal, ThreadFactory |

---

## ⚡ Key Methods

```
// ── Creating ──────────────────────────────────────────────────
new Thread(runnable)                    // wrap a Runnable
new Thread(runnable, "thread-name")     // with name — critical for debugging
thread.start()                          // schedule for execution — never call run() directly

// ── Control ───────────────────────────────────────────────────
thread.join()                           // wait for thread to finish
thread.join(200)                        // wait max 200ms
Thread.sleep(100)                       // pause current thread
thread.interrupt()                      // set interrupt flag — cooperative signal
Thread.currentThread().isInterrupted()  // check flag without clearing
Thread.interrupted()                    // check flag AND clear it

// ── State ─────────────────────────────────────────────────────
thread.getState()                       // Thread.State enum
thread.isAlive()                        // true if started and not terminated
thread.isDaemon()                       // true if daemon

// ── Properties ────────────────────────────────────────────────
thread.setName("worker-1")              // set before start for useful logs
thread.setPriority(Thread.NORM_PRIORITY)// 1(MIN) to 10(MAX), default 5
thread.setDaemon(true)                  // must set BEFORE start()

// ── Handlers ──────────────────────────────────────────────────
thread.setUncaughtExceptionHandler((t, ex) -> log(ex))
Thread.setDefaultUncaughtExceptionHandler((t, ex) -> log(ex))
```

---

## ⚡ ThreadLocal

```
// Each thread gets its own independent copy
ThreadLocal<String> requestId = new ThreadLocal<>();

requestId.set("REQ-123");        // set for current thread only
requestId.get();                 // get current thread's value
requestId.remove();              // ALWAYS remove — prevents memory leaks in thread pools

// With initial value
ThreadLocal<List<String>> logs = ThreadLocal.withInitial(ArrayList::new);

// Common uses: request context, user session, DB connection, MDC logging
```

---

## 🔑 Common Mistakes

```
// ❌ Calling run() instead of start() — no new thread created
thread.run();   // executes on calling thread
thread.start(); // ✅ creates new thread

// ❌ Swallowing InterruptedException — signal permanently lost
try { Thread.sleep(100); } catch (InterruptedException e) { } // bad

// ✅ Always restore the interrupt flag
try { Thread.sleep(100); } catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}

// ❌ Setting daemon after start
thread.start();
thread.setDaemon(true); // IllegalThreadStateException!

// ✅ Set before start
thread.setDaemon(true);
thread.start();

// ❌ ThreadLocal leak in thread pools — stale value on reused thread
threadLocal.set(value);
doWork();
// thread returns to pool with value still attached!

// ✅ Always remove in finally
try {
    threadLocal.set(value);
    doWork();
} finally {
    threadLocal.remove();
}
```

---