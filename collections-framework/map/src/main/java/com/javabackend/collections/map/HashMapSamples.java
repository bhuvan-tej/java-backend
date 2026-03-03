package com.javabackend.collections.map;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * HASHMAP
 *
 * What's it?
 * HashMap stores key-value pairs using a hash table.
 * Internally: an array of "buckets" (Node[]).
 * When you put(key, value):
 *  1. Compute key.hashCode()
 *  2. Apply additional mixing: (h = key.hashCode()) ^ (h >>> 16)
 *  3. Find bucket index: hash & (capacity - 1)
 *  4. If bucket empty → store directly
 *  5. If collision → add to linked list in bucket
 *  6. If bucket list > 8 entries → convert to Red-Black Tree (Java 8+)
 *
 * KEY internals to know:
 *  - Default capacity: 16
 *  - Load factor: 0.75 (resize when 75% full)
 *  - Resize: doubles to 32 when 12 entries (16 * 0.75)
 *  - Java 8+: Treeify threshold = 8 (list → tree for dense buckets)
 *
 * When to use?
 *  ✅ Key-value lookup — O(1) average
 *  ✅ Counting frequencies, grouping data
 *  ✅ Caching computed results
 *
 * When not to use?
 *  ❌ Need sorted keys → TreeMap
 *  ❌ Need insertion order → LinkedHashMap
 *  ❌ Thread safety needed → ConcurrentHashMap
 *  ❌ Keys are null sensitive (HashMap allows ONE null key, ConcurrentHashMap doesn't)
 *
 */
public class HashMapSamples {

    public static void main(String[] args) {

        System.out.println("======== FOUNDATIONAL ========\n");
        foundationalExample();

        System.out.println("\n======== ADV LEVEL ========\n");
        advLevelExample();
    }

    // Foundational
    // Scenario: Word frequency counter
    static void foundationalExample() {
        // Basic operations
        Map<String, Integer> wordCount = new HashMap<>();
        String[] words = {"java", "is", "great", "java", "is", "java", "fast"};

        for (String word : words) {
            // CLASSIC pattern — check if exists, then increment or initialize
            // Problem: Two lookups (get + put)
            // wordCount.put(word, wordCount.containsKey(word) ? wordCount.get(word) + 1 : 1);

            // BETTER — getOrDefault() — single expression, one get + one put
            wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
        }
        System.out.println("Word frequencies: " + wordCount);

        // get() — O(1) average
        System.out.println("Count of 'java': " + wordCount.get("java"));

        // get() returns null for missing keys — getOrDefault saves null checks
        System.out.println("Count of 'python': " + wordCount.get("python")); // null
        System.out.println("Count of 'python' (with default): " + wordCount.getOrDefault("python", 0)); // 0

        // put() — O(1), replaces value if key exists, returns OLD value
        Integer oldValue = wordCount.put("java", 99);
        System.out.println("Old value for 'java': " + oldValue + " | New: " + wordCount.get("java"));

        // putIfAbsent() — only puts if key is NOT already present
        wordCount.putIfAbsent("java", 0); // Won't change because "java" exists
        wordCount.putIfAbsent("kotlin", 1); // Will add because "kotlin" doesn't exist
        System.out.println("After putIfAbsent: " + wordCount);

        // remove(key) — O(1), returns the removed value
        Integer removed = wordCount.remove("kotlin");
        System.out.println("Removed 'kotlin' (value was): " + removed);

        // containsKey() / containsValue() — O(1) for key, O(n) for value!
        System.out.println("Has key 'is': " + wordCount.containsKey("is"));
        System.out.println("Has value 99: " + wordCount.containsValue(99));

        // Iterating over Map — three patterns:
        System.out.println("\n Iterating ");

        // Pattern 1: entrySet() — best when you need both key AND value
        for (Map.Entry<String, Integer> entry : wordCount.entrySet()) {
            System.out.println("  " + entry.getKey() + " → " + entry.getValue());
        }

        // Pattern 2: keySet() — when you only need keys
        System.out.print("Keys only: ");
        wordCount.keySet().forEach(k -> System.out.print(k + " "));
        System.out.println();

        // Pattern 3: values() — when you only need values (not keys)
        System.out.println("Sum of all counts: " + wordCount.values().stream().mapToInt(i -> i).sum());

    }

    static class Transaction {
        String id, user, type;
        int amount;

        Transaction(String id, String user, int amount, String type) {
            this.id = id;
            this.user = user;
            this.amount = amount;
            this.type = type;
        }

        String getUser() { return user; }
        int getAmount() { return amount; }

        @Override
        public String toString() { return id + "(" + user + ":" + amount + ")"; }
    }

    // ADV LEVEL
    // Scenario: Building an in-memory cache, grouping API responses, using compute/merge for complex update patterns
    static void advLevelExample() {
        System.out.println("Advanced HashMap Patterns\n");

        // PATTERN 1: merge() — cleaner than getOrDefault for aggregation
        // merge(key, value, remappingFunction)
        // If key absent → just stores value
        // If key present → applies remappingFunction(existingValue, newValue)
        Map<String, Integer> salesByRegion = new HashMap<>();
        String[] regions = {"North", "South", "North", "East", "South", "North"};
        int[] amounts = {5000, 3000, 4500, 2000, 1500, 6000};

        for (int i = 0; i < regions.length; i++) {
            final int amount = amounts[i];
            // Merge: if region exists, sum up; if not, just store amount
            salesByRegion.merge(regions[i], amount, Integer::sum);
        }
        System.out.println("Sales by region: " + salesByRegion);

        // PATTERN 2: compute() / computeIfAbsent() for lazy initialization
        // computeIfAbsent — only computes if key is missing
        // Great for lazy loading or grouping
        Map<String, List<String>> groupedByDept = new HashMap<>();
        String[][] employees = {
                {"Alice", "Engineering"}, {"Bob", "Marketing"},
                {"Charlie", "Engineering"}, {"Diana", "Marketing"},
                {"Eve", "Engineering"}
        };

        for (String[] emp : employees) {
            // computeIfAbsent: if "Engineering" key doesn't exist, create new ArrayList
            // then immediately add to it — in ONE line, thread-safe for list creation
            groupedByDept.computeIfAbsent(emp[1], dept -> new ArrayList<>()).add(emp[0]);
        }
        System.out.println("\nEmployees grouped by department: " + groupedByDept);

        // computeIfPresent — only computes if key EXISTS
        // Useful for updates without accidental key creation
        groupedByDept.computeIfPresent("Engineering", (dept, list) -> {
            list.add("Frank"); // Add new joiner
            return list;
        });
        System.out.println("After adding Frank to Engineering: " + groupedByDept.get("Engineering"));

        // compute() — always called regardless of key presence
        // Return null from function → removes the key!
        Map<String, Integer> inventory = new HashMap<>();
        inventory.put("apples", 10);
        inventory.put("bananas", 3);

        // Decrement bananas — remove if reaches 0
        inventory.compute("bananas", (key, qty) -> (qty == null || qty <= 1) ? null : qty - 1);
        inventory.compute("bananas", (key, qty) -> (qty == null || qty <= 1) ? null : qty - 1);
        inventory.compute("bananas", (key, qty) -> (qty == null || qty <= 1) ? null : qty - 1);
        System.out.println("\nInventory after selling bananas: " + inventory); // bananas removed when hits 0

        // PATTERN 3: Grouping and aggregation with Streams
        List<Transaction> transactions = Arrays.asList(
                new Transaction("TXN001", "Alice", 5000, "CREDIT"),
                new Transaction("TXN002", "Bob", 3000, "DEBIT"),
                new Transaction("TXN003", "Alice", 2000, "DEBIT"),
                new Transaction("TXN004", "Charlie", 8000, "CREDIT"),
                new Transaction("TXN005", "Bob", 1500, "CREDIT"),
                new Transaction("TXN006", "Alice", 4000, "CREDIT")
        );

        // Group transactions by user
        Map<String, List<Transaction>> byUser = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getUser));
        System.out.println("\nTransaction count per user:");
        byUser.forEach((user, txns) ->
                System.out.println("  " + user + ": " + txns.size() + " transactions"));

        // Sum amounts per user
        Map<String, Integer> totalByUser = transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getUser,
                        Collectors.summingInt(Transaction::getAmount)
                ));
        System.out.println("\nNet amount per user (not accounting type): " + totalByUser);

        // PATTERN 4: Capacity planning to avoid rehashing
        // If you expect ~1000 entries, initialize with enough capacity
        // to avoid resize: expectedSize / loadFactor = 1000 / 0.75 ≈ 1334
        // Round up to next power of 2: 2048
        // This avoids ~3 resize operations during population
        Map<String, Object> largeCache = new HashMap<>(2048, 0.75f);
        System.out.println("\nPre-sized HashMap created with capacity 2048 to avoid rehashing");

        // PATTERN 5: replaceAll() for bulk transformations
        Map<String, String> configs = new HashMap<>();
        configs.put("DB_URL", "localhost:5432");
        configs.put("CACHE_URL", "localhost:6379");
        configs.put("API_KEY", "dev_123abc");

        // Convert all config values to uppercase in-place
        configs.replaceAll((key, value) -> value.toUpperCase());
        System.out.println("\nConfigs after replaceAll toUpperCase: " + configs);
    }

}