package com.javabackend.java8.streams;

import java.util.*;
import java.util.stream.*;

/**
 *
 * Terminal Operations
 *
 * Covers:
 *   collect, count, reduce, min, max,
 *   findFirst, findAny, anyMatch, allMatch,
 *   noneMatch, forEach, toArray
 *
 */
public class StreamTerminalOps {

    public static void main(String[] args) {
        System.out.println("━━━ count / collect ━━━\n");   countAndCollect();
        System.out.println("\n━━━ reduce ━━━\n");            reduceDemo();
        System.out.println("\n━━━ min / max ━━━\n");         minMaxDemo();
        System.out.println("\n━━━ match / find ━━━\n");      matchAndFind();
        System.out.println("\n━━━ forEach / toArray ━━━\n"); forEachAndToArray();
    }

    static void countAndCollect() {
        List<Employee> employees = employees();

        System.out.println("count all        : " + employees.stream().count());
        System.out.println("count engg       : " +
                employees.stream().filter(e -> "Engineering".equals(e.dept)).count());

        List<String> names = employees.stream()
                .map(e -> e.name).collect(Collectors.toList());
        System.out.println("toList           : " + names);

        Set<String> depts = employees.stream()
                .map(e -> e.dept).collect(Collectors.toSet());
        System.out.println("toSet (depts)    : " + depts);
    }

    static void reduceDemo() {
        List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);

        // reduce with identity — always returns T
        int sum = nums.stream().reduce(0, Integer::sum);
        System.out.println("sum (identity 0) : " + sum);

        int product = nums.stream().reduce(1, (a, b) -> a * b);
        System.out.println("product          : " + product);

        // reduce without identity — returns Optional<T>
        Optional<Integer> max = nums.stream().reduce(Integer::max);
        max.ifPresent(v -> System.out.println("max (no identity): " + v));

        // reduce on objects
        Optional<String> longest = employees().stream()
                .map(e -> e.name)
                .reduce((a, b) -> a.length() >= b.length() ? a : b);
        longest.ifPresent(n -> System.out.println("longest name     : " + n));

        // Total salary via reduce
        int total = employees().stream()
                .mapToInt(e -> e.salary)
                .reduce(0, Integer::sum);
        System.out.printf("total salary     : ₹%,d%n", total);
    }

    static void minMaxDemo() {
        List<Employee> employees = employees();

        employees.stream()
                .min(Comparator.comparingInt(e -> e.salary))
                .ifPresent(e -> System.out.printf(
                        "lowest paid      : %-10s ₹%,d%n", e.name, e.salary));

        employees.stream()
                .max(Comparator.comparingInt(e -> e.salary))
                .ifPresent(e -> System.out.printf(
                        "highest paid     : %-10s ₹%,d%n", e.name, e.salary));

        // On numeric stream — no Comparator needed
        System.out.println("IntStream min    : " +
                employees.stream().mapToInt(e -> e.salary).min().getAsInt());
        System.out.println("IntStream max    : " +
                employees.stream().mapToInt(e -> e.salary).max().getAsInt());
    }

    static void matchAndFind() {
        List<Employee> employees = employees();

        // anyMatch — short-circuits on first true
        System.out.println("any remote       : " +
                employees.stream().anyMatch(e -> e.remote));

        // allMatch — short-circuits on first false
        System.out.println("all salary > 0   : " +
                employees.stream().allMatch(e -> e.salary > 0));

        // noneMatch — short-circuits on first true
        System.out.println("none negative sal: " +
                employees.stream().noneMatch(e -> e.salary < 0));

        // findFirst — always returns same element (ordered)
        employees.stream()
                .filter(e -> e.salary > 90_000)
                .findFirst()
                .ifPresent(e -> System.out.println("findFirst >90k   : " + e.name));

        // findAny — may return any element (useful in parallel)
        employees.stream()
                .filter(e -> "Marketing".equals(e.dept))
                .findAny()
                .ifPresent(e -> System.out.println("findAny mktg     : " + e.name));

        // Empty stream edge cases
        System.out.println("empty anyMatch   : " +
                Stream.empty().anyMatch(x -> true));   // false
        System.out.println("empty allMatch   : " +
                Stream.empty().allMatch(x -> false));  // true (vacuously)
        System.out.println("empty noneMatch  : " +
                Stream.empty().noneMatch(x -> true));  // true
    }

    static void forEachAndToArray() {
        List<Employee> employees = employees();

        // forEach — terminal, processes each element
        System.out.print("forEach names    : ");
        employees.stream()
                .map(e -> e.name)
                .forEach(n -> System.out.print(n + " "));
        System.out.println();

        // toArray — with generator for typed array
        String[] arr = employees.stream()
                .map(e -> e.name)
                .toArray(String[]::new);
        System.out.println("toArray length   : " + arr.length);
        System.out.println("toArray[0]       : " + arr[0]);

        // Object[] without generator
        Object[] objs = employees.stream().toArray();
        System.out.println("Object[] length  : " + objs.length);
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