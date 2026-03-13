# ⚛️ Atomic Variables

> Thread-safe operations without locks — using hardware-level CAS
> (Compare-And-Swap) instructions. Faster than `synchronized` for
> single-variable updates under low-to-medium contention.

---

## 🧠 Core Idea — CAS

CAS is a single atomic CPU instruction:

- Read current value
- Compare with expected
- If match → swap to new value, return true
- If no match → do nothing, return false

No lock acquired. No thread suspended. The operation either succeeds or the
caller retries. This is the foundation of all atomic classes.

---

## 📄 Classes in this Module

### `AtomicSamples.java`

| Example | What it covers |
|---------|----------------|
| AtomicInteger | Basic ops, CAS, updateAndGet, thread-safe counter |
| AtomicReference | Lock-free object update, lazy init, immutable swap pattern |
| ABA Problem | Why CAS can be fooled, AtomicStampedReference fix |
| LongAdder | Striped counter, vs AtomicLong under contention, LongAccumulator |
| Senior Level | Lock-free stack, spin lock, AtomicIntegerArray histogram |

---

## ⚡ AtomicInteger / AtomicLong

```
AtomicInteger counter = new AtomicInteger(0);

counter.get()                    // read
counter.set(10)                  // write
counter.incrementAndGet()        // ++i — returns new value
counter.getAndIncrement()        // i++ — returns old value
counter.addAndGet(5)             // add and return new value
counter.getAndAdd(5)             // add and return old value

// CAS — core operation
counter.compareAndSet(10, 20)    // if current==10, set to 20, return true

// Functional updates — atomic
counter.updateAndGet(v -> v * 2)             // apply function, return new value
counter.accumulateAndGet(3, Integer::sum)    // combine with value, return new value
```

---

## ⚡ AtomicReference

```
AtomicReference<Config> ref = new AtomicReference<>(initial);

ref.get()                          // read current reference
ref.set(newConfig)                 // write — uses reference equality
ref.compareAndSet(expected, next)  // CAS — == comparison, not equals()

// Lock-free immutable object update — read → build new → CAS → retry
Config current, updated;
do {
    current = ref.get();
    updated = new Config(current.host, current.port + 1);
} while (!ref.compareAndSet(current, updated));

// Lazy initialisation
AtomicReference<String> lazy = new AtomicReference<>(null);
lazy.compareAndSet(null, "value"); // only first call sets — rest are no-ops
```

**Important:** CAS uses `==` (reference equality), not `.equals()`. Two
different objects with the same content are NOT considered equal by CAS.

---

## ⚡ ABA Problem

CAS can be fooled when a value changes A → B → A between a read and a swap:

```
Thread 1 reads: A
Thread 2 changes: A → B → A
Thread 1 CAS(A, newVal): succeeds — value looks unchanged but state has changed
```

**Fix: `AtomicStampedReference`** — pairs the reference with an integer stamp
(version counter). Both value and stamp must match for CAS to succeed:

```
AtomicStampedReference<Integer> ref = new AtomicStampedReference<>(1, 0);

// Read value and stamp together
int[] stamp = new int[1];
Integer val = ref.get(stamp);       // val=1, stamp[0]=0

// CAS requires both value and stamp to match
ref.compareAndSet(1, 2, 0, 1);     // val 1→2, stamp 0→1
ref.compareAndSet(2, 1, 1, 2);     // val 2→1, stamp 1→2

// Thread 1 CAS fails — stamp changed even though value is same
ref.compareAndSet(1, 99, 0, 1);    // fails — current stamp is 2, not 0
```

---

## ⚡ LongAdder vs AtomicLong

```
// AtomicLong — all threads CAS the same memory cell
// Under high contention: many failures → retry loops → slow
AtomicLong counter = new AtomicLong();
counter.incrementAndGet();

// LongAdder — striped counter
// Maintains base + array of cells, one per thread (approximately)
// Each thread updates its own cell — minimal contention
// sum() = base + all cells — slightly stale under concurrency
LongAdder adder = new LongAdder();
adder.increment();
adder.add(5);
long total = adder.sum();    // combine all cells
adder.reset();               // reset to 0
long sumAndReset = adder.sumThenReset();

// LongAccumulator — generalised LongAdder with custom function
LongAccumulator max = new LongAccumulator(Long::max, Long.MIN_VALUE);
max.accumulate(42);
max.accumulate(99);
max.get(); // 99
```

**When to use which:**
- `AtomicLong` — low contention, or when you need `compareAndSet`
- `LongAdder` — high contention write-heavy counter (metrics, hit counts)
- `LongAccumulator` — high contention with a custom combine function (max, min, product)

---

## ⚡ AtomicBoolean

```
AtomicBoolean flag = new AtomicBoolean(false);

flag.get()                          // read
flag.set(true)                      // write
flag.compareAndSet(false, true)     // CAS — returns true if swapped

// Common pattern: one-time initialisation gate
AtomicBoolean initialised = new AtomicBoolean(false);
if (initialised.compareAndSet(false, true)) {
    init(); // only one thread executes this
}
```

---

## 🔑 Choosing the Right Tool

| Need | Use |
|------|-----|
| Thread-safe int/long counter | `AtomicInteger` / `AtomicLong` |
| High-contention counter | `LongAdder` |
| High-contention max/min | `LongAccumulator` |
| Lock-free object swap | `AtomicReference` |
| ABA-safe object swap | `AtomicStampedReference` |
| One-time flag | `AtomicBoolean` |
| Per-slot int array | `AtomicIntegerArray` |

---

## 🔑 Common Mistakes

```
// ❌ Assuming get() + set() is atomic — it is not
if (ref.get() == null) {        // check
    ref.set(new Object());      // act — race window between check and set
}
// ✅ Use compareAndSet
ref.compareAndSet(null, new Object()); // atomic

// ❌ Using equals() logic with AtomicReference CAS
// CAS uses == not equals() — two new String("same") are different references
AtomicReference<String> r = new AtomicReference<>(new String("hello"));
r.compareAndSet(new String("hello"), "world"); // FAILS — different reference

// ✅ CAS works on same reference
String current = r.get();
r.compareAndSet(current, "world"); // works — same reference

// ❌ Using AtomicLong as a high-throughput counter under heavy contention
// Under 8+ threads hammering one AtomicLong → CAS retry storms → slow
// ✅ Use LongAdder for high-contention counters

// ❌ Spin lock on long operations — wastes CPU
SpinLock spin = new SpinLock();
spin.lock();
doSlowNetworkCall(); // burns CPU while waiting
spin.unlock();
// ✅ Spin locks only for nanosecond-level critical sections
```

---