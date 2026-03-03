package com.javabackend.collections.map;

import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 *
 * TREEMAP
 *
 * What's it?
 * TreeMap is a Red-Black Tree (self-balancing BST) based map.
 * Keys are always maintained in SORTED order.
 * It implements NavigableMap which gives you powerful range operations.
 *
 * When to use?
 *  ✅ Need keys in sorted order
 *  ✅ Range queries: "give me all entries where key is between X and Y"
 *  ✅ Floor/ceiling/nearest key lookups
 *  ✅ Time-series data with timestamp keys
 *  ✅ Implementing sliding windows, brackets, ranges
 *
 * All operations: O(log n)
 *   vs HashMap: O(1) but no ordering
 *   For sorted + range needs, TreeMap's O(log n) is the price you pay.
 *
 */
public class TreeMapSamples {

    public static void main(String[] args) {
        System.out.println("======== FOUNDATIONAL ========\n");
        foundationalExample();

        System.out.println("\n======== ADV LEVEL ========\n");
        advLevelExample();
    }

    // Foundational
    // Scenario: Grade brackets lookup
    static void foundationalExample() {
        // TreeMap — keys auto-sorted
        TreeMap<String, Integer> studentGrades = new TreeMap<>();
        studentGrades.put("Charlie", 78);
        studentGrades.put("Alice", 92);
        studentGrades.put("Bob", 85);
        studentGrades.put("Diana", 67);
        studentGrades.put("Eve", 95);

        System.out.println("Students sorted alphabetically: " + studentGrades);
        System.out.println("First student: " + studentGrades.firstKey());
        System.out.println("Last student:  " + studentGrades.lastKey());

        // NavigableMap range operations on keys
        System.out.println("\nStudents from Bob onwards: " + studentGrades.tailMap("Bob"));
        System.out.println("Students before Eve: " + studentGrades.headMap("Eve"));
        System.out.println("Students B to D: " + studentGrades.subMap("B", "E"));

        // floorKey/ceilingKey on strings
        System.out.println("\nfloorKey('Carl') [closest ≤]: " + studentGrades.floorKey("Carl"));
        System.out.println("ceilingKey('Carl') [closest ≥]: " + studentGrades.ceilingKey("Carl"));

        // Descending map — reversed view without creating new map
        NavigableMap<String, Integer> reversed = studentGrades.descendingMap();
        System.out.println("\nDescending order: " + reversed);
    }

    // ADV LEVEL
    // Scenario: Tax bracket calculation and time-series event lookup
    static void advLevelExample() {
        System.out.println(" Tax Bracket Calculator\n");

        // TreeMap is PERFECT for bracket lookups — floorKey() finds the right bracket
        // Key = income lower bound, Value = tax rate
        TreeMap<Integer, Double> taxBrackets = new TreeMap<>();
        taxBrackets.put(0, 0.0);        // 0 - 2.5L: 0%
        taxBrackets.put(250000, 0.05);  // 2.5L - 5L: 5%
        taxBrackets.put(500000, 0.20);  // 5L - 10L: 20%
        taxBrackets.put(1000000, 0.30); // 10L+: 30%

        int[] incomes = {150000, 350000, 750000, 1500000};

        for (int income : incomes) {
            // floorKey() — finds largest key ≤ income (the applicable bracket floor)
            Integer bracketFloor = taxBrackets.floorKey(income);
            double taxRate = taxBrackets.get(bracketFloor);
            System.out.printf("Income: ₹%-10d → Bracket: ₹%-8d → Rate: %.0f%%%n",
                    income, bracketFloor, taxRate * 100);
        }

        System.out.println("\nTime-Series Event Log\n");

        // TreeMap with timestamp keys — perfect for event logs, monitoring
        TreeMap<Long, String> eventLog = new TreeMap<>();
        long now = System.currentTimeMillis();

        // Simulate events at different timestamps
        eventLog.put(now - 5000, "UserLogin: alice");
        eventLog.put(now - 4000, "PageView: /dashboard");
        eventLog.put(now - 3000, "APICall: /api/orders");
        eventLog.put(now - 2000, "DBQuery: SELECT users");
        eventLog.put(now - 1000, "CacheHit: user:101");
        eventLog.put(now,        "Response: 200 OK");

        System.out.println("All events (chronological order):");
        eventLog.forEach((ts, event) ->
                System.out.printf("  T-%ds: %s%n", (now - ts) / 1000, event));

        // Get events in LAST 3 seconds — range query!
        long threeSecsAgo = now - 3000;
        System.out.println("\nEvents in last 3 seconds:");
        eventLog.tailMap(threeSecsAgo).forEach((ts, event) ->
                System.out.printf("  T-%ds: %s%n", (now - ts) / 1000, event));

        // Get MOST RECENT event before a point in time
        long twoSecsAgo = now - 2000;
        Map.Entry<Long, String> lastBeforePoint = eventLog.floorEntry(twoSecsAgo);
        System.out.println("\nLast event at or before T-2s: " + lastBeforePoint.getValue());

        // Get next event AFTER a point in time
        Map.Entry<Long, String> nextAfterPoint = eventLog.higherEntry(twoSecsAgo);
        System.out.println("First event after T-2s: " + nextAfterPoint.getValue());

        System.out.println("\nTreeMap with Custom Comparator\n");

        // Custom comparator — sort by string LENGTH, then alphabetically
        TreeMap<String, Integer> byLength = new TreeMap<>(
                Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder())
        );
        byLength.put("java", 1);
        byLength.put("go", 2);
        byLength.put("python", 3);
        byLength.put("c", 4);
        byLength.put("rust", 5);
        byLength.put("scala", 6);
        byLength.put("kotlin", 7);

        System.out.println("Languages sorted by name length: " + byLength.keySet());

        // Poll to process in order (destructive read)
        System.out.println("\nProcessing languages shortest name first:");
        while (!byLength.isEmpty()) {
            Map.Entry<String, Integer> entry = byLength.pollFirstEntry();
            System.out.println("  Processing: " + entry.getKey() + " (rank " + entry.getValue() + ")");
        }
    }

}