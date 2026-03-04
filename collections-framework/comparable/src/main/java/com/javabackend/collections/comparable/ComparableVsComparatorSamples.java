package com.javabackend.collections.comparable;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * COMPARABLE vs COMPARATOR
 *
 * Two ways to sort in JAVA:
 *
 * 1. Comparable (java.lang)
 *   → The object defines its OWN natural ordering
 *   → Implement: int compareTo(T other)
 *   → "I know how to compare myself to another of my kind"
 *   → Only ONE natural order per class
 *   → Used when you OWN the class
 *
 * 2. Comparator (java.util)
 *   → External strategy for comparing objects
 *   → Implement: int compare(T o1, T o2)
 *   → "I know how to compare these two objects"
 *   → Multiple different orderings possible
 *   → Used when you DON'T own the class, or need multiple orderings
 *
 * compareTo / compare contract:
 *  Returns negative → first is LESS than second
 *  Returns zero     → they are EQUAL
 *  Returns positive → first is GREATER than second
 *
 */
public class ComparableVsComparatorSamples {

    public static void main(String[] args) {
        System.out.println("======== COMPARABLE (Natural Ordering) ========\n");
        comparableDemo();

        System.out.println("\n======== COMPARATOR (Strategy Ordering) ========\n");
        comparatorDemo();

        System.out.println("\n======== CHAINING COMPARATORS (Senior Pattern) ========\n");
        chainingComparatorsDemo();
    }

    static class Employee implements Comparable<Employee> {
        int id, salary;
        String name, department;

        Employee(int id, String name, String department, int salary) {
            this.id = id;
            this.name = name;
            this.department = department;
            this.salary = salary;
        }

        int getSalary() { return salary; }

        @Override
        public int compareTo(Employee other) {
            // Natural order: salary ascending
            // Integer.compare is safer than subtraction (avoids overflow)
            return Integer.compare(this.salary, other.salary);
        }

        @Override
        public String toString() { return name + "(₹" + salary + ")"; }
    }

    // COMPARABLE — Natural ordering
    // Employee natural order = by salary ascending
    static void comparableDemo() {
        List<Employee> employees = Arrays.asList(
                new Employee(3, "Charlie", "Engineering", 95000),
                new Employee(1, "Alice", "Marketing", 75000),
                new Employee(5, "Eve", "Engineering", 85000),
                new Employee(2, "Bob", "HR", 65000),
                new Employee(4, "Diana", "Marketing", 80000)
        );

        // Collections.sort() uses Comparable.compareTo() — natural ordering
        Collections.sort(employees);
        System.out.println("Natural order (by salary asc):");
        employees.forEach(e -> System.out.printf("  %-10s ₹%,d%n", e.name, e.salary));

        // TreeSet also uses natural ordering
        TreeSet<Employee> empTree = new TreeSet<>(employees);
        System.out.println("\nLowest paid: " + empTree.first());
        System.out.println("Highest paid: " + empTree.last());
    }

    // COMPARATOR — Multiple orderings
    static void comparatorDemo() {
        List<Employee> employees = Arrays.asList(
                new Employee(3, "Charlie", "Engineering", 95000),
                new Employee(1, "Alice", "Marketing", 75000),
                new Employee(5, "Eve", "Engineering", 85000),
                new Employee(2, "Bob", "HR", 65000),
                new Employee(4, "Diana", "Marketing", 80000)
        );

        // Sort by NAME alphabetically — different from natural order
        Comparator<Employee> byName = Comparator.comparing(e -> e.name);
        employees.sort(byName);
        System.out.println("Sorted by name:");
        employees.forEach(e -> System.out.printf("  %s (dept: %s)%n", e.name, e.department));

        // Sort by DEPARTMENT, then by NAME — multi-level sort
        Comparator<Employee> byDeptThenName = Comparator
                .comparing((Employee e) -> e.department)
                .thenComparing(e -> e.name);
        employees.sort(byDeptThenName);
        System.out.println("\nSorted by department then name:");
        employees.forEach(e -> System.out.printf("  %-15s %s%n", e.department, e.name));

        // Sort by SALARY descending — reverse
        employees.sort(Comparator.comparingInt(Employee::getSalary).reversed());
        System.out.println("\nSorted by salary (highest first):");
        employees.forEach(e -> System.out.printf("  %-10s ₹%,d%n", e.name, e.salary));

        // Comparators for null-safe sorting (production pattern!)
        List<Employee> withNulls = new ArrayList<>(employees);
        withNulls.add(null); // Simulating null in list
        withNulls.add(new Employee(6, null, "Engineering", 70000)); // Null name

        // nullsFirst() and nullsLast() — production-safe sorting
        Comparator<Employee> nullSafe = Comparator.nullsLast(
                Comparator.comparing(e -> e.name, Comparator.nullsLast(Comparator.naturalOrder()))
        );
        withNulls.sort(nullSafe);
        System.out.println("\nNull-safe sort (nulls last):");
        withNulls.forEach(e -> System.out.println("  " + (e == null ? "NULL" : e.name)));
    }

    static class Product {
        private String name, category;
        private double price, rating;
        private int stock;

        Product(String name, String category, double price, double rating, int stock) {
            this.name = name;
            this.category = category;
            this.price = price;
            this.rating = rating;
            this.stock = stock;
        }

        String getName() { return name; }
        String getCategory() { return category; }
        double getPrice() { return price; }
        double getRating() { return rating; }
    }

    // CHAINING COMPARATORS — Adv level pattern
    // Real-world: Sort by multiple fields with type-safe lambda chain
    static void chainingComparatorsDemo() {
        List<Product> products = Arrays.asList(
                new Product("Laptop", "Electronics", 75000, 4.5, 150),
                new Product("Phone", "Electronics", 45000, 4.7, 320),
                new Product("Desk", "Furniture", 15000, 4.2, 80),
                new Product("Chair", "Furniture", 8000, 4.6, 200),
                new Product("Headphones", "Electronics", 8000, 4.3, 500),
                new Product("Lamp", "Furniture", 3000, 4.1, 90)
        );

        // Complex sort: Category → Price desc → Rating desc → Name asc
        // This is the kind of sort you'd write for a search result listing
        Comparator<Product> searchResultComparator = Comparator
                .comparing(Product::getCategory)                     // 1. Group by category A-Z
                .thenComparingDouble(Product::getPrice).reversed()   // 2. Most expensive first
                .thenComparingDouble(Product::getRating).reversed()  // 3. Highest rated first
                .thenComparing(Product::getName);                    // 4. Alphabetical tiebreaker

        // ⚠️ WARNING: .reversed() applies to the ENTIRE chain up to that point!
        // Use this pattern for clarity when mixing asc/desc:
        Comparator<Product> clearComparator = Comparator
                .comparing(Product::getCategory)                                      // ASC
                .thenComparing(Comparator.comparingDouble(Product::getPrice).reversed())  // DESC
                .thenComparing(Comparator.comparingDouble(Product::getRating).reversed()) // DESC
                .thenComparing(Product::getName);                                         // ASC

        products.sort(clearComparator);
        System.out.println("Products sorted by: Category(asc) → Price(desc) → Rating(desc) → Name(asc):");
        String prevCategory = "";
        for (Product p : products) {
            if (!p.getCategory().equals(prevCategory)) {
                System.out.println("  [" + p.getCategory() + "]");
                prevCategory = p.getCategory();
            }
            System.out.printf("    %-15s ₹%,7.0f  ⭐%.1f%n", p.getName(), p.getPrice(), p.getRating());
        }

        // Grouping by comparator result (production pattern)
        Map<String, List<Product>> byCategory = products.stream()
                .collect(Collectors.groupingBy(Product::getCategory));
        System.out.println("\nProduct counts by category:");
        byCategory.forEach((cat, prods) -> System.out.println("  " + cat + ": " + prods.size()));
    }

}