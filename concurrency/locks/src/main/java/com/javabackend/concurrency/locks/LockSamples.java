package com.javabackend.concurrency.locks;

import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 *
 * Locks
 *
 * WHY LOCKS OVER SYNCHRONIZED?
 *  synchronized — simple but limited:
 *    - no timeout on lock acquisition
 *    - no interruptible waiting
 *    - no fairness control
 *    - no read/write separation
 *
 *  java.util.concurrent.locks — explicit, flexible:
 *    - tryLock(timeout) — avoid deadlocks
 *    - lockInterruptibly() — cancel waiting threads
 *    - fairness — longest-waiting thread gets lock first
 *    - ReadWriteLock — multiple readers OR one writer
 *    - StampedLock — optimistic reads (Java 8+)
 *    - Condition — multiple wait sets per lock
 *
 * GOLDEN RULE: always unlock in finally block
 *
 */
public class LockSamples {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("━━━ EXAMPLE 1 — ReentrantLock ━━━\n");
        reentrantLock();

        System.out.println("\n━━━ EXAMPLE 2 — tryLock ━━━\n");
        tryLock();

        System.out.println("\n━━━ EXAMPLE 3 — ReadWriteLock ━━━\n");
        readWriteLock();

        System.out.println("\n━━━ EXAMPLE 4 — Condition ━━━\n");
        conditionDemo();

        System.out.println("\n━━━ EXAMPLE 5 — StampedLock ━━━\n");
        stampedLock();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — ReentrantLock
    // ─────────────────────────────────────────────
    static void reentrantLock() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        int[] count = {0};

        Runnable task = () -> {
            for (int i = 0; i < 5_000; i++) {
                lock.lock();       // acquire
                try {
                    count[0]++;    // critical section
                } finally {
                    lock.unlock(); // ALWAYS in finally — even if exception thrown
                }
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start(); t2.start();
        t1.join();  t2.join();

        System.out.println("  count (expected 10000): " + count[0]);

        // ── Reentrant — same thread can lock multiple times ──
        ReentrantLock rl = new ReentrantLock();
        rl.lock();
        rl.lock(); // same thread — allowed, hold count = 2
        System.out.println("  hold count after 2 locks : " + rl.getHoldCount());
        rl.unlock();
        System.out.println("  hold count after 1 unlock: " + rl.getHoldCount());
        rl.unlock();
        System.out.println("  hold count after 2 unlock: " + rl.getHoldCount());

        // ── Fairness ──
        // ReentrantLock(true) — longest-waiting thread gets lock first
        // ReentrantLock(false) — default, no fairness, higher throughput
        ReentrantLock fairLock = new ReentrantLock(true);
        System.out.println("  fair lock    : " + fairLock.isFair());
        System.out.println("  unfair lock  : " + lock.isFair());

        // ── Useful diagnostics ──
        ReentrantLock diagLock = new ReentrantLock();
        System.out.println("  isLocked     : " + diagLock.isLocked());
        System.out.println("  queueLength  : " + diagLock.getQueueLength());
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — tryLock
    // Avoids deadlock — gives up if lock not available
    // ─────────────────────────────────────────────
    static void tryLock() throws InterruptedException {
        ReentrantLock lockA = new ReentrantLock();
        ReentrantLock lockB = new ReentrantLock();

        // ── tryLock() — non-blocking, returns immediately ──
        if (lockA.tryLock()) {
            try {
                System.out.println("  tryLock acquired immediately ✓");
            } finally {
                lockA.unlock();
            }
        } else {
            System.out.println("  tryLock failed — lock not available");
        }

        // ── tryLock(timeout) — wait up to timeout, then give up ──
        // This is the key deadlock-prevention tool
        Thread holder = new Thread(() -> {
            lockA.lock();
            try { sleep(300); } // hold lock for 300ms
            finally { lockA.unlock(); }
        });
        holder.start();
        Thread.sleep(50); // ensure holder has the lock

        boolean acquired = false;
        try {
            acquired = lockA.tryLock(100, TimeUnit.MILLISECONDS); // wait max 100ms
            if (acquired) {
                System.out.println("  tryLock(100ms) acquired ✓");
            } else {
                System.out.println("  tryLock(100ms) timed out — avoided blocking ✓");
            }
        } finally {
            if (acquired) lockA.unlock();
        }
        holder.join();

        // ── lockInterruptibly — can be cancelled while waiting ──
        lockB.lock(); // main thread holds lockB
        Thread waiter = new Thread(() -> {
            try {
                System.out.println("  waiter: trying lockInterruptibly...");
                lockB.lockInterruptibly(); // blocks — can be interrupted
                try {
                    System.out.println("  waiter: acquired");
                } finally {
                    lockB.unlock();
                }
            } catch (InterruptedException e) {
                System.out.println("  waiter: interrupted while waiting ✓");
            }
        });
        waiter.start();
        Thread.sleep(50);
        waiter.interrupt(); // cancel the waiting thread
        waiter.join();
        lockB.unlock();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — ReadWriteLock
    // Multiple readers OR one writer — never both
    // ─────────────────────────────────────────────
    static void readWriteLock() throws InterruptedException {
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        Lock readLock  = rwLock.readLock();
        Lock writeLock = rwLock.writeLock();
        int[] data = {0};

        // ── Multiple readers can hold read lock simultaneously ──
        Runnable reader = () -> {
            readLock.lock();
            try {
                // Multiple threads can be here at the same time
                System.out.println("  reader [" +
                        Thread.currentThread().getName() + "] read: " + data[0]);
                sleep(50);
            } finally {
                readLock.unlock();
            }
        };

        // ── Only one writer, blocks all readers ──
        Runnable writer = () -> {
            writeLock.lock();
            try {
                data[0]++;
                System.out.println("  writer wrote: " + data[0]);
                sleep(30);
            } finally {
                writeLock.unlock();
            }
        };

        // Start 3 readers concurrently — they all proceed simultaneously
        Thread r1 = new Thread(reader, "R1");
        Thread r2 = new Thread(reader, "R2");
        Thread r3 = new Thread(reader, "R3");
        r1.start(); r2.start(); r3.start();
        r1.join();  r2.join();  r3.join();

        // Writer gets exclusive access
        Thread w = new Thread(writer, "W1");
        w.start();
        w.join();

        System.out.println("  read locks held : " + rwLock.getReadLockCount());
        System.out.println("  write locked    : " + rwLock.isWriteLocked());

        // ── When to use ReadWriteLock ──
        // Read-heavy workloads: cache, config, reference data
        // Write is rare but must be exclusive
        // Poor fit: write-heavy or short read operations (overhead not worth it)
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — Condition
    // Multiple wait sets per lock — more flexible than wait/notify
    // ─────────────────────────────────────────────
    static void conditionDemo() throws InterruptedException {
        // Bounded buffer — classic Condition use case
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(3);

        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 6; i++) {
                try {
                    buffer.put(i);
                    System.out.println("  produced: " + i);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "producer");

        Thread consumer = new Thread(() -> {
            for (int i = 0; i < 6; i++) {
                try {
                    int val = buffer.take();
                    System.out.println("  consumed: " + val);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "consumer");

        producer.start();
        Thread.sleep(50);
        consumer.start();
        producer.join();
        consumer.join();
    }

    // Bounded buffer using Condition — cleaner than wait/notify
    // Two conditions: notFull (producer waits here) notEmpty (consumer waits here)
    static class BoundedBuffer<T> {
        private final Object[] items;
        private int count, putIdx, takeIdx;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition notFull  = lock.newCondition(); // producer waits here
        private final Condition notEmpty = lock.newCondition(); // consumer waits here

        BoundedBuffer(int capacity) { items = new Object[capacity]; }

        void put(T item) throws InterruptedException {
            lock.lock();
            try {
                while (count == items.length) notFull.await(); // wait if full
                items[putIdx] = item;
                if (++putIdx == items.length) putIdx = 0;
                count++;
                notEmpty.signal(); // wake one consumer
            } finally { lock.unlock(); }
        }

        @SuppressWarnings("unchecked")
        T take() throws InterruptedException {
            lock.lock();
            try {
                while (count == 0) notEmpty.await(); // wait if empty
                T item = (T) items[takeIdx];
                if (++takeIdx == items.length) takeIdx = 0;
                count--;
                notFull.signal(); // wake one producer
                return item;
            } finally { lock.unlock(); }
        }
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — StampedLock
    // Java 8+ — optimistic reads for read-heavy workloads
    // ─────────────────────────────────────────────
    static void stampedLock() throws InterruptedException {
        StampedLock sl = new StampedLock();
        double[] point = {1.0, 2.0}; // x, y

        // ── Write lock — returns a stamp ──
        long stamp = sl.writeLock();
        try {
            point[0] = 3.0;
            point[1] = 4.0;
            System.out.println("  write: point set to (" + point[0] + ", " + point[1] + ")");
        } finally {
            sl.unlockWrite(stamp);
        }

        // ── Optimistic read — no lock acquired ──
        // Fastest — just reads a stamp and validates after
        // If a write happened between tryOptimisticRead and validate → retry
        long oStamp = sl.tryOptimisticRead();
        double x = point[0];
        double y = point[1];
        if (!sl.validate(oStamp)) {
            // A write happened — fall back to real read lock
            long rStamp = sl.readLock();
            try {
                x = point[0];
                y = point[1];
            } finally {
                sl.unlockRead(rStamp);
            }
        }
        System.out.println("  optimistic read: (" + x + ", " + y + ")");

        // ── Convert read lock → write lock ──
        long readStamp = sl.readLock();
        try {
            if (point[0] < 0) {
                // Need to write — convert without releasing read lock
                long writeStamp = sl.tryConvertToWriteLock(readStamp);
                if (writeStamp != 0) {
                    readStamp = writeStamp;
                    point[0] = 0;
                } else {
                    // Conversion failed — release and acquire write lock
                    sl.unlockRead(readStamp);
                    readStamp = sl.writeLock();
                    point[0] = 0;
                }
            }
            System.out.println("  after convert: (" + point[0] + ", " + point[1] + ")");
        } finally {
            sl.unlock(readStamp);
        }

        // ── StampedLock is NOT reentrant — same thread cannot lock twice ──
        System.out.println("  StampedLock is NOT reentrant — use with care");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

}