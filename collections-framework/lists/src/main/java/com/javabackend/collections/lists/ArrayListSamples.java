package com.javabackend.collections.lists;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * ARRAYLIST — Deep Dive
 *
 * What's it?
 * ArrayList is a resizable array. Internally it's just a plain Object[]
 * that doubles in size when it runs out of space (default capacity = 10).
 *
 * When to use?
 * ✅ You need fast random access by index (O(1))
 * ✅ You mostly ADD to the end
 * ✅ You iterate a lot more than you insert/delete
 *
 * When not to use?
 * ❌ Frequent insertions/deletions in the MIDDLE → use LinkedList
 * ❌ Need thread-safety → use CopyOnWriteArrayList
 * ❌ Need unique elements → use HashSet
 *
 * Internal Mechanics:
 * - Backed by Object[] elementData
 * - When size == capacity, it grows to (oldCapacity * 3/2 + 1) roughly
 * - This is why pre-sizing with new ArrayList<>(expectedSize) saves time
 */
public class ArrayListSamples {

    public static void main(String[] args) {

        System.out.println("======== FOUNDATIONAL ========\n");
        foundationalExample();

        System.out.println("\n======== ADV LEVEL ========\n");
        advLevelExample();
    }

    // Foundational
    static void foundationalExample() {

        // Creating ArrayList — no initial size specified (defaults to 10 internally)
        List<String> waitlist = new ArrayList<>();

        // add() — O(1) amortized. Adds to end of internal array.
        waitlist.add("Alice");
        waitlist.add("Bob");
        waitlist.add("Charlie");
        waitlist.add("Diana");
        System.out.println("Initial waitlist: " + waitlist);

        // add(index, element) — O(n) because it shifts elements to the right
        // Use sparingly in performance-critical code
        waitlist.add(1, "Eve"); // Insert Eve at position 1
        System.out.println("After inserting Eve at index 1: " + waitlist);

        // get(index) — O(1), this is ArrayList's superpower
        String firstStudent = waitlist.get(0);
        System.out.println("First on waitlist: " + firstStudent);

        // set(index, element) — Replace value at index, O(1)
        waitlist.set(2, "Frank");
        System.out.println("After replacing index 2 with Frank: " + waitlist);

        // remove(index) — O(n) because it shifts elements left
        waitlist.remove(0); // Removes "Alice"
        System.out.println("After removing index 0 (Alice): " + waitlist);

        // remove(Object) — O(n) because it scans for the object first
        waitlist.remove("Diana");
        System.out.println("After removing Diana by value: " + waitlist);

        // contains() — O(n) linear scan. ArrayList is NOT a good lookup structure.
        boolean hasBob = waitlist.contains("Bob");
        System.out.println("Is Bob still on waitlist? " + hasBob);

        // size() — O(1) just returns the count field
        System.out.println("Waitlist size: " + waitlist.size());

        // Iterating — for-each is the cleanest way, uses Iterator internally
        System.out.print("Current waitlist: ");
        for (String student : waitlist) {
            System.out.print(student + " ");
        }
        System.out.println();

        // subList() — returns a VIEW of the original list (not a copy!)
        // Modifying the sublist affects the original!
        List<String> topTwo = waitlist.subList(0, 2);
        System.out.println("Top 2 from waitlist (view): " + topTwo);

        // Sorting the waitlist alphabetically
        Collections.sort(waitlist);
        System.out.println("Sorted waitlist: " + waitlist);

        // Convert array to ArrayList — common pattern
        String[] namesArray = {"Zara", "Mark", "Lucy"};
        // Arrays.asList returns FIXED-SIZE list — cannot add/remove, only set!
        List<String> fixedList = Arrays.asList(namesArray);
        // To get a mutable list, wrap it:
        List<String> mutableList = new ArrayList<>(Arrays.asList(namesArray));
        mutableList.add("Tom"); // Works fine
        System.out.println("Mutable list from array: " + mutableList);
    }

    static class Order {

        int id;
        String productName;
        double price;
        String status;

        Order(int id, String productName, double price, String status) {
            this.id = id;
            this.productName = productName;
            this.price = price;
            this.status = status;
        }

        int getId() { return id; }
        double getPrice() { return price; }

        @Override
        public String toString() {
            return productName + "(" + status + ")";
        }

    }

    // Scenario: E-commerce order processing with filtering, sorting, pre-sizing, and safe iteration patterns
    static void advLevelExample() {

        // Pre-sizing — If you know approximate size, pre-size to avoid
        // multiple internal array resize+copy operations. Performance win!
        ArrayList<Order> orders = new ArrayList<>(100); // Reserve for 100 orders

        // Populate with sample orders
        orders.add(new Order(1, "Laptop", 75000.0, "PENDING"));
        orders.add(new Order(2, "Phone", 45000.0, "SHIPPED"));
        orders.add(new Order(3, "Headphones", 8000.0, "PENDING"));
        orders.add(new Order(4, "Monitor", 22000.0, "DELIVERED"));
        orders.add(new Order(5, "Keyboard", 3500.0, "PENDING"));
        orders.add(new Order(6, "Mouse", 1200.0, "CANCELLED"));

        System.out.println("All orders: " + orders);

        // PATTERN 1: Safe removal during iteration using removeIf()
        // ❌ WRONG WAY — will throw ConcurrentModificationException:
        //    for (Order o : orders) { if (o.status.equals("CANCELLED")) orders.remove(o); }
        // ✅ RIGHT WAY 1 — removeIf() (Java 8+), cleanest solution
        orders.removeIf(order -> order.status.equals("CANCELLED"));
        System.out.println("After removing CANCELLED orders: " + orders.size() + " remaining");

        // PATTERN 2: Custom sorting with Comparator
        // Sort by price descending, then by ID ascending as tiebreaker
        orders.sort(Comparator.comparing(Order::getPrice)
                .reversed()
                .thenComparing(Order::getId));
        System.out.println("\nOrders sorted by price (desc):");
        orders.forEach(order -> System.out.printf(" [%d] %s - ₹%.0f (%s)%n",
                order.id, order.productName, order.price, order.status));

        // PATTERN 3: Filtering into a new list using Streams
        List<Order> pendingOrders = orders.stream()
                .filter(order -> order.status.equals("PENDING"))
                .collect(Collectors.toList());
        System.out.println("\nPending orders count: " + pendingOrders.size());

        // PATTERN 4: Partitioning into two lists
        // Split orders into expensive (>20000) and affordable
        Map<Boolean, List<Order>> partitioned = orders.stream()
                .collect(Collectors.partitioningBy(order -> order.price > 20000));
        System.out.println("\nExpensive orders (>₹20k): " + partitioned.get(true).size());
        System.out.println("Affordable orders: " + partitioned.get(false).size());

        // PATTERN 5: Using ListIterator for bidirectional traversal
        // ListIterator can go forward AND backward — useful for specific algorithms
        System.out.println("\nReverse traversal using ListIterator:");
        ListIterator<Order> lit = orders.listIterator(orders.size()); // Start at end
        while (lit.hasPrevious()) {
            Order order = lit.previous();
            System.out.println("  " + order.productName);
        }

        // PATTERN 6: Collections.unmodifiableList for defensive programming
        // Return read-only view to prevent callers from mutating your internal list
        List<Order> readOnlyOrders = Collections.unmodifiableList(orders);
        System.out.println("\nUnmodifiable list size: " + readOnlyOrders.size());
        // readOnlyOrders.add(new Order(7, "Tab", 500, "NEW")); // Would throw UnsupportedOperationException

        // ---- PATTERN 7: Trimming internal capacity after bulk removals ----
        // After lots of removes, internal array might still be large
        // trimToSize() reduces memory footprint — useful for long-lived lists
        orders.trimToSize();
        System.out.println("\nMemory trimmed to actual size: " + orders.size());
    }

}