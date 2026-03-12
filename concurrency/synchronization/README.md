# 🔒 Synchronization

> Coordinating access to shared state across multiple threads.
> Without it: race conditions, stale reads, data corruption.

---

## 🧠 The Three Problems

- **Atomicity** — `count++` is 3 steps (read, add, write). Two threads can interleave and lose an update.
- **Visibility** — A write on Thread A may sit in CPU cache. Thread B may never see it.
- **Ordering** — The compiler and CPU reorder instructions for performance. This can break assumptions across threads.

---

## 🛠 Tools and What They Fix

| Tool | Atomicity | Visibility | Ordering | Cost |
|------|-----------|------------|----------|------|
| `synchronized` | ✅ | ✅ | ✅ | Heavy — one thread at a time |
| `volatile` | ❌ | ✅ | ✅ | Light — no mutual exclusion |
| `AtomicInteger` | ✅ | ✅ | ✅ | Light — CAS hardware instruction |

---

## 📄 Classes in this Module

### `SynchronizationSamples.java`

| Example | What it covers |
|---------|----------------|
| Race Condition | What goes wrong without synchronization — lost updates |
| synchronized | Method lock, block lock, static lock, reentrant behaviour |
| volatile | Visibility fix, double-checked locking, what volatile does NOT fix |
| wait / notify | Producer-consumer pattern, spurious wakeups, lock release on wait |
| Deadlock | How it happens, consistent lock ordering fix, detection with jstack |

---

## ⚡ synchronized

```
// ── Method — lock on 'this' ──────────────────────────────────
class Counter {
    private int count = 0;
    synchronized void increment() { count++; }   // one thread at a time
    synchronized int getCount()   { return count; }
}

// ── Block — finer granularity ─────────────────────────────────
synchronized (lockObject) {
    // only critical section locked
    // rest of method runs without lock
}

// ── Static — lock on Class object ─────────────────────────────
static synchronized void increment() { count++; } // locks on MyClass.class

// ── Reentrant — same thread can re-acquire its own lock ───────
synchronized void outer() {
    inner(); // calls another synchronized method — no deadlock
}
synchronized void inner() { ... }
```

---

## ⚡ volatile

```
// ✅ Visibility — write immediately visible to all threads
volatile boolean running = true;

// ✅ Ordering — prevents reordering across read/write
// ✅ Singleton double-checked locking
private static volatile Singleton instance;
static Singleton getInstance() {
    if (instance == null) {
        synchronized (Singleton.class) {
            if (instance == null) {
                instance = new Singleton(); // volatile prevents partial construction
            }
        }
    }
    return instance;
}

// ❌ NOT atomic — volatile int x; x++ still has race condition
// Use AtomicInteger for compound operations
```

---

## ⚡ wait / notify

```
// Must be called inside synchronized block — always
synchronized (lock) {
    while (!condition) {     // while loop — not if — guards spurious wakeups
        lock.wait();         // releases lock + suspends thread
    }
    // condition is now true — proceed
}

// Producer side
synchronized (lock) {
    condition = true;
    lock.notify();           // wake ONE waiting thread
    // lock.notifyAll()      // wake ALL waiting threads — safer
}
```

**Rules:**
- `wait()` / `notify()` must be inside `synchronized` on the same object — `IllegalMonitorStateException` otherwise
- Always use `while` not `if` — spurious wakeups can occur
- `wait()` releases the lock — `notify()` does not
- Prefer `notifyAll()` over `notify()` unless exactly one thread should wake

---

## 🔑 Deadlock

**Four conditions (all must hold):**
- Mutual exclusion — only one thread holds a resource
- Hold and wait — holding one lock while waiting for another
- No preemption — locks cannot be forcibly taken
- Circular wait — Thread A waits for B, Thread B waits for A

**Prevention:**
```
// ✅ Fix 1 — consistent lock ordering
// Always acquire locks in the same order across all threads
synchronized (lockA) {
    synchronized (lockB) { ... }
}

// ✅ Fix 2 — tryLock with timeout (ReentrantLock)
if (lockA.tryLock(100, TimeUnit.MILLISECONDS)) {
    try {
        if (lockB.tryLock(100, TimeUnit.MILLISECONDS)) {
            try { ... } finally { lockB.unlock(); }
        }
    } finally { lockA.unlock(); }
}
```

**Detection:** `jstack <pid>` or VisualVM → thread dump → look for circular `waiting to lock` chains.

---

## 🔑 Common Mistakes

```
// ❌ Synchronizing on a different object per instance — no mutual exclusion
synchronized (new Object()) { ... } // each call locks a different object!

// ❌ Using 'if' instead of 'while' for wait()
if (!ready) lock.wait(); // spurious wakeup → proceeds even if not ready

// ✅ Always while
while (!ready) lock.wait();

// ❌ volatile for compound operations
volatile int count;
count++; // read-modify-write — still NOT atomic

// ✅ Use AtomicInteger
AtomicInteger count = new AtomicInteger();
count.incrementAndGet(); // atomic

// ❌ Calling wait/notify without holding the lock
lock.notify(); // IllegalMonitorStateException
```

---