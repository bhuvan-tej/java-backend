package com.javabackend.concurrency.executors;

import java.util.*;
import java.util.concurrent.*;

/**
 *
 * Executors
 *
 * WHY EXECUTORS?
 *  Creating a new Thread per task is expensive — OS thread creation,
 *  stack allocation, scheduling overhead. Thread pools reuse threads.
 *
 * KEY TYPES
 *  Executor          — submit Runnable, fire and forget
 *  ExecutorService   — submit Callable, get Future, lifecycle management
 *  ThreadPoolExecutor— full control: core/max pool size, queue, policy
 *  ScheduledExecutorService — run tasks at fixed rate or with delay
 *  ForkJoinPool      — work stealing, parallel decomposition of tasks
 *
 */
public class ExecutorSamples {

    public static void main(String[] args) throws Exception {
        System.out.println("━━━ EXAMPLE 1 — Executors Factory ━━━\n");
        executorsFactory();

        System.out.println("\n━━━ EXAMPLE 2 — Future and Callable ━━━\n");
        futureAndCallable();

        System.out.println("\n━━━ EXAMPLE 3 — ThreadPoolExecutor ━━━\n");
        threadPoolExecutor();

        System.out.println("\n━━━ EXAMPLE 4 — ScheduledExecutor ━━━\n");
        scheduledExecutor();

        System.out.println("\n━━━ EXAMPLE 5 — ForkJoin ━━━\n");
        forkJoin();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — Executors Factory Methods
    // ─────────────────────────────────────────────
    static void executorsFactory() throws InterruptedException {

        // ── Fixed thread pool — bounded, predictable ──
        // N threads, unbounded LinkedBlockingQueue
        // Good for CPU-bound tasks — set N = Runtime.availableProcessors()
        ExecutorService fixed = Executors.newFixedThreadPool(3);
        for (int i = 1; i <= 5; i++) {
            int taskId = i;
            fixed.submit(() ->
                    System.out.println("  fixed [" + Thread.currentThread().getName()
                            + "] task-" + taskId));
        }
        fixed.shutdown();
        fixed.awaitTermination(2, TimeUnit.SECONDS);

        // ── Cached thread pool — unbounded, elastic ──
        // Creates threads on demand, reuses idle threads (60s keepalive)
        // Dangerous under burst load — can create thousands of threads
        ExecutorService cached = Executors.newCachedThreadPool();
        for (int i = 1; i <= 3; i++) {
            int taskId = i;
            cached.submit(() ->
                    System.out.println("  cached [" + Thread.currentThread().getName()
                            + "] task-" + taskId));
        }
        cached.shutdown();
        cached.awaitTermination(2, TimeUnit.SECONDS);

        // ── Single thread executor — serial execution ──
        // One thread, tasks execute in submission order
        // Good for sequencing work, actor pattern
        ExecutorService single = Executors.newSingleThreadExecutor();
        for (int i = 1; i <= 3; i++) {
            int taskId = i;
            single.submit(() ->
                    System.out.println("  single [" + Thread.currentThread().getName()
                            + "] task-" + taskId));
        }
        single.shutdown();
        single.awaitTermination(2, TimeUnit.SECONDS);

        // ── Virtual thread executor — Java 21+ ──
        // One virtual thread per task — lightweight, no pooling needed
        // Best for I/O-bound tasks — can run millions concurrently
        ExecutorService virtual = Executors.newVirtualThreadPerTaskExecutor();
        for (int i = 1; i <= 3; i++) {
            int taskId = i;
            virtual.submit(() ->
                    System.out.println("  virtual [" + Thread.currentThread().getName()
                            + "] task-" + taskId));
        }
        virtual.shutdown();
        virtual.awaitTermination(2, TimeUnit.SECONDS);
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — Future and Callable
    // ─────────────────────────────────────────────
    static void futureAndCallable() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);

        // ── Callable — task with return value ──
        Callable<String> task = () -> {
            Thread.sleep(100);
            return "result from " + Thread.currentThread().getName();
        };

        Future<String> future = pool.submit(task);

        // Non-blocking check
        System.out.println("  isDone before get: " + future.isDone());

        // Blocking get — waits for result
        String result = future.get();
        System.out.println("  result: " + result);
        System.out.println("  isDone after get : " + future.isDone());

        // ── get with timeout — don't wait forever ──
        Future<String> slow = pool.submit(() -> {
            Thread.sleep(2000);
            return "slow result";
        });
        try {
            slow.get(200, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            System.out.println("  get(200ms) timed out — cancelling");
            slow.cancel(true); // interrupt the running task
        }

        // ── invokeAll — submit multiple, wait for all ──
        List<Callable<Integer>> tasks = List.of(
                () -> { Thread.sleep(50);  return 1; },
                () -> { Thread.sleep(100); return 2; },
                () -> { Thread.sleep(30);  return 3; }
        );
        List<Future<Integer>> futures = pool.invokeAll(tasks);
        List<Integer> results = new ArrayList<>();
        for (Future<Integer> f : futures) {
            results.add(f.get());
        }
        System.out.println("  invokeAll results: " + results);

        // ── invokeAny — first to complete wins, rest cancelled ──
        String first = pool.invokeAny(List.of(
                () -> { Thread.sleep(200); return "slow"; },
                () -> { Thread.sleep(50);  return "fast"; },
                () -> { Thread.sleep(300); return "slower"; }
        ));
        System.out.println("  invokeAny winner : " + first);

        pool.shutdown();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — ThreadPoolExecutor
    // Full control over pool behaviour
    // ─────────────────────────────────────────────
    static void threadPoolExecutor() throws InterruptedException {
        // ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAlive,
        //                    unit, workQueue, threadFactory, rejectionHandler)
        //
        // Sizing guide:
        //   CPU-bound : corePoolSize = Runtime.availableProcessors()
        //   IO-bound  : corePoolSize = processors * (1 + wait/compute ratio)

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                2,                               // corePoolSize — always alive
                4,                               // maxPoolSize — under surge
                30, TimeUnit.SECONDS,            // keepAlive for extra threads
                new ArrayBlockingQueue<>(10),    // bounded queue — backpressure
                r -> {                           // named thread factory
                    Thread t = new Thread(r);
                    t.setName("worker-" + t.getId());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // rejection policy
        );

        // Submit tasks
        for (int i = 1; i <= 6; i++) {
            int taskId = i;
            pool.submit(() -> {
                System.out.println("  [" + Thread.currentThread().getName()
                        + "] executing task-" + taskId);
                sleep(50);
            });
        }

        // Pool diagnostics
        System.out.println("  poolSize      : " + pool.getPoolSize());
        System.out.println("  activeCount   : " + pool.getActiveCount());
        System.out.println("  queueSize     : " + pool.getQueue().size());
        System.out.println("  completedTasks: " + pool.getCompletedTaskCount());

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("  completedTasks: " + pool.getCompletedTaskCount());

        // ── Rejection policies ──
        // AbortPolicy        — throw RejectedExecutionException (default)
        // CallerRunsPolicy   — caller thread runs the task (backpressure)
        // DiscardPolicy      — silently discard
        // DiscardOldestPolicy— discard oldest queued task, retry submit
        System.out.println("  rejection policy: CallerRunsPolicy applied above");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — ScheduledExecutorService
    // ─────────────────────────────────────────────
    static void scheduledExecutor() throws InterruptedException {
        ScheduledExecutorService scheduler =
                Executors.newScheduledThreadPool(2);

        // ── schedule — run once after delay ──
        scheduler.schedule(
                () -> System.out.println("  schedule: ran after 100ms delay"),
                100, TimeUnit.MILLISECONDS
        );

        // ── scheduleAtFixedRate — run every N ms regardless of task duration ──
        // If task takes longer than period — next run starts immediately after
        int[] count = {0};
        ScheduledFuture<?> fixed = scheduler.scheduleAtFixedRate(() -> {
            count[0]++;
            System.out.println("  fixedRate: tick " + count[0]);
        }, 50, 100, TimeUnit.MILLISECONDS); // initial delay 50ms, then every 100ms

        // ── scheduleWithFixedDelay — N ms AFTER task completes ──
        // Guarantees gap between end of one run and start of next
        ScheduledFuture<?> delayed = scheduler.scheduleWithFixedDelay(() ->
                        System.out.println("  fixedDelay: tick"),
                50, 150, TimeUnit.MILLISECONDS
        );

        Thread.sleep(400); // let it run a few times
        fixed.cancel(false);
        delayed.cancel(false);
        scheduler.shutdown();
        scheduler.awaitTermination(1, TimeUnit.SECONDS);
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — ForkJoinPool
    // Recursive task decomposition + work stealing
    // ─────────────────────────────────────────────
    static void forkJoin() throws Exception {

        // ── RecursiveTask — returns a value ──
        // Sum an array by splitting in half recursively
        int[] data = new int[1_000];
        Arrays.fill(data, 1); // sum should be 1000

        ForkJoinPool pool = new ForkJoinPool(
                Runtime.getRuntime().availableProcessors()
        );

        long sum = pool.invoke(new SumTask(data, 0, data.length));
        System.out.println("  ForkJoin sum (expected 1000): " + sum);

        // ── Work stealing ──
        // Each worker thread has its own deque of tasks
        // Idle threads steal tasks from the tail of busy threads' deques
        // Minimises contention — no central task queue
        System.out.println("  parallelism: " + pool.getParallelism());
        System.out.println("  stealCount : " + pool.getStealCount());

        pool.shutdown();

        // ── Common pool — shared JVM-wide pool ──
        // Used by parallel streams, CompletableFuture by default
        // Avoid blocking tasks here — starves the common pool
        long result = ForkJoinPool.commonPool()
                .invoke(new SumTask(data, 0, data.length));
        System.out.println("  commonPool sum: " + result);
    }

    // Recursive sum — split until threshold, then compute directly
    static class SumTask extends RecursiveTask<Long> {
        private static final int THRESHOLD = 100;
        private final int[] data;
        private final int start, end;

        SumTask(int[] data, int start, int end) {
            this.data = data; this.start = start; this.end = end;
        }

        @Override
        protected Long compute() {
            if (end - start <= THRESHOLD) {
                // Base case — compute directly
                long sum = 0;
                for (int i = start; i < end; i++) sum += data[i];
                return sum;
            }
            // Split and fork
            int mid = (start + end) / 2;
            SumTask left  = new SumTask(data, start, mid);
            SumTask right = new SumTask(data, mid, end);
            left.fork();                    // submit left asynchronously
            long rightResult = right.compute(); // compute right in current thread
            long leftResult  = left.join();     // wait for left
            return leftResult + rightResult;
        }
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

}