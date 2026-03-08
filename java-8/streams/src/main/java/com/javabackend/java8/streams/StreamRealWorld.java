package com.javabackend.java8.streams;

import java.util.*;
import java.util.stream.*;
import java.util.function.*;

/**
 *
 * Stream Real World Problems
 *
 * Combines all stream knowledge into production scenarios:
 *   1. Sales report by region + product
 *   2. Log file analyzer — error rate per service
 *   3. Student grade report — pass/fail/top10%
 *   4. Order pipeline — filter, enrich, group, summarise
 *   5. Word count + top-K
 *   6. Flat invoice lines from nested orders
 *
 */
public class StreamRealWorld {

    public static void main(String[] args) {
        sep("1. Sales Report");         salesReport();
        sep("2. Log Analyser");         logAnalyser();
        sep("3. Grade Report");         gradeReport();
        sep("4. Order Pipeline");       orderPipeline();
        sep("5. Word Count Top-K");     wordCountTopK();
        sep("6. Flat Invoice Lines");   invoiceLines();
    }

    // ─────────────────────────────────────────────
    // 1. SALES REPORT
    // ─────────────────────────────────────────────
    static void salesReport() {
        List<Sale> sales = Arrays.asList(
                new Sale("North", "Laptop",  75_000, 3),
                new Sale("South", "Phone",   45_000, 5),
                new Sale("North", "Phone",   45_000, 2),
                new Sale("East",  "Laptop",  75_000, 1),
                new Sale("South", "Laptop",  75_000, 4),
                new Sale("East",  "Monitor", 22_000, 6),
                new Sale("North", "Monitor", 22_000, 2)
        );

        // Total revenue by region
        System.out.println("Revenue by region:");
        sales.stream()
                .collect(Collectors.groupingBy(
                        s -> s.region,
                        Collectors.summingLong(s -> (long)s.price * s.qty)))
                .entrySet().stream()
                .sorted(Map.Entry.<String,Long>comparingByValue().reversed())
                .forEach(e -> System.out.printf(
                        "  %-8s ₹%,d%n", e.getKey(), e.getValue()));

        // Best selling product by units
        System.out.println("Units by product:");
        sales.stream()
                .collect(Collectors.groupingBy(
                        s -> s.product,
                        Collectors.summingInt(s -> s.qty)))
                .entrySet().stream()
                .sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
                .forEach(e -> System.out.printf(
                        "  %-10s %d units%n", e.getKey(), e.getValue()));
    }

    // ─────────────────────────────────────────────
    // 2. LOG ANALYSER
    // ─────────────────────────────────────────────
    static void logAnalyser() {
        List<LogEntry> logs = Arrays.asList(
                new LogEntry("auth-service",    "INFO",  "Login OK"),
                new LogEntry("order-service",   "ERROR", "DB timeout"),
                new LogEntry("auth-service",    "ERROR", "Invalid token"),
                new LogEntry("payment-service", "INFO",  "Payment OK"),
                new LogEntry("order-service",   "INFO",  "Order placed"),
                new LogEntry("payment-service", "ERROR", "Card declined"),
                new LogEntry("auth-service",    "INFO",  "Logout OK"),
                new LogEntry("order-service",   "ERROR", "Out of stock"),
                new LogEntry("payment-service", "ERROR", "Gateway timeout")
        );

        // Error count per service
        System.out.println("Errors per service:");
        logs.stream()
                .filter(l -> "ERROR".equals(l.level))
                .collect(Collectors.groupingBy(l -> l.service, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String,Long>comparingByValue().reversed())
                .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue()));

        // Error rate per service (%)
        Map<String, Long> total = logs.stream()
                .collect(Collectors.groupingBy(l -> l.service, Collectors.counting()));
        Map<String, Long> errors = logs.stream()
                .filter(l -> "ERROR".equals(l.level))
                .collect(Collectors.groupingBy(l -> l.service, Collectors.counting()));

        System.out.println("Error rate per service:");
        total.forEach((svc, tot) -> {
            long err  = errors.getOrDefault(svc, 0L);
            System.out.printf("  %-20s %.0f%%%n", svc, 100.0 * err / tot);
        });
    }

    // ─────────────────────────────────────────────
    // 3. GRADE REPORT
    // ─────────────────────────────────────────────
    static void gradeReport() {
        List<Student> students = Arrays.asList(
                new Student("Alice",   92), new Student("Bob",     54),
                new Student("Charlie", 78), new Student("Diana",   95),
                new Student("Eve",     43), new Student("Frank",   88),
                new Student("Grace",   67), new Student("Hank",    71),
                new Student("Ivy",     98), new Student("Jack",    39)
        );

        // Pass/fail partition
        Map<Boolean, List<Student>> partition = students.stream()
                .collect(Collectors.partitioningBy(s -> s.score >= 60));
        System.out.println("Passed: " +
                partition.get(true).stream().map(s -> s.name).collect(Collectors.toList()));
        System.out.println("Failed: " +
                partition.get(false).stream().map(s -> s.name).collect(Collectors.toList()));

        // Top 3 students
        System.out.println("Top 3:");
        students.stream()
                .sorted(Comparator.comparingInt((Student s) -> s.score).reversed())
                .limit(3)
                .forEach(s -> System.out.printf("  %-10s %d%n", s.name, s.score));

        // Grade distribution
        System.out.println("Grade distribution:");
        students.stream()
                .collect(Collectors.groupingBy(
                        s -> s.score >= 90 ? "A" :
                                s.score >= 75 ? "B" :
                                        s.score >= 60 ? "C" : "F",
                        Collectors.mapping(s -> s.name, Collectors.toList())))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue()));

        // Class statistics
        IntSummaryStatistics stats = students.stream()
                .collect(Collectors.summarizingInt(s -> s.score));
        System.out.printf("Stats: avg=%.1f min=%d max=%d%n",
                stats.getAverage(), stats.getMin(), stats.getMax());
    }

    // ─────────────────────────────────────────────
    // 4. ORDER PIPELINE
    // ─────────────────────────────────────────────
    static void orderPipeline() {
        List<Order> orders = Arrays.asList(
                new Order("O1", "Alice",   "Electronics", 45_000, "PENDING"),
                new Order("O2", "Bob",     "Books",          500, "SHIPPED"),
                new Order("O3", "Charlie", "Electronics", 12_000, "DELIVERED"),
                new Order("O4", "Diana",   "Books",          800, "PENDING"),
                new Order("O5", "Alice",   "Electronics", 75_000, "PENDING"),
                new Order("O6", "Eve",     "Clothing",     3_000, "CANCELLED"),
                new Order("O7", "Bob",     "Electronics", 22_000, "SHIPPED")
        );

        // Active Electronics orders > ₹10k, sorted by amount desc
        System.out.println("Active Electronics >10k:");
        orders.stream()
                .filter(o -> "Electronics".equals(o.category))
                .filter(o -> !o.status.equals("CANCELLED") && !o.status.equals("DELIVERED"))
                .filter(o -> o.amount > 10_000)
                .sorted(Comparator.comparingInt((Order o) -> o.amount).reversed())
                .forEach(o -> System.out.printf(
                        "  %-3s %-10s ₹%,6d  [%s]%n",
                        o.id, o.customer, o.amount, o.status));

        // Revenue by status
        System.out.println("Revenue by status:");
        orders.stream()
                .filter(o -> !"CANCELLED".equals(o.status))
                .collect(Collectors.groupingBy(
                        o -> o.status,
                        Collectors.summingInt(o -> o.amount)))
                .entrySet().stream()
                .sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
                .forEach(e -> System.out.printf(
                        "  %-12s ₹%,d%n", e.getKey(), e.getValue()));

        // Orders per customer
        System.out.println("Orders per customer:");
        orders.stream()
                .collect(Collectors.groupingBy(o -> o.customer, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String,Long>comparingByValue().reversed())
                .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue()));
    }

    // ─────────────────────────────────────────────
    // 5. WORD COUNT TOP-K
    // ─────────────────────────────────────────────
    static void wordCountTopK() {
        String text = "to be or not to be that is the question " +
                "whether tis nobler in the mind to suffer " +
                "the slings and arrows of outrageous fortune " +
                "or to take arms against a sea of troubles";

        int k = 5;

        Map<String, Long> freq = Arrays.stream(text.split("\\s+"))
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

        System.out.println("Top " + k + " words:");
        freq.entrySet().stream()
                .sorted(Map.Entry.<String,Long>comparingByValue().reversed())
                .limit(k)
                .forEach(e -> System.out.printf("  %-12s %d%n", e.getKey(), e.getValue()));

        // Words appearing only once
        long unique = freq.values().stream().filter(c -> c == 1).count();
        System.out.println("Unique words     : " + unique);

        // Average word length
        OptionalDouble avgLen = Arrays.stream(text.split("\\s+"))
                .distinct()
                .mapToInt(String::length)
                .average();
        System.out.printf("Avg word length  : %.2f%n", avgLen.getAsDouble());
    }

    // ─────────────────────────────────────────────
    // 6. FLAT INVOICE LINES
    // ─────────────────────────────────────────────
    static void invoiceLines() {
        List<Invoice> invoices = Arrays.asList(
                new Invoice("INV001", "Alice",   Arrays.asList(
                        new LineItem("Laptop",    75_000, 1),
                        new LineItem("Mouse",      1_500, 2))),
                new Invoice("INV002", "Bob",     Arrays.asList(
                        new LineItem("Phone",     45_000, 1),
                        new LineItem("Cover",        500, 3))),
                new Invoice("INV003", "Charlie", Arrays.asList(
                        new LineItem("Monitor",   22_000, 2),
                        new LineItem("Keyboard",   3_500, 1),
                        new LineItem("Mouse",      1_500, 1)))
        );

        // Flat list of all line items
        System.out.println("All line items:");
        invoices.stream()
                .flatMap(inv -> inv.lines.stream())
                .forEach(line -> System.out.printf(
                        "  %-12s ₹%,6d × %d = ₹%,d%n",
                        line.name, line.price, line.qty,
                        line.price * line.qty));

        // Total revenue across all invoices
        long totalRevenue = invoices.stream()
                .flatMap(inv -> inv.lines.stream())
                .mapToLong(line -> (long) line.price * line.qty)
                .sum();
        System.out.printf("Total revenue    : ₹%,d%n", totalRevenue);

        // Revenue per customer
        System.out.println("Revenue per customer:");
        invoices.stream()
                .collect(Collectors.toMap(
                        inv -> inv.customer,
                        inv -> inv.lines.stream()
                                .mapToLong(l -> (long)l.price * l.qty)
                                .sum()))
                .entrySet().stream()
                .sorted(Map.Entry.<String,Long>comparingByValue().reversed())
                .forEach(e -> System.out.printf(
                        "  %-10s ₹%,d%n", e.getKey(), e.getValue()));

        // Most expensive single item across all invoices
        invoices.stream()
                .flatMap(inv -> inv.lines.stream())
                .max(Comparator.comparingInt(l -> l.price))
                .ifPresent(l -> System.out.println("Most expensive   : " + l.name));
    }

    // ── Helpers ──────────────────────────────────
    static void sep(String title) {
        System.out.println("\n━━━ " + title + " ━━━\n");
    }

    // ── Models ───────────────────────────────────
    record Sale(String region, String product, int price, int qty) {}
    record LogEntry(String service, String level, String message) {}
    record Student(String name, int score) {}
    record Order(String id, String customer, String category, int amount, String status) {}
    record Invoice(String id, String customer, List<LineItem> lines) {}
    record LineItem(String name, int price, int qty) {}

}