package com.javabackend.concurrency.concurrent;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Concurrent Collections
 *
 * WHY NOT JUST SYNCHRONIZE A REGULAR COLLECTION?
 *  Collections.synchronizedMap(map) — coarse lock on every operation.
 *  One thread at a time for reads AND writes.
 *  Iteration still requires external synchronization.
 *
 * CONCURRENT COLLECTIONS
 *  ConcurrentHashMap      — segmented locking, concurrent reads + fine writes
 *  CopyOnWriteArrayList   — reads lock-free, writes copy the array
 *  CopyOnWriteArraySet    — same as above, backed by CopyOnWriteArrayList
 *  BlockingQueue          — producer-consumer, thread-safe put/take with blocking
 *  ConcurrentLinkedQueue  — lock-free, non-blocking, unbounded queue
 *  LinkedBlockingDeque    — blocking deque, useful for work stealing patterns
 *
 */
public class ConcurrentCollectionSamples {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("━━━ EXAMPLE 1 — ConcurrentHashMap ━━━\n");
        concurrentHashMap();

        System.out.println("\n━━━ EXAMPLE 2 — CopyOnWriteArrayList ━━━\n");
        copyOnWrite();

        System.out.println("\n━━━ EXAMPLE 3 — BlockingQueue ━━━\n");
        blockingQueue();

        System.out.println("\n━━━ EXAMPLE 4 — ConcurrentLinkedQueue ━━━\n");
        concurrentLinkedQueue();

        System.out.println("\n━━━ EXAMPLE 5 — Senior Level ━━━\n");
        seniorLevel();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — ConcurrentHashMap
    // ─────────────────────────────────────────────
    static void concurrentHashMap() throws InterruptedException {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        // ── Basic thread-safe operations ──
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        // putIfAbsent — atomic check-then-act
        map.putIfAbsent("d", 4);    // inserts
        map.putIfAbsent("a", 99);   // no-op — "a" already exists
        System.out.println("  putIfAbsent: " + map.get("a")); // 1

        // computeIfAbsent — create value only if key missing
        map.computeIfAbsent("e", k -> k.length()); // e → 1
        System.out.println("  computeIfAbsent e: " + map.get("e"));

        // compute — atomic read-modify-write
        map.compute("a", (k, v) -> v == null ? 1 : v + 10); // a → 11
        System.out.println("  compute a: " + map.get("a"));

        // merge — combine existing value with new one
        map.merge("a", 5, Integer::sum); // a → 16
        System.out.println("  merge a: " + map.get("a"));

        // ── Concurrent word count ──
        ConcurrentHashMap<String, Integer> wordCount = new ConcurrentHashMap<>();
        String[] words = {"apple","banana","apple","cherry","banana","apple"};

        // Multiple threads safely counting words
        List<Thread> threads = new ArrayList<>();
        for (String word : words) {
            Thread t = new Thread(() ->
                    wordCount.merge(word, 1, Integer::sum)); // atomic merge
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) t.join();
        System.out.println("  word count: " + wordCount);

        // ── Bulk operations (Java 8+) ──
        ConcurrentHashMap<String, Integer> scores = new ConcurrentHashMap<>();
        scores.put("Alice", 85); scores.put("Bob", 92); scores.put("Carol", 78);

        // forEach with parallelism threshold
        scores.forEach(1, (name, score) ->
                System.out.println("  " + name + " = " + score));

        // reduce — parallel aggregation
        int total = scores.reduceValues(1, Integer::sum);
        System.out.println("  total score: " + total);

        // search — returns first match or null
        String topScorer = scores.search(1, (k, v) -> v >= 90 ? k : null);
        System.out.println("  top scorer : " + topScorer);
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — CopyOnWriteArrayList
    // ─────────────────────────────────────────────
    static void copyOnWrite() throws InterruptedException {
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
        list.add("a"); list.add("b"); list.add("c");

        // ── Reads are lock-free — iterators see snapshot at creation time ──
        Iterator<String> it = list.iterator();

        // Modify while iterating — no ConcurrentModificationException
        list.add("d");
        list.remove("a");

        System.out.print("  iterator snapshot: ");
        while (it.hasNext()) System.out.print(it.next() + " "); // sees a,b,c
        System.out.println();
        System.out.println("  current list     : " + list); // b,c,d

        // ── Concurrent reads ──
        List<Thread> readers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            readers.add(new Thread(() -> {
                // All readers proceed simultaneously — no lock
                long count = list.stream().filter(s -> s.compareTo("b") >= 0).count();
                System.out.println("  reader count >= b: " + count
                        + " [" + Thread.currentThread().getName() + "]");
            }));
        }
        readers.forEach(Thread::start);
        for (Thread r : readers) r.join();

        // ── Write cost — copies entire array ──
        // Fine for small lists with rare writes
        // Poor for large lists with frequent writes
        System.out.println("  write cost: copies entire array on every add/remove");
        System.out.println("  best fit  : listener lists, observer registries, config");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — BlockingQueue
    // Producer-consumer with natural backpressure
    // ─────────────────────────────────────────────
    static void blockingQueue() throws InterruptedException {

        // ArrayBlockingQueue — bounded, FIFO, single lock
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5);

        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 8; i++) {
                try {
                    queue.put(i); // blocks if full
                    System.out.println("  produced: " + i
                            + " | queue size: " + queue.size());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "producer");

        Thread consumer = new Thread(() -> {
            for (int i = 0; i < 8; i++) {
                try {
                    Thread.sleep(80); // consume slower than produce
                    int val = queue.take(); // blocks if empty
                    System.out.println("  consumed: " + val
                            + " | queue size: " + queue.size());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        // ── BlockingQueue variants ──
        // ArrayBlockingQueue  — bounded, single lock, fair option
        // LinkedBlockingQueue — optionally bounded, separate head/tail locks
        //                       higher throughput than ArrayBlockingQueue
        // PriorityBlockingQueue — unbounded, tasks ordered by priority
        // SynchronousQueue    — zero capacity, direct hand-off producer→consumer
        //                       used in Executors.newCachedThreadPool()
        // DelayQueue          — tasks become available after delay expires
        //                       used for scheduled task implementations
        System.out.println("\n  BlockingQueue variants:");
        System.out.println("  ArrayBlockingQueue   — bounded, single lock");
        System.out.println("  LinkedBlockingQueue  — opt. bounded, better throughput");
        System.out.println("  PriorityBlockingQueue— unbounded, priority ordered");
        System.out.println("  SynchronousQueue     — zero capacity, direct hand-off");
        System.out.println("  DelayQueue           — tasks available after delay");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — ConcurrentLinkedQueue
    // Lock-free, non-blocking, unbounded
    // ─────────────────────────────────────────────
    static void concurrentLinkedQueue() throws InterruptedException {
        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

        // ── Non-blocking — offer/poll never block ──
        queue.offer("task-1");
        queue.offer("task-2");
        queue.offer("task-3");

        System.out.println("  peek (no remove): " + queue.peek());
        System.out.println("  poll (removes)  : " + queue.poll());
        System.out.println("  size            : " + queue.size());

        // ── Concurrent producers ──
        ExecutorService pool = Executors.newFixedThreadPool(3);
        for (int i = 4; i <= 8; i++) {
            int id = i;
            pool.submit(() -> queue.offer("task-" + id));
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.SECONDS);
        System.out.println("  after concurrent offers: " + queue.size() + " items");

        // ── vs BlockingQueue ──
        // ConcurrentLinkedQueue — non-blocking, never waits, caller must retry
        // BlockingQueue         — blocks caller until space/item available
        // Use CLQ when: producer/consumer must not block (event loop, reactive)
        // Use BQ  when: natural backpressure is desired (thread pools, pipelines)
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — Senior Level
    //   Real patterns: frequency map, cache, pipeline
    // ─────────────────────────────────────────────
    static void seniorLevel() throws InterruptedException {

        // ── Pattern 1: Concurrent frequency counter ──
        // merge is atomic — no external synchronization needed
        ConcurrentHashMap<String, Long> freq = new ConcurrentHashMap<>();
        String[] events = {"click","view","click","purchase","view","click"};
        ExecutorService pool = Executors.newFixedThreadPool(3);
        for (String event : events) {
            pool.submit(() -> freq.merge(event, 1L, Long::sum));
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.SECONDS);
        System.out.println("  event freq: " + freq);

        // ── Pattern 2: Simple concurrent cache ──
        // computeIfAbsent is atomic — no duplicate computation
        ConcurrentHashMap<Integer, String> cache = new ConcurrentHashMap<>();
        ExecutorService loaders = Executors.newFixedThreadPool(4);
        for (int i = 1; i <= 4; i++) {
            int id = i;
            loaders.submit(() -> {
                // Only computes if absent — safe under concurrent access
                String val = cache.computeIfAbsent(id,
                        k -> "user-" + k); // expensive load happens once per key
                System.out.println("  cache[" + id + "] = " + val
                        + " [" + Thread.currentThread().getName() + "]");
            });
        }
        loaders.shutdown();
        loaders.awaitTermination(1, TimeUnit.SECONDS);

        // ── Pattern 3: Pipeline with BlockingQueue ──
        // Stage 1 produces → queue → Stage 2 consumes + transforms
        BlockingQueue<Integer> stage1to2 = new LinkedBlockingQueue<>(10);
        BlockingQueue<String>  stage2to3 = new LinkedBlockingQueue<>(10);

        // Stage 1 — emit numbers
        Thread stage1 = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                try { stage1to2.put(i); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        });

        // Stage 2 — square them
        Thread stage2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    int val = stage1to2.take();
                    stage2to3.put("result=" + (val * val));
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        });

        // Stage 3 — print
        Thread stage3 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    System.out.println("  pipeline: " + stage2to3.take());
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        });

        stage1.start(); stage2.start(); stage3.start();
        stage1.join();  stage2.join();  stage3.join();
    }

}