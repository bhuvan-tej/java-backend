# 🔐 Locks

> Explicit locks from `java.util.concurrent.locks` — more flexible than
> `synchronized` when you need timeouts, interruptibility, fairness,
> or read/write separation.

---

## 🧠 Why Locks Over synchronized?

- `synchronized` — no timeout, no interruption, no fairness, no read/write split
- `ReentrantLock` — tryLock with timeout, interruptible waiting, fairness control
- `ReadWriteLock` — multiple concurrent readers OR one exclusive writer
- `StampedLock` — optimistic reads for read-heavy workloads (Java 8+)
- `Condition` — multiple wait sets per lock, cleaner than wait/notify

**Golden rule: always unlock in a `finally` block — no exceptions.**

---

## 📄 Classes in this Module

### `LockSamples.java`

| Example | What it covers |
|---------|----------------|
| ReentrantLock | Basic usage, reentrant behaviour, fairness, diagnostics |
| tryLock | Non-blocking acquire, timeout, lockInterruptibly |
| ReadWriteLock | Concurrent readers, exclusive writer, when to use |
| Condition | Bounded buffer, multiple wait sets, signal vs signalAll |
| StampedLock | Optimistic read, write lock, read-to-write conversion |

---

## ⚡ ReentrantLock

```
ReentrantLock lock = new ReentrantLock();

lock.lock();
try {
    // critical section
} finally {
    lock.unlock(); // ALWAYS in finally
}

// Fairness — longest-waiting thread gets lock first
ReentrantLock fairLock = new ReentrantLock(true);

// Diagnostics
lock.isLocked()        // is any thread holding this lock?
lock.getHoldCount()    // how many times current thread has locked
lock.getQueueLength()  // how many threads are waiting
lock.isFair()          // is this a fair lock?
```

---

## ⚡ tryLock

```
// Non-blocking — returns immediately
if (lock.tryLock()) {
    try { ... }
    finally { lock.unlock(); }
} else {
    // lock not available — do something else
}

// With timeout — wait up to N ms, then give up
if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
    try { ... }
    finally { lock.unlock(); }
} else {
    // timed out — deadlock avoided
}

// Interruptible — waiting thread can be cancelled
lock.lockInterruptibly(); // throws InterruptedException if interrupted while waiting
```

---

## ⚡ ReadWriteLock

```
ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
Lock readLock  = rwLock.readLock();
Lock writeLock = rwLock.writeLock();

// Multiple threads can hold read lock simultaneously
readLock.lock();
try { readData(); }
finally { readLock.unlock(); }

// Write lock is exclusive — blocks all readers and writers
writeLock.lock();
try { writeData(); }
finally { writeLock.unlock(); }
```

**When to use:**
- Read-heavy workloads — cache, config, reference data
- Writes are rare but must be exclusive
- Not worth it for write-heavy or very short read operations — lock overhead dominates

---

## ⚡ Condition

```
// One lock, two wait sets — producer and consumer wait separately
ReentrantLock lock     = new ReentrantLock();
Condition notFull      = lock.newCondition(); // producer waits here when buffer full
Condition notEmpty     = lock.newCondition(); // consumer waits here when buffer empty

// Producer
lock.lock();
try {
    while (isFull()) notFull.await();  // wait — releases lock
    add(item);
    notEmpty.signal();                 // wake one consumer
} finally { lock.unlock(); }

// Consumer
lock.lock();
try {
    while (isEmpty()) notEmpty.await(); // wait — releases lock
    T item = remove();
    notFull.signal();                   // wake one producer
} finally { lock.unlock(); }
```

**Condition vs wait/notify:**
- `wait/notify` — one wait set per object monitor
- `Condition` — multiple wait sets per lock, more precise signalling
- `signal()` wakes one thread from that Condition's wait set only
- `signalAll()` wakes all — safer when multiple threads may satisfy condition

---

## ⚡ StampedLock

```
StampedLock sl = new StampedLock();

// Write lock
long stamp = sl.writeLock();
try { write(); }
finally { sl.unlockWrite(stamp); }

// Read lock
long stamp = sl.readLock();
try { read(); }
finally { sl.unlockRead(stamp); }

// Optimistic read — no lock, just a stamp — fastest path
long stamp = sl.tryOptimisticRead();
int x = readX();
int y = readY();
if (!sl.validate(stamp)) {
    // a write happened — fall back to real read lock
    stamp = sl.readLock();
    try { x = readX(); y = readY(); }
    finally { sl.unlockRead(stamp); }
}
```

**StampedLock vs ReadWriteLock:**
- Optimistic read avoids lock acquisition entirely — much faster under low write contention
- StampedLock is **NOT reentrant** — same thread cannot lock twice
- No Condition support
- Best fit: read-heavy, rarely written data (coordinates, config, metrics)

---

## 🔑 Common Mistakes

```
// ❌ Unlock outside finally — exception skips unlock → lock held forever
lock.lock();
doWork();          // throws exception
lock.unlock();     // never reached!

// ✅ Always finally
lock.lock();
try { doWork(); }
finally { lock.unlock(); }

// ❌ tryLock without checking return value
lock.tryLock();    // return value ignored — may not hold the lock!
doWork();

// ✅ Check the return value
if (lock.tryLock()) {
    try { doWork(); }
    finally { lock.unlock(); }
}

// ❌ StampedLock — same thread locking twice (not reentrant)
long s1 = sl.readLock();
long s2 = sl.readLock(); // deadlock — StampedLock is not reentrant!

// ❌ Using 'if' instead of 'while' with Condition.await()
if (isEmpty()) notEmpty.await(); // spurious wakeup → wrong behaviour

// ✅ Always while
while (isEmpty()) notEmpty.await();
```

---

## 🔑 Lock Comparison

| | synchronized | ReentrantLock | ReadWriteLock | StampedLock |
|---|---|---|---|---|
| Timeout | ❌ | ✅ tryLock | ✅ tryLock | ✅ |
| Interruptible | ❌ | ✅ | ✅ | ✅ |
| Fairness | ❌ | ✅ | ✅ | ❌ |
| Reentrant | ✅ | ✅ | ✅ | ❌ |
| Read/Write split | ❌ | ❌ | ✅ | ✅ |
| Optimistic read | ❌ | ❌ | ❌ | ✅ |
| Condition support | wait/notify | ✅ | ✅ | ❌ |

---