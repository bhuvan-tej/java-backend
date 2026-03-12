package com.javabackend.concurrency.synchronization;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 *
 * Synchronization
 *
 * THE PROBLEM
 *  Multiple threads reading/writing shared state without coordination
 *  → race conditions, data corruption, visibility issues
 *
 * THREE THINGS TO PROTECT
 *  1. Atomicity   — read-modify-write must happen as one unit
 *  2. Visibility  — changes made by one thread must be seen by others
 *  3. Ordering    — compiler/CPU can reorder instructions — must prevent
 *
 * TOOLS
 *  synchronized  — mutual exclusion + visibility + ordering (heavy)
 *  volatile      — visibility + ordering only, NO atomicity (lightweight)
 *  happens-before — JMM guarantee: action A is visible to action B
 *
 */
public class SynchronizationSamples {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("━━━ Race Condition ━━━\n");
        raceCondition();

        System.out.println("\n━━━ synchronized ━━━\n");
        synchronizedDemo();

        System.out.println("\n━━━ volatile ━━━\n");
        volatileDemo();

        System.out.println("\n━━━ wait / notify ━━━\n");
        waitNotifyDemo();

        System.out.println("\n━━━ Deadlock ━━━\n");
        deadlockDemo();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — Race Condition
    // Shows WHAT goes wrong without synchronization
    // ─────────────────────────────────────────────
    static void raceCondition() throws InterruptedException {
        // Unsafe counter — no synchronization
        UnsafeCounter unsafe = new UnsafeCounter();

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10_000; i++) unsafe.increment();
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10_000; i++) unsafe.increment();
        });

        t1.start(); t2.start();
        t1.join();  t2.join();

        // Expected: 20000. Actual: less — due to lost updates
        System.out.println("unsafe count (expected 20000): " + unsafe.count);

        // WHY: count++ is NOT atomic. It is 3 steps:
        //   1. read  count into register
        //   2. add   1
        //   3. write back to count
        // Two threads can read the same value, both add 1, both write same result
        // → one increment is lost
    }

    static class UnsafeCounter {
        int count = 0;
        void increment() { count++; } // NOT thread-safe
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — synchronized
    // ─────────────────────────────────────────────
    static void synchronizedDemo() throws InterruptedException {

        // ── Synchronized method — lock on 'this' ──
        SafeCounter safe = new SafeCounter();
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10_000; i++) safe.increment();
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10_000; i++) safe.increment();
        });
        t1.start(); t2.start();
        t1.join();  t2.join();
        System.out.println("safe count (expected 20000) : " + safe.getCount());

        // ── Synchronized block — finer granularity ──
        // Lock only what needs protection, not the entire method
        Object lock = new Object();
        int[] shared = {0};

        Runnable task = () -> {
            // non-critical work outside lock
            String name = Thread.currentThread().getName();
            synchronized (lock) {
                // only critical section is locked
                shared[0]++;
            }
        };

        Thread t3 = new Thread(task, "T3");
        Thread t4 = new Thread(task, "T4");
        t3.start(); t4.start();
        t3.join();  t4.join();
        System.out.println("block count (expected 2)    : " + shared[0]);

        // ── Synchronized static — lock on Class object ──
        // StaticCounter.increment() locks on StaticCounter.class
        Thread t5 = new Thread(() -> {
            for (int i = 0; i < 5_000; i++) StaticCounter.increment();
        });
        Thread t6 = new Thread(() -> {
            for (int i = 0; i < 5_000; i++) StaticCounter.increment();
        });
        t5.start(); t6.start();
        t5.join();  t6.join();
        System.out.println("static count (expected 10000): " + StaticCounter.count);

        // ── Reentrant — a thread can re-acquire its own lock ──
        ReentrantExample re = new ReentrantExample();
        re.outer(); // outer calls inner — same thread, same lock — no deadlock
    }

    static class SafeCounter {
        private int count = 0;
        synchronized void increment()  { count++; }    // lock on 'this'
        synchronized int  getCount()   { return count; }
    }

    static class StaticCounter {
        static int count = 0;
        static synchronized void increment() { count++; } // lock on StaticCounter.class
    }

    static class ReentrantExample {
        synchronized void outer() {
            System.out.println("outer — acquired lock");
            inner(); // re-enters same lock — allowed because same thread
        }
        synchronized void inner() {
            System.out.println("inner — re-acquired same lock (reentrant ✓)");
        }
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — volatile
    // ─────────────────────────────────────────────
    static void volatileDemo() throws InterruptedException {

        // ── WITHOUT volatile — visibility problem ──
        // Worker may never see running=false because it reads from CPU cache
        // (In practice JVM may optimize this — volatile makes it reliable)
        VolatileFlag flag = new VolatileFlag();

        Thread worker = new Thread(() -> {
            int i = 0;
            while (flag.running) { i++; } // reads running each iteration
            System.out.println("worker stopped after " + i + " iterations");
        });

        worker.start();
        Thread.sleep(10);
        flag.running = false; // signal the worker to stop
        worker.join(1000);

        if (worker.isAlive()) {
            System.out.println("worker still running! (visibility issue without volatile)");
            worker.interrupt();
        }

        // ── volatile guarantees ──
        // 1. Visibility  — write to volatile is immediately visible to all threads
        // 2. Ordering    — prevents reordering across volatile read/write
        // 3. NOT atomic  — volatile int x; x++ is still NOT thread-safe
        System.out.println("volatile read/write — visible across threads ✓");
        System.out.println("volatile x++ — still NOT atomic ✗ (use AtomicInteger)");

        // ── Double-checked locking — classic volatile use case ──
        Singleton s1 = Singleton.getInstance();
        Singleton s2 = Singleton.getInstance();
        System.out.println("same singleton: " + (s1 == s2));
    }

    static class VolatileFlag {
        volatile boolean running = true; // volatile — write visible across threads
    }

    // Double-checked locking — requires volatile on instance field
    static class Singleton {
        // volatile prevents instruction reordering during construction
        // Without volatile: another thread may see a partially constructed object
        private static volatile Singleton instance;

        private Singleton() {}

        static Singleton getInstance() {
            if (instance == null) {                    // first check — no lock
                synchronized (Singleton.class) {
                    if (instance == null) {            // second check — with lock
                        instance = new Singleton();
                    }
                }
            }
            return instance;
        }
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — wait / notify
    // Producer-Consumer pattern
    // ─────────────────────────────────────────────
    static void waitNotifyDemo() throws InterruptedException {
        // wait() and notify() must be called inside synchronized block
        // wait() releases the lock and suspends the thread
        // notify() wakes ONE waiting thread — it must re-acquire the lock

        final Object lock   = new Object();
        final int[]  buffer = {-1};
        final boolean[] ready = {false};

        Thread producer = new Thread(() -> {
            synchronized (lock) {
                buffer[0] = 42;       // produce value
                ready[0]  = true;
                System.out.println("  producer: produced " + buffer[0]);
                lock.notify();        // wake consumer
            }
        }, "producer");

        Thread consumer = new Thread(() -> {
            synchronized (lock) {
                while (!ready[0]) {   // loop — guard against spurious wakeups
                    try {
                        System.out.println("  consumer: waiting...");
                        lock.wait(); // release lock + suspend
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                System.out.println("  consumer: consumed " + buffer[0]);
            }
        }, "consumer");

        consumer.start();
        Thread.sleep(50); // ensure consumer waits first
        producer.start();
        consumer.join();
        producer.join();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — Deadlock
    // Shows how deadlock happens and how to avoid it
    // ─────────────────────────────────────────────
    static void deadlockDemo() throws InterruptedException {
        Object lockA = new Object();
        Object lockB = new Object();

        // ── Deadlock scenario (commented out — would hang forever) ──
        // Thread 1: acquires A, waits for B
        // Thread 2: acquires B, waits for A
        // → circular wait → deadlock
        System.out.println("  Deadlock scenario (not running — would hang):");
        System.out.println("  Thread1: lock A → wait for B");
        System.out.println("  Thread2: lock B → wait for A");
        System.out.println("  → circular wait → neither can proceed");

        // ── Fix 1: consistent lock ordering — always acquire in same order ──
        System.out.println("\n  Fix: consistent lock ordering");
        Thread t1 = new Thread(() -> {
            synchronized (lockA) {          // always A first
                sleep(10);
                synchronized (lockB) {
                    System.out.println("  T1: acquired A then B ✓");
                }
            }
        });

        Thread t2 = new Thread(() -> {
            synchronized (lockA) {          // always A first — no deadlock
                synchronized (lockB) {
                    System.out.println("  T2: acquired A then B ✓");
                }
            }
        });

        t1.start(); t2.start();
        t1.join();  t2.join();

        // ── Fix 2: tryLock with timeout (shown in locks module) ──
        System.out.println("Fix: tryLock with timeout — see locks module");

        // ── Detecting deadlock ──
        System.out.println("\n  Detection: jstack <pid> or VisualVM → thread dump");
        System.out.println("  Look for: 'waiting to lock' + circular dependency");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

}