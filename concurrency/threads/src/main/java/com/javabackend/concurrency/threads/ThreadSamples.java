package com.javabackend.concurrency.threads;

import java.util.concurrent.*;

/**
 *
 * Threads
 *
 * A Thread is the smallest unit of execution in the JVM.
 * Every Java program starts with one thread — the main thread.
 *
 * THREAD LIFECYCLE
 *
 * NEW → RUNNABLE → RUNNING → BLOCKED/WAITING/TIMED_WAITING → TERMINATED
 *
 * NEW            — created, not yet started
 * RUNNABLE       — eligible to run, waiting for CPU
 * RUNNING        — actively executing (subset of RUNNABLE in JVM model)
 * BLOCKED        — waiting to acquire a monitor lock
 * WAITING        — waiting indefinitely (wait(), join())
 * TIMED_WAITING  — waiting with timeout (sleep(), wait(ms), join(ms))
 * TERMINATED     — finished or threw uncaught exception
 *
 * TWO WAYS TO CREATE A THREAD
 *   1. Extend Thread
 *   2. Implement Runnable (preferred — decouples task from execution)
 *
 */
public class ThreadSamples {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("━━━ Creating Threads ━━━\n");
        creatingThreads();

        System.out.println("\n━━━ Thread Lifecycle ━━━\n");
        threadLifecycle();

        System.out.println("\n━━━ join / sleep / interrupt ━━━\n");
        joinSleepInterrupt();

        System.out.println("\n━━━ Daemon Threads ━━━\n");
        daemonThreads();

        System.out.println("\n━━━ Adv Level ━━━\n");
        advLevel();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — Creating Threads
    // ─────────────────────────────────────────────
    static void creatingThreads() throws InterruptedException {

        // ── Way 1: Extend Thread ──
        // Tightly couples task logic with thread mechanism — avoid in production
        Thread t1 = new Thread() {
            @Override
            public void run() {
                System.out.println("Way 1 (extend Thread): " + Thread.currentThread().getName());
            }
        };

        // ── Way 2: Implement Runnable — preferred ──
        // Decouples WHAT to run from HOW to run it
        // Same Runnable can be submitted to a thread pool later
        Runnable task = () -> System.out.println("Way 2 (Runnable): " + Thread.currentThread().getName());
        Thread t2 = new Thread(task, "worker-thread");

        // ── Way 3: Callable + FutureTask — when you need a return value ──
        Callable<String> callable = () -> {
            Thread.sleep(50);
            return "result from " + Thread.currentThread().getName();
        };
        FutureTask<String> future = new FutureTask<>(callable);
        Thread t3 = new Thread(future, "callable-thread");

        t1.start(); t1.join();
        t2.start(); t2.join();
        t3.start();
        try {
            System.out.println("Way 3 (Callable): " + future.get());
        } catch (ExecutionException e) {
            System.out.println("error: " + e.getCause().getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — Thread Lifecycle
    // ─────────────────────────────────────────────
    static void threadLifecycle() throws InterruptedException {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "lifecycle-thread");

        System.out.println("  before start : " + t.getState()); // NEW
        t.start();
        System.out.println("  after  start : " + t.getState()); // RUNNABLE or TIMED_WAITING
        Thread.sleep(50);
        System.out.println("  while running: " + t.getState()); // TIMED_WAITING
        t.join();
        System.out.println("  after  join  : " + t.getState()); // TERMINATED

        // Thread properties
        System.out.println("  name         : " + t.getName());
        System.out.println("  id           : " + t.getId());
        System.out.println("  priority     : " + t.getPriority()); // 1-10, default 5
        System.out.println("  isDaemon     : " + t.isDaemon());
        System.out.println("  isAlive      : " + t.isAlive());
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — join / sleep / interrupt
    // ─────────────────────────────────────────────
    static void joinSleepInterrupt() throws InterruptedException {

        // ── join — wait for a thread to finish ──
        Thread worker = new Thread(() -> {
            System.out.println("  worker: started");
            try { Thread.sleep(100); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("  worker: done");
        });

        worker.start();
        System.out.println("  main: waiting for worker...");
        worker.join(); // main thread blocks here until worker finishes
        System.out.println("  main: worker finished");

        // ── join with timeout — don't wait forever ──
        Thread slow = new Thread(() -> {
            try { Thread.sleep(5000); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        slow.start();
        slow.join(200); // wait max 200ms
        System.out.println("  slow thread state after join(200): " + slow.getState());
        slow.interrupt(); // clean up

        // ── interrupt — cooperative cancellation ──
        // interrupt() sets a flag — the thread must CHECK and respond
        Thread interruptable = new Thread(() -> {
            System.out.println("  interruptable: started");
            try {
                for (int i = 0; i < 10; i++) {
                    if (Thread.currentThread().isInterrupted()) {
                        System.out.println("  interruptable: interrupted, stopping");
                        return; // cooperate — stop cleanly
                    }
                    Thread.sleep(50); // throws InterruptedException if interrupted while sleeping
                    System.out.println("  interruptable: step " + i);
                }
            } catch (InterruptedException e) {
                // InterruptedException clears the flag — restore it
                Thread.currentThread().interrupt();
                System.out.println("  interruptable: caught InterruptedException");
            }
        });

        interruptable.start();
        Thread.sleep(120); // let it run 2 steps
        interruptable.interrupt();
        interruptable.join();
        System.out.println("  interrupt flag after: " + interruptable.isInterrupted());
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — Daemon Threads
    // ─────────────────────────────────────────────
    static void daemonThreads() throws InterruptedException {
        // Daemon thread — background thread
        // JVM exits when only daemon threads remain
        // Use for: GC, log flushing, heartbeat, cleanup tasks

        Thread daemon = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(100);
                    System.out.println("  daemon: tick");
                } catch (InterruptedException e) {
                    return;
                }
            }
        });

        // Must set BEFORE start() — cannot change after
        daemon.setDaemon(true);
        daemon.start();

        System.out.println("  daemon isDeamon: " + daemon.isDaemon());

        Thread.sleep(250); // let daemon tick twice
        // When main exits, JVM will kill the daemon thread automatically
        System.out.println("  main: finishing — daemon will be killed by JVM");
        daemon.interrupt(); // clean up for this demo
        daemon.join();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — Senior Level
    //   Thread-per-task pattern, uncaught exception
    //   handler, ThreadLocal
    // ─────────────────────────────────────────────
    static void advLevel() throws InterruptedException {

        // ── UncaughtExceptionHandler ──
        // Default: prints stack trace to stderr and thread dies silently
        // Production: log properly, alert, clean up resources
        Thread.UncaughtExceptionHandler handler = (thread, ex) ->
                System.out.println("  [HANDLER] thread=" + thread.getName() +
                        " threw: " + ex.getMessage());

        Thread risky = new Thread(() -> {
            throw new RuntimeException("something went wrong");
        }, "risky-thread");
        risky.setUncaughtExceptionHandler(handler);
        risky.start();
        risky.join();

        // ── ThreadLocal — per-thread storage ──
        // Each thread gets its own independent copy of the value
        // Common use: request context, user session, DB connection per thread
        ThreadLocal<String> requestId = new ThreadLocal<>();

        Runnable requestHandler = () -> {
            // Each thread sets its own value — invisible to other threads
            requestId.set("REQ-" + Thread.currentThread().getName());
            System.out.println("  [" + Thread.currentThread().getName() +
                    "] requestId = " + requestId.get());
            // ALWAYS remove after use — prevents memory leaks in thread pools
            requestId.remove();
        };

        Thread r1 = new Thread(requestHandler, "T1");
        Thread r2 = new Thread(requestHandler, "T2");
        Thread r3 = new Thread(requestHandler, "T3");
        r1.start(); r2.start(); r3.start();
        r1.join();  r2.join();  r3.join();

        // ── Thread naming — critical for debugging in production ──
        // Default names: Thread-0, Thread-1 — useless in logs
        ThreadFactory namedFactory = r -> {
            Thread t = new Thread(r);
            t.setName("order-processor-" + t.getId());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        };

        Thread named = namedFactory.newThread(() ->
                System.out.println("  running as: " + Thread.currentThread().getName()));
        named.start();
        named.join();
    }

}