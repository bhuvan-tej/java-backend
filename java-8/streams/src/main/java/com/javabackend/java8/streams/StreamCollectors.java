package com.javabackend.java8.streams;

import java.util.*;
import java.util.stream.*;

/**
 *
 * Collectors
 *
 * Covers:
 *   toList, toSet, toMap, groupingBy, partitioningBy,
 *   joining, counting, summarizingInt, averagingInt,
 *   maxBy, minBy, toUnmodifiableList, custom toMap with merge fn
 *
 */
public class StreamCollectors {

    public static void main(String[] args) {
        System.out.println("━━━ toList / toSet / toMap ━━━\n");     basicCollectors();
        System.out.println("\n━━━ groupingBy ━━━\n");                groupingByDemo();
        System.out.println("\n━━━ partitioningBy ━━━\n");            partitioningDemo();
        System.out.println("\n━━━ joining ━━━\n");                   joiningDemo();
        System.out.println("\n━━━ summarizing / counting ━━━\n");    summarizingDemo();
        System.out.println("\n━━━ toMap edge cases ━━━\n");          toMapEdgeCases();
    }

    static void basicCollectors() {
        List<Employee> employees = employees();

        // toList
        List<String> names = employees.stream()
                .map(e -> e.name)
                .collect(Collectors.toList());
        System.out.println("toList           : " + names);

        // toSet — no duplicates, no order
        Set<String> depts = employees.stream()
                .map(e -> e.dept)
                .collect(Collectors.toSet());
        System.out.println("toSet (depts)    : " + depts);

        // toMap — key + value extractor
        Map<String, Integer> nameSalary = employees.stream()
                .collect(Collectors.toMap(
                        e -> e.name,
                        e -> e.salary));
        System.out.println("toMap name→sal   : " + nameSalary);

        // toUnmodifiableList (Java 10+)
        List<String> immutable = employees.stream()
                .map(e -> e.name)
                .collect(Collectors.toUnmodifiableList());
        System.out.println("unmodifiable     : " + immutable);
        try {
            immutable.add("X");
        } catch (UnsupportedOperationException e) {
            System.out.println("add rejected     : UnsupportedOperationException ✓");
        }
    }

    static void groupingByDemo() {
        List<Employee> employees = employees();

        // Basic groupingBy
        Map<String, List<Employee>> byDept =
                employees.stream().collect(Collectors.groupingBy(e -> e.dept));
        System.out.println("By dept (lists):");
        byDept.forEach((dept, emps) ->
                System.out.println("  " + dept + " → " +
                        emps.stream().map(e -> e.name).collect(Collectors.toList())));

        // groupingBy + counting
        Map<String, Long> countByDept = employees.stream()
                .collect(Collectors.groupingBy(e -> e.dept, Collectors.counting()));
        System.out.println("Count by dept    : " + countByDept);

        // groupingBy + averagingInt
        Map<String, Double> avgSalByDept = employees.stream()
                .collect(Collectors.groupingBy(
                        e -> e.dept,
                        Collectors.averagingInt(e -> e.salary)));
        System.out.println("Avg sal by dept  : " + avgSalByDept);

        // groupingBy + maxBy
        Map<String, Optional<Employee>> topByDept = employees.stream()
                .collect(Collectors.groupingBy(
                        e -> e.dept,
                        Collectors.maxBy(Comparator.comparingInt(e -> e.salary))));
        System.out.println("Highest paid/dept:");
        topByDept.forEach((dept, emp) ->
                emp.ifPresent(e -> System.out.println(
                        "  " + dept + " → " + e.name + " ₹" + e.salary)));

        // groupingBy + mapping (extract names per dept)
        Map<String, List<String>> namesByDept = employees.stream()
                .collect(Collectors.groupingBy(
                        e -> e.dept,
                        Collectors.mapping(e -> e.name, Collectors.toList())));
        System.out.println("Names by dept    : " + namesByDept);

        // Multi-level groupingBy
        Map<String, Map<Boolean, List<Employee>>> byDeptAndRemote =
                employees.stream()
                        .collect(Collectors.groupingBy(
                                e -> e.dept,
                                Collectors.groupingBy(e -> e.remote)));
        System.out.println("Dept → remote?:");
        byDeptAndRemote.forEach((dept, remoteMap) -> {
            List<Employee> remote = remoteMap.getOrDefault(true, Collections.emptyList());
            System.out.println("  " + dept + " remote: " +
                    remote.stream().map(e -> e.name).collect(Collectors.toList()));
        });
    }

    static void partitioningDemo() {
        List<Employee> employees = employees();

        // Splits into exactly 2 groups: true and false
        Map<Boolean, List<Employee>> byRemote =
                employees.stream()
                        .collect(Collectors.partitioningBy(e -> e.remote));

        System.out.println("Remote    : " +
                byRemote.get(true).stream().map(e -> e.name).collect(Collectors.toList()));
        System.out.println("On-site   : " +
                byRemote.get(false).stream().map(e -> e.name).collect(Collectors.toList()));

        // partitioningBy + downstream
        Map<Boolean, Long> countByRemote = employees.stream()
                .collect(Collectors.partitioningBy(e -> e.remote, Collectors.counting()));
        System.out.println("Count remote/onsite: " + countByRemote);

        // partitioningBy + summarizingInt
        Map<Boolean, IntSummaryStatistics> statsByRemote = employees.stream()
                .collect(Collectors.partitioningBy(
                        e -> e.remote,
                        Collectors.summarizingInt(e -> e.salary)));
        System.out.printf("Remote avg salary  : ₹%,.0f%n",
                statsByRemote.get(true).getAverage());
        System.out.printf("Onsite avg salary  : ₹%,.0f%n",
                statsByRemote.get(false).getAverage());
    }

    static void joiningDemo() {
        List<String> names = employees().stream()
                .map(e -> e.name)
                .collect(Collectors.toList());

        // Simple join — no delimiter
        System.out.println("joining()        : " +
                names.stream().collect(Collectors.joining()));

        // With delimiter
        System.out.println("joining(', ')    : " +
                names.stream().collect(Collectors.joining(", ")));

        // With prefix and suffix
        System.out.println("joining full     : " +
                names.stream().collect(Collectors.joining(", ", "[", "]")));

        // CSV line
        String csv = employees().stream()
                .map(e -> e.name + "," + e.dept + "," + e.salary)
                .collect(Collectors.joining("\n"));
        System.out.println("CSV:\n" + csv);
    }

    static void summarizingDemo() {
        List<Employee> employees = employees();

        // counting
        System.out.println("counting         : " +
                employees.stream().collect(Collectors.counting()));

        // averagingInt
        System.out.printf("averagingInt     : ₹%,.0f%n",
                employees.stream().collect(Collectors.averagingInt(e -> e.salary)));

        // summarizingInt — count + sum + min + max + avg in one pass
        IntSummaryStatistics stats = employees.stream()
                .collect(Collectors.summarizingInt(e -> e.salary));
        System.out.printf("summarizingInt   : count=%d min=₹%,d max=₹%,d avg=₹%,.0f%n",
                stats.getCount(), stats.getMin(), stats.getMax(), stats.getAverage());

        // summingInt
        System.out.printf("summingInt       : ₹%,d%n",
                employees.stream().collect(Collectors.summingInt(e -> e.salary)));
    }

    static void toMapEdgeCases() {
        List<Employee> employees = employees();

        // toMap with merge function — handles duplicate keys
        // Duplicate key → IllegalStateException without merge fn
        List<Employee> withDupe = new ArrayList<>(employees);
        withDupe.add(new Employee("Alice", "HR", 55_000, false)); // duplicate name!

        Map<String, Integer> safeMerge = withDupe.stream()
                .collect(Collectors.toMap(
                        e -> e.name,
                        e -> e.salary,
                        (existing, replacement) -> Math.max(existing, replacement))); // keep higher
        System.out.println("merge (keep max) : " + safeMerge.get("Alice"));

        // toMap with specific map type (LinkedHashMap preserves insertion order)
        Map<String, Integer> ordered = employees.stream()
                .collect(Collectors.toMap(
                        e -> e.name,
                        e -> e.salary,
                        (a, b) -> a,
                        LinkedHashMap::new));
        System.out.println("ordered toMap    : " + ordered.keySet());
    }

    // ── Data ──
    static List<Employee> employees() {
        return Arrays.asList(
                new Employee("Alice",   "Engineering", 95_000, false),
                new Employee("Bob",     "Engineering", 85_000, true),
                new Employee("Charlie", "Marketing",   72_000, false),
                new Employee("Diana",   "Engineering", 98_000, true),
                new Employee("Eve",     "Marketing",   68_000, true),
                new Employee("Frank",   "HR",          60_000, false)
        );
    }

    static class Employee {
        String name, dept; int salary; boolean remote;
        Employee(String n, String d, int s, boolean r) {
            name=n; dept=d; salary=s; remote=r; }
        public String toString() { return name; }
    }

}