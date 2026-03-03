package com.javabackend.collections.set;

import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;

/**
 *
 * TREESET
 *
 * What's it?
 * TreeSet is backed by a TreeMap (Red-Black Tree).
 * Elements are stored in SORTED order automatically.
 *
 * Red-Black Tree = self-balancing BST → guarantees O(log n)
 * for add, remove, contains even in worst case.
 *
 * When to use?
 *  ✅ Need elements in SORTED order without calling sort()
 *  ✅ Need range operations: "give me all elements between X and Y"
 *  ✅ Need floor/ceiling/first/last operations
 *  ✅ Need a navigable sorted set
 *
 * When not to use?
 *  ❌ Just need uniqueness without sorting → HashSet (faster O(1))
 *  ❌ Elements are not Comparable and no Comparator provided → ClassCastException
 *
 * IMPORTANT:
 *  TreeSet uses compareTo() (or Comparator) to determine uniqueness,
 *  NOT hashCode/equals! If compareTo() returns 0, elements are equal — period.
 *
 */
public class TreeSetSamples {

    public static void main(String[] args) {
        System.out.println("======== FOUNDATIONAL ========\n");
        foundationalExample();

        System.out.println("\n======== ADV LEVEL ========\n");
        advLevelExample();
    }

    // Foundational
    // Scenario: Leaderboard with sorted scores
    static void foundationalExample() {
        // TreeSet of integers — sorted in natural (ascending) order
        TreeSet<Integer> scores = new TreeSet<>();
        scores.add(85);
        scores.add(92);
        scores.add(67);
        scores.add(92); // Duplicate! TreeSet ignores it
        scores.add(78);
        scores.add(100);

        System.out.println("Scores (auto-sorted): " + scores);
        System.out.println("Lowest score:  " + scores.first());  // O(log n)
        System.out.println("Highest score: " + scores.last());   // O(log n)

        // NavigableSet operations — this is TreeSet's real power
        // floor(x) — largest element <= x
        System.out.println("\nfloor(90) — highest score ≤ 90: " + scores.floor(90));
        // ceiling(x) — smallest element >= x
        System.out.println("ceiling(90) — lowest score ≥ 90: " + scores.ceiling(90));
        // lower(x) — largest element strictly < x
        System.out.println("lower(92) — highest score < 92: " + scores.lower(92));
        // higher(x) — smallest element strictly > x
        System.out.println("higher(85) — lowest score > 85: " + scores.higher(85));

        // Range views — subSet, headSet, tailSet
        // Scores between 70 and 90 (inclusive on both by default for subSet with booleans)
        System.out.println("\nScores from 70 to 90 (inclusive): " + scores.subSet(70, true, 90, true));
        // All scores below 90
        System.out.println("Scores below 90: " + scores.headSet(90));
        // All scores from 85 and above
        System.out.println("Scores from 85 up: " + scores.tailSet(85));

        // Descending view — reversed without creating new collection
        System.out.println("\nDescending order: " + scores.descendingSet());

        // pollFirst() / pollLast() — retrieve and remove
        System.out.println("\nRemoving and returning highest: " + scores.pollLast());
        System.out.println("Remaining: " + scores);
    }

    static class Product {
        private String name;
        private double price;

        Product(String name, double price) {
            this.name = name;
            this.price = price;
        }

        String getName() { return name; }
        double getPrice() { return price; }

        @Override
        public String toString() {
            return name + "(₹" + (int)price + ")";
        }
    }

    // ADV LEVEL
    // Scenario: Stock price tracking system — find nearest prices, range queries, custom sorted products
    static void advLevelExample() {
        System.out.println("Stock Price Range Finder\n");

        // TreeSet of stock prices — always sorted
        TreeSet<Double> stockPrices = new TreeSet<>(
                Arrays.asList(150.5, 148.2, 155.0, 147.8, 160.0, 142.3, 158.7, 163.5)
        );
        System.out.println("Stock prices this week: " + stockPrices);

        double currentPrice = 154.0;
        System.out.println("\nCurrent price: " + currentPrice);
        System.out.println("Nearest lower price point: " + stockPrices.floor(currentPrice));
        System.out.println("Nearest higher price point: " + stockPrices.ceiling(currentPrice));

        // Find prices within a trading band (±5% of current price)
        double lower = currentPrice * 0.95;
        double upper = currentPrice * 1.05;
        System.out.printf("Prices within 5%% band [%.2f - %.2f]: %s%n",
                lower, upper, stockPrices.subSet(lower, true, upper, true));

        // Custom Comparator with TreeSet
        System.out.println("\n Custom-Sorted Products \n");

        // Sort products: primarily by price ascending, secondarily by name
        // NOTE: Comparator must be consistent — if two products compare as 0,
        // TreeSet treats them as EQUAL (only one will be stored!)
        TreeSet<Product> products = new TreeSet<>(
                Comparator.comparingDouble(Product::getPrice)
                        .thenComparing(Product::getName));

        products.add(new Product("Laptop", 75000));
        products.add(new Product("Phone", 45000));
        products.add(new Product("Headphones", 8000));
        products.add(new Product("Monitor", 22000));
        products.add(new Product("Tablet", 45000)); // Same price as Phone — saved because name differs

        System.out.println("Products sorted by price then name:");
        products.forEach(p -> System.out.printf("  %-15s ₹%.0f%n", p.getName(), p.getPrice()));

        // Find the cheapest product above ₹20,000
        Product threshold = new Product("", 20000);
        Product cheapestAbove20k = products.higher(threshold);
        System.out.println("\nCheapest product above ₹20k: " + cheapestAbove20k);

        // Most expensive product below ₹50,000
        Product under50k = new Product("", 50000);
        Product mostExpUnder50k = products.lower(under50k);
        System.out.println("Most expensive under ₹50k: " + mostExpUnder50k);

        // TreeSet vs HashSet performance context
        System.out.println("\n When to choose TreeSet vs HashSet");
        System.out.println("Use HashSet when: just need uniqueness, O(1) is priority");
        System.out.println("Use TreeSet when: need sorted order OR range queries");
        System.out.println("TreeSet operations: O(log n) vs HashSet: O(1)");
        System.out.println("For 1M elements: HashSet contains() = ~1 op, TreeSet = ~20 ops");
    }

}