package com.javabackend.collections.map;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * CONCURRENTHASHMAP
 *
 * What's it?
 * ConcurrentHashMap is the thread-safe alternative to HashMap.
 * Unlike Hashtable (which synchronizes the ENTIRE map for every op),
 * ConcurrentHashMap uses SEGMENT LOCKING (Java 7) or
 * CAS + lock-per-bucket (Java 8+) for much better concurrency.
 *
 * JAVA 8 internals:
 * - Uses CAS (Compare-And-Swap) for most operations — lock-free!
 * - Synchronizes only at the bucket level when needed
 * - Reads are almost always lock-free
 * - 16 concurrent writes can happen simultaneously (in different buckets)
 *
 * Key differences from HashMap:
 * - NO null keys or null values (throws NullPointerException!)
 * - Thread-safe without external synchronization
 * - Iterators are weakly consistent (won't throw CME, may see partial updates)
 * - putIfAbsent, compute, merge are ATOMIC operations
 *
 * When to use?
 * ✅ Multiple threads reading/writing the same map
 * ✅ Counters, accumulators in concurrent code
 * ✅ Shared cache in a service
 *
 * When not to use?
 * ❌ Single-threaded — just use HashMap (simpler, slightly faster)
 * ❌ Need compound operations to be atomic — must handle externally
 *
 */
public class ConcurrentHashMapSamples {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("======== FOUNDATIONAL ========\n");
        foundationalExample();

        System.out.println("\n======== ADV LEVEL ========\n");
        advLevelExample();

    }

    // Foundational
    // Showing why ConcurrentHashMap is needed
    static void foundationalExample() throws InterruptedException {
        // Problem: Two threads updating a HashMap simultaneously
        // This leads to LOST UPDATES and DATA CORRUPTION
        Map<String, Integer> unsafeMap = new HashMap<>();
        unsafeMap.put("counter", 0);

        // SAFE: Use ConcurrentHashMap instead
        ConcurrentHashMap<String, Integer> safeMap = new ConcurrentHashMap<>();
        safeMap.put("counter", 0);

        // Basic operations — same API as HashMap
        safeMap.put("users", 100);
        safeMap.put("orders", 250);
        safeMap.put("products", 50);

        System.out.println("ConcurrentHashMap: " + safeMap);

        // get() — lock-free read, very fast
        System.out.println("Users: " + safeMap.get("users"));

        // getOrDefault() — atomic read with fallback
        System.out.println("Revenue (not set): " + safeMap.getOrDefault("revenue", 0));

        // putIfAbsent() — atomic check-then-put
        // Returns the EXISTING value if key was present, null if this was the first put
        Integer existing = safeMap.putIfAbsent("users", 999);
        System.out.println("putIfAbsent users (should return 100 since exists): " + existing);
        System.out.println("Map after putIfAbsent: " + safeMap.get("users")); // Still 100

        // replace() — atomic conditional replace
        boolean replaced = safeMap.replace("users", 100, 150); // Only replaces if current value is 100
        System.out.println("replace users 100→150: " + replaced + " | New value: " + safeMap.get("users"));

        // IMPORTANT: null is NOT allowed!
        try {
            safeMap.put("key", null); // NullPointerException!
        } catch (NullPointerException e) {
            System.out.println("\nConcurrentHashMap rejects null values! (unlike HashMap)");
        }

        // SIMPLE CONCURRENT COUNTER EXAMPLE
        System.out.println("\n Thread-safe word counter");
        ConcurrentHashMap<String, Integer> wordCount = new ConcurrentHashMap<>();
        String[] words = {"java", "python", "java", "go", "java", "python"};

        // merge() is ATOMIC — thread-safe increment!
        for (String word : words) {
            wordCount.merge(word, 1, Integer::sum);
        }
        System.out.println("Word counts: " + wordCount);
    }

    // ADV LEVEL
    // Multi-threaded access, compute operations, concurrent counters
    static void advLevelExample() throws InterruptedException {
        System.out.println("Multi-threaded Request Counter\n");

        // Simulating a web server tracking endpoint hit counts
        ConcurrentHashMap<String, AtomicInteger> hitCounters = new ConcurrentHashMap<>();

        // Initialize counters for known endpoints
        String[] endpoints = {"/api/users", "/api/orders", "/api/products", "/api/reports"};
        for (String ep : endpoints) {
            hitCounters.put(ep, new AtomicInteger(0));
        }

        // Simulate 4 threads hitting endpoints concurrently
        int threadCount = 4;
        int hitsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        Random random = new Random();

        for (int t = 0; t < threadCount; t++) {
            new Thread(() -> {
                for (int i = 0; i < hitsPerThread; i++) {
                    String endpoint = endpoints[random.nextInt(endpoints.length)];
                    // computeIfAbsent is atomic — safe to use from multiple threads
                    hitCounters.computeIfAbsent(endpoint, k -> new AtomicInteger(0))
                            .incrementAndGet(); // AtomicInteger.incrementAndGet is atomic
                }
                latch.countDown();
            }).start();
        }

        latch.await(); // Wait for all threads to finish

        System.out.println("Endpoint hit counts after concurrent access:");
        hitCounters.forEach((endpoint, count) ->
                System.out.printf("  %-20s %d hits%n", endpoint, count.get()));

        int totalHits = hitCounters.values().stream().mapToInt(AtomicInteger::get).sum();
        System.out.println("Total hits (should be " + (threadCount * hitsPerThread) + "): " + totalHits);

        // IMPORTANT: compute() operations are ATOMIC
        System.out.println("\n Atomic compute() operations\n");

        ConcurrentHashMap<String, List<String>> userSessions = new ConcurrentHashMap<>();

        // This is SAFE even from multiple threads — compute is synchronized per key
        // For HashMap this would be a race condition!
        String userId = "user-101";
        userSessions.compute(userId, (key, sessions) -> {
            if (sessions == null) sessions = new ArrayList<>();
            sessions.add("session-" + UUID.randomUUID().toString().substring(0, 8));
            return sessions;
        });

        System.out.println("Sessions for " + userId + ": " + userSessions.get(userId));

        // BULK OPERATIONS (Java 8+) — parallel-friendly
        System.out.println("\n Bulk Parallel Operations\n");

        ConcurrentHashMap<String, Integer> inventory = new ConcurrentHashMap<>();
        inventory.put("laptop", 50);
        inventory.put("phone", 200);
        inventory.put("tablet", 30);
        inventory.put("monitor", 75);
        inventory.put("keyboard", 150);

        // forEach with parallelism threshold
        // =1 means use parallelism for ANY size (useful for large maps)
        System.out.println("Low stock items (< 60):");
        inventory.forEach(1, (product, qty) -> {
            if (qty < 60) {
                System.out.println("  ⚠️ " + product + ": only " + qty + " left");
            }
        });

        // search() — find first match in parallel
        String lowStockItem = inventory.search(1,
                (product, qty) -> qty < 40 ? product : null
        );
        System.out.println("\nFirst critically low stock item: " + lowStockItem);

        // reduce() — aggregate all values in parallel
        int totalInventory = inventory.reduceValues(1, Integer::sum);
        System.out.println("Total inventory count: " + totalInventory);

        // mappingCount() — better than size() for very large maps (returns long)
        System.out.println("Product count: " + inventory.mappingCount());

        System.out.println("\n ConcurrentHashMap.newKeySet() — Thread-safe Set=\n");
        // Need a thread-safe Set? Use this!
        Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
        onlineUsers.add("alice");
        onlineUsers.add("bob");
        onlineUsers.add("charlie");
        onlineUsers.remove("bob");
        System.out.println("Online users: " + onlineUsers);
    }

}