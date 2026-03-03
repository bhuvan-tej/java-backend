package com.javabackend.collections.set;

import java.util.*;

/**
 *
 * LINKEDHASHSET
 *
 * What's it?
 * HashSet + doubly-linked list through entries.
 * Preserves INSERTION ORDER. No duplicates. O(1) ops.
 *
 * When to use?
 *  ✅ Need uniqueness AND insertion order preserved
 *  ✅ Deduplication while maintaining first-seen order
 *
 * When not to use?
 *  ❌ Need sorted order            → TreeSet
 *  ❌ Don't care about order       → HashSet (less memory)
 *
 */
public class LinkedHashSetSamples {

    public static void main(String[] args) {
        System.out.println("======== FOUNDATIONAL ========\n");
        foundationalExample();

        System.out.println("\n======== ADV LEVEL ========\n");
        advLevelExample();
    }

    // Foundational
    static void foundationalExample() {

        // HashSet — order NOT guaranteed
        Set<String> hash = new HashSet<>(
                Arrays.asList("banana", "apple", "cherry", "apple"));
        System.out.println("HashSet (no order)       : " + hash);

        // LinkedHashSet — INSERTION ORDER guaranteed
        Set<String> linked = new LinkedHashSet<>(
                Arrays.asList("banana", "apple", "cherry", "apple"));
        System.out.println("LinkedHashSet (ordered)  : " + linked);

        // Deduplication preserving first-seen order
        List<String> events = Arrays.asList(
                "LOGIN", "PAGE_VIEW", "CLICK",
                "LOGIN", "PAGE_VIEW", "PURCHASE", "CLICK");

        Set<String> uniqueEvents = new LinkedHashSet<>(events);
        System.out.println("\nRaw events       : " + events);
        System.out.println("Unique (ordered) : " + uniqueEvents);

        // All Set operations work exactly like HashSet
        Set<String> visited = new LinkedHashSet<>(
                Arrays.asList("home", "about", "pricing"));
        visited.add("contact");
        visited.remove("about");
        System.out.println("Visited pages    : " + visited); // order preserved
    }

    // ADV LEVEL — Recently viewed items tracker
    // Scenario: remove + re-add to move item to end (most recently viewed is always last)
    static class RecentlyViewed {
        private final int maxSize;
        private final LinkedHashSet<String> items;

        RecentlyViewed(int maxSize) {
            this.maxSize = maxSize;
            this.items = new LinkedHashSet<>();
        }

        void view(String item) {
            items.remove(item);    // remove from current position
            items.add(item);       // re-add at END (most recent)

            // Evict the oldest if over capacity
            if (items.size() > maxSize) {
                String oldest = items.iterator().next(); // head = oldest
                items.remove(oldest);
            }
            System.out.println("Viewed: " + item + " | tracked: " + items);
        }

        Set<String> getAll() {
            return Collections.unmodifiableSet(items);
        }
        boolean hasViewed(String item) {
            return items.contains(item);
        }

        String getMostRecent() {
            // Traverse to last element
            String last = null;
            for (String s : items) last = s;
            return last;
        }
    }

    static void advLevelExample() {
        RecentlyViewed tracker = new RecentlyViewed(5);

        tracker.view("Laptop");
        tracker.view("Phone");
        tracker.view("Headphones");
        tracker.view("Monitor");
        tracker.view("Laptop");     // re-viewed — moves to end (most recent)
        tracker.view("Keyboard");
        tracker.view("Phone");      // re-viewed — moves to end
        tracker.view("Tablet");     // 6th unique item — evicts oldest

        System.out.println("Recently viewed (oldest → newest):");
        tracker.getAll().forEach(item ->
                System.out.println("  → " + item));

        System.out.println("Most recent : " + tracker.getMostRecent());
        System.out.println("Viewed Laptop? : " + tracker.hasViewed("Laptop"));

        // Practical use: deduplicate API response fields
        System.out.println("\n── Deduplicate API fields preserving order ──");
        List<String> fields = Arrays.asList(
                "id", "name", "email", "id", "phone", "name", "address");
        Set<String> uniqueFields = new LinkedHashSet<>(fields);
        System.out.println("Raw    : " + fields);
        System.out.println("Unique : " + uniqueFields);
    }
}
