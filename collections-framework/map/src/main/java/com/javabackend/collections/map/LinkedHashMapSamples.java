package com.javabackend.collections.map;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * LINKEDHASHMAP
 *
 * What's it?
 * LinkedHashMap = HashMap + doubly-linked list running through entries.
 * This linked list records either:
 *  - INSERTION ORDER (default) — order elements were put in
 *  - ACCESS ORDER (via constructor flag) — most recently accessed last
 *
 * Why it matters:
 *  - Deterministic iteration order (HashMap has no guarantee)
 *  - ACCESS ORDER mode enables LRU Cache in ~10 lines!
 *
 * When to use?
 *  ✅ Need HashMap performance + predictable iteration order
 *  ✅ Building LRU Cache (access order mode + removeEldestEntry)
 *  ✅ Need to maintain insertion order (e.g., config, pipelines)
 *
 * LRU CACHE (Least Recently Used):
 *  - Cache has limited capacity (e.g., 100 items)
 *  - When full, evict the LEAST RECENTLY USED item
 *  - LinkedHashMap in ACCESS ORDER is perfect for this!
 *  - Every get() moves the accessed entry to the END of the list
 *  - The HEAD of the list = LRU candidate for eviction
 *
 */
public class LinkedHashMapSamples {

    public static void main(String[] args) {
        System.out.println("======== FOUNDATIONAL ========\n");
        foundationalExample();

        System.out.println("\n======== ADV LEVEL ========\n");
        advLevelExample();
    }

    // Foundational
    // Scenario: Preserving order of configuration properties
    static void foundationalExample() {
        // HashMap — iteration order not guaranteed
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("server.port", "8080");
        hashMap.put("db.url", "localhost:5432");
        hashMap.put("cache.ttl", "300");
        hashMap.put("api.timeout", "30");
        System.out.print("HashMap order (unpredictable): ");
        hashMap.forEach((k, v) -> System.out.print(k + " "));
        System.out.println();

        // LinkedHashMap — INSERTION ORDER preserved
        Map<String, String> configMap = new LinkedHashMap<>();
        configMap.put("server.port", "8080");
        configMap.put("db.url", "localhost:5432");
        configMap.put("cache.ttl", "300");
        configMap.put("api.timeout", "30");

        System.out.print("LinkedHashMap order (insertion order): ");
        configMap.forEach((k, v) -> System.out.print(k + " "));
        System.out.println();

        // Operations are same as HashMap — O(1) average
        System.out.println("\nAll config properties:");
        configMap.forEach((key, value) ->
                System.out.printf("  %-20s = %s%n", key, value));

        // LinkedHashMap.removeEldestEntry() hook — basis for LRU
        // The oldest entry is always at the front of the linked list
        Map.Entry<String, String> oldest = configMap.entrySet().iterator().next();
        System.out.println("\nOldest entry (would be evicted in LRU): " + oldest.getKey());
    }

    // ADV LEVEL
    // Scenario: LRU Cache for database query results. Classic interview question + production pattern
    /**
     * LRU Cache Implementation using LinkedHashMap.
     *
     * KEY TRICK: LinkedHashMap(capacity, loadFactor, accessOrder=true)
     *   → accessOrder=true means each get() moves element to end of linked list
     *   → removeEldestEntry() is called after each put()
     *   → if it returns true, oldest element (head of list = LRU) is removed
     */
    static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        /**
         * @param capacity  Maximum number of entries before eviction kicks in
         *
         * Constructor args:
         *   16         → initial bucket capacity (standard)
         *   0.75f      → load factor (standard)
         *   true       → ACCESS ORDER! This is the magic flag.
         *               Without this, it's insertion order (not useful for LRU)
         */
        LRUCache(int capacity) {
            super(16, 0.75f, true); // true = accessOrder
            this.capacity = capacity;
        }

        /**
         * This method is called after every put().
         * Return true → remove the eldest entry (the LRU one, at the head).
         * We return true when we exceed capacity.
         */
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }

        // Override get() to return null instead of throwing on missing keys
        // (LinkedHashMap.get() already returns null, this just makes intent clear)
        @Override
        public V get(Object key) {
            return super.getOrDefault(key, null);
        }

        @Override
        public String toString() {
            // Show entries in order (LRU → MRU, left to right)
            StringBuilder sb = new StringBuilder("[LRU→MRU: ");
            this.forEach((k, v) -> sb.append(k).append("→").append(v).append(", "));
            if (size() > 0) sb.setLength(sb.length() - 2);
            sb.append("]");
            return sb.toString();
        }
    }

    static void advLevelExample() {
        System.out.println("LRU Cache Demo — Database Query Cache\n");

        // Create an LRU cache with capacity 3
        LRUCache<String, String> queryCache = new LRUCache<>(3);

        // Simulate caching database query results
        queryCache.put("SELECT * FROM users WHERE id=1", "Alice, alice@email.com");
        queryCache.put("SELECT * FROM orders WHERE user=1", "Order#101, Order#102");
        queryCache.put("SELECT * FROM products WHERE category=electronics", "Laptop, Phone, TV");

        System.out.println("Cache after 3 puts: " + queryCache);

        // Access the first query — moves it to "most recently used"
        queryCache.get("SELECT * FROM users WHERE id=1");
        System.out.println("After accessing query1 (moves to recent end): " + queryCache);

        // Adding 4th item — SHOULD evict the LRU item
        // At this point, "orders" query hasn't been accessed recently → gets evicted
        queryCache.put("SELECT COUNT(*) FROM users", "1500");
        System.out.println("After adding 4th item (LRU evicted): " + queryCache);

        // Verify eviction happened correctly
        System.out.println("\nIs 'orders' query still cached? " +
                (queryCache.get("SELECT * FROM orders WHERE user=1") != null));

        System.out.println("\nTesting Full LRU Behavior");
        LRUCache<Integer, String> cache = new LRUCache<>(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        System.out.println("Initial: " + cache);

        cache.get(1); // Access 1 → 1 becomes MRU
        System.out.println("After get(1) [1 is now MRU]: " + cache);

        cache.put(4, "four"); // 4 added → 2 is LRU, gets evicted
        System.out.println("After put(4) [2 evicted as LRU]: " + cache);

        System.out.println("2 still in cache? " + (cache.get(2) != null)); // false — evicted
    }

}