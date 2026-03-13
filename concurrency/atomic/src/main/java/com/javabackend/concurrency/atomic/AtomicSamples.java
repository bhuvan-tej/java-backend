package com.javabackend.concurrency.atomic;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 *
 * Atomic Variables
 *
 * WHY ATOMICS?
 *  synchronized and locks are heavy — they block threads.
 *  volatile fixes visibility but not atomicity.
 *  Atomics give you thread-safe operations WITHOUT locking —
 *  using hardware-level CAS (Compare-And-Swap) instructions.
 *
 * KEY CLASSES
 *  AtomicInteger / AtomicLong    — lock-free int/long operations
 *  AtomicBoolean                 — lock-free boolean flag
 *  AtomicReference<T>            — lock-free reference updates
 *  AtomicIntegerArray            — lock-free array of ints
 *  AtomicStampedReference<T>     — reference + stamp, solves ABA problem
 *  LongAdder / LongAccumulator   — high-throughput counters (Java 8+)
 *
 * HOW CAS WORKS
 *   compareAndSet(expected, update):
 *   if current == expected → set to update, return true
 *   if current != expected → do nothing,   return false
 *   This is a single atomic CPU instruction — no lock needed
 *
 */
public class AtomicSamples {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("━━━ EXAMPLE 1 — AtomicInteger ━━━\n");
        atomicInteger();

        System.out.println("\n━━━ EXAMPLE 2 — AtomicReference ━━━\n");
        atomicReference();

        System.out.println("\n━━━ EXAMPLE 3 — ABA Problem ━━━\n");
        abaProblem();

        System.out.println("\n━━━ EXAMPLE 4 — LongAdder ━━━\n");
        longAdder();

        System.out.println("\n━━━ EXAMPLE 5 — Senior Level ━━━\n");
        seniorLevel();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — AtomicInteger
    // ─────────────────────────────────────────────
    static void atomicInteger() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);

        // ── Basic operations ──
        System.out.println("  get            : " + counter.get());
        System.out.println("  incrementAndGet: " + counter.incrementAndGet()); // ++i
        System.out.println("  getAndIncrement: " + counter.getAndIncrement()); // i++
        System.out.println("  addAndGet(5)   : " + counter.addAndGet(5));
        System.out.println("  getAndAdd(3)   : " + counter.getAndAdd(3));
        System.out.println("  get after      : " + counter.get());

        // ── compareAndSet — the core CAS operation ──
        counter.set(10);
        boolean swapped = counter.compareAndSet(10, 20); // if 10, set to 20
        System.out.println("  CAS(10→20)     : " + swapped + " value=" + counter.get());

        boolean missed = counter.compareAndSet(10, 30);  // fails — current is 20
        System.out.println("  CAS(10→30)     : " + missed + " value=" + counter.get());

        // ── updateAndGet — apply a function atomically ──
        counter.set(5);
        int doubled = counter.updateAndGet(v -> v * 2);
        System.out.println("  updateAndGet×2 : " + doubled);

        // ── accumulateAndGet — combine with another value ──
        int result = counter.accumulateAndGet(3, Integer::sum);
        System.out.println("  accumulateAndGet+3: " + result);

        // ── Thread-safe counter — no locks ──
        AtomicInteger shared = new AtomicInteger(0);
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10_000; i++) shared.incrementAndGet();
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10_000; i++) shared.incrementAndGet();
        });
        t1.start(); t2.start();
        t1.join();  t2.join();
        System.out.println("  safe count (expected 20000): " + shared.get());
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — AtomicReference
    // ─────────────────────────────────────────────
    static void atomicReference() throws InterruptedException {
        // ── Basic AtomicReference ──
        AtomicReference<String> ref = new AtomicReference<>("initial");

        System.out.println("  get            : " + ref.get());
        ref.set("updated");
        System.out.println("  after set      : " + ref.get());

        // CAS on reference — uses reference equality (==), not equals()
        boolean swapped = ref.compareAndSet("updated", "final");
        System.out.println("  CAS updated→final: " + swapped + " → " + ref.get());

        // ── Lock-free immutable object update ──
        // Pattern: read → create new → CAS → retry on failure
        AtomicReference<Config> configRef = new AtomicReference<>(new Config("host-1", 8080));

        // Simulate concurrent config update
        Thread updater = new Thread(() -> {
            Config current, updated;
            do {
                current = configRef.get();
                updated = new Config(current.host, current.port + 1); // new object
            } while (!configRef.compareAndSet(current, updated)); // retry if stale
            System.out.println("  config updated to port: " + configRef.get().port);
        });

        updater.start();
        updater.join();
        System.out.println("  final config: " + configRef.get());

        // ── AtomicReference for lazy init ──
        // Safe alternative to double-checked locking without volatile
        AtomicReference<String> lazy = new AtomicReference<>(null);
        lazy.compareAndSet(null, "lazily-initialised"); // only first call succeeds
        lazy.compareAndSet(null, "second attempt");     // no-op — already set
        System.out.println("  lazy init: " + lazy.get());
    }

    static record Config(String host, int port) {
        public String toString() { return host + ":" + port; }
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — ABA Problem + AtomicStampedReference
    // ─────────────────────────────────────────────
    static void abaProblem() throws InterruptedException {
        // ── The ABA problem ──
        // Thread 1 reads value A
        // Thread 2 changes A → B → A
        // Thread 1 does CAS(A, newVal) — succeeds even though value changed!
        // The value LOOKS the same but the state has changed underneath

        AtomicInteger value = new AtomicInteger(1);

        // Simulate ABA
        int snapshot = value.get(); // Thread 1 reads: 1

        // Thread 2: 1 → 2 → 1
        value.set(2);
        value.set(1); // back to 1

        // Thread 1 CAS succeeds — unaware that value changed and came back
        boolean result = value.compareAndSet(snapshot, 99);
        System.out.println("  ABA CAS succeeded (shouldn't have): " + result);
        System.out.println("  value: " + value.get());

        // ── Fix: AtomicStampedReference — reference + version stamp ──
        AtomicStampedReference<Integer> stamped =
                new AtomicStampedReference<>(1, 0); // value=1, stamp=0

        int[] stampHolder = new int[1];
        int snapshotVal = stamped.get(stampHolder);
        int snapshotStamp = stampHolder[0];
        System.out.println("  stamped snapshot: val=" + snapshotVal + " stamp=" + snapshotStamp);

        // Thread 2: 1→2→1, stamps increment each time
        stamped.compareAndSet(1, 2, 0, 1); // val 1→2, stamp 0→1
        stamped.compareAndSet(2, 1, 1, 2); // val 2→1, stamp 1→2

        // Thread 1 CAS fails — stamp has changed even though value is same
        boolean fixed = stamped.compareAndSet(
                snapshotVal, 99,
                snapshotStamp, snapshotStamp + 1
        );
        System.out.println("  stamped CAS (ABA prevented): " + fixed);
        System.out.println("  stamped value: " + stamped.getReference()
                + " stamp: " + stamped.getStamp());
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — LongAdder
    // Better than AtomicLong under high contention
    // ─────────────────────────────────────────────
    static void longAdder() throws InterruptedException {
        // ── AtomicLong under high contention ──
        // All threads CAS the same memory location
        // Under high contention → many CAS failures → retry loops → slow
        AtomicLong atomicLong = new AtomicLong(0);

        // ── LongAdder — striped counter ──
        // Maintains a base + array of cells
        // Each thread updates its own cell — far less contention
        // sum() = base + all cells combined
        LongAdder adder = new LongAdder();

        int threads = 8;
        int iterations = 100_000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        long start = System.nanoTime();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                for (int j = 0; j < iterations; j++) adder.increment();
                latch.countDown();
            });
        }
        latch.await();
        long adderTime = System.nanoTime() - start;

        CountDownLatch latch2 = new CountDownLatch(threads);
        start = System.nanoTime();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                for (int j = 0; j < iterations; j++) atomicLong.incrementAndGet();
                latch2.countDown();
            });
        }
        latch2.await();
        long atomicTime = System.nanoTime() - start;

        pool.shutdown();
        System.out.println("  LongAdder sum  : " + adder.sum()
                + " time: " + adderTime / 1_000_000 + "ms");
        System.out.println("  AtomicLong sum : " + atomicLong.get()
                + " time: " + atomicTime / 1_000_000 + "ms");

        // ── LongAccumulator — generalised LongAdder ──
        // Combine values with any associative, commutative function
        LongAccumulator maxAcc = new LongAccumulator(Long::max, Long.MIN_VALUE);
        maxAcc.accumulate(42);
        maxAcc.accumulate(17);
        maxAcc.accumulate(99);
        maxAcc.accumulate(55);
        System.out.println("  LongAccumulator max: " + maxAcc.get());
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — Senior Level
    //   Lock-free stack, spin lock, non-blocking counter
    // ─────────────────────────────────────────────
    static void seniorLevel() throws InterruptedException {

        // ── Lock-free stack using AtomicReference ──
        LockFreeStack<Integer> stack = new LockFreeStack<>();
        stack.push(1); stack.push(2); stack.push(3);
        System.out.println("  stack pop: " + stack.pop()); // 3
        System.out.println("  stack pop: " + stack.pop()); // 2

        // ── Spin lock — CAS-based mutex ──
        // Busy-waits rather than blocking — good for very short critical sections
        SpinLock spin = new SpinLock();
        int[] count = {0};
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 5_000; i++) {
                spin.lock();
                try { count[0]++; }
                finally { spin.unlock(); }
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 5_000; i++) {
                spin.lock();
                try { count[0]++; }
                finally { spin.unlock(); }
            }
        });
        t1.start(); t2.start();
        t1.join();  t2.join();
        System.out.println("  spinlock count (expected 10000): " + count[0]);

        // ── AtomicIntegerArray — per-slot atomic counters ──
        // Useful for sharded counters, histogram buckets
        AtomicIntegerArray buckets = new AtomicIntegerArray(5);
        int[] values = {3, 1, 4, 1, 5, 9, 2, 6, 5, 3};
        for (int v : values) {
            int bucket = Math.min(v / 2, 4); // bucket 0-4
            buckets.incrementAndGet(bucket);
        }
        System.out.print("  histogram buckets: ");
        for (int i = 0; i < buckets.length(); i++) {
            System.out.print("[" + i + "]=" + buckets.get(i) + " ");
        }
        System.out.println();
    }

    // Lock-free stack — classic CAS pattern
    static class LockFreeStack<T> {
        private final AtomicReference<Node<T>> top = new AtomicReference<>(null);

        void push(T value) {
            Node<T> newNode = new Node<>(value);
            do {
                newNode.next = top.get();
            } while (!top.compareAndSet(newNode.next, newNode)); // retry on contention
        }

        T pop() {
            Node<T> current;
            do {
                current = top.get();
                if (current == null) return null;
            } while (!top.compareAndSet(current, current.next));
            return current.value;
        }

        static class Node<T> {
            T value; Node<T> next;
            Node(T value) { this.value = value; }
        }
    }

    // Spin lock — CAS on AtomicBoolean
    // Busy-waits — only suitable for very short critical sections
    static class SpinLock {
        private final AtomicBoolean locked = new AtomicBoolean(false);

        void lock() {
            while (!locked.compareAndSet(false, true)) {
                Thread.onSpinWait(); // hint to CPU — use pause instruction
            }
        }

        void unlock() {
            locked.set(false);
        }
    }
}