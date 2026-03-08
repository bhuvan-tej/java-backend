package com.javabackend.java8.streams;

import java.util.*;
import java.util.stream.*;

/**
 *
 * Intermediate Operations
 *
 * Covers:
 *   filter, map, flatMap, distinct, sorted,
 *   peek, limit, skip, mapToInt/Double/Long
 *
 */
public class StreamIntermediateOps {

    public static void main(String[] args) {
        System.out.println("━━━ filter ━━━\n");       filterDemo();
        System.out.println("\n━━━ map ━━━\n");         mapDemo();
        System.out.println("\n━━━ flatMap ━━━\n");     flatMapDemo();
        System.out.println("\n━━━ distinct / sorted / limit / skip ━━━\n"); slicingDemo();
        System.out.println("\n━━━ peek ━━━\n");        peekDemo();
        System.out.println("\n━━━ mapToInt ━━━\n");    numericMapDemo();
    }

    static void filterDemo() {
        List<String> words = Arrays.asList(
                "stream", "lambda", "optional", "map", "filter", "collect", "hi");

        // Basic filter
        System.out.println("Length > 5:");
        words.stream()
                .filter(w -> w.length() > 5)
                .forEach(w -> System.out.println("  " + w));

        // Chained filters (each is AND)
        System.out.println("Length > 4 AND contains 'a':");
        words.stream()
                .filter(w -> w.length() > 4)
                .filter(w -> w.contains("a"))
                .forEach(w -> System.out.println("  " + w));

        // Filter on object field
        List<Employee> employees = employees();
        System.out.println("Engineering salary > 90k:");
        employees.stream()
                .filter(e -> "Engineering".equals(e.dept))
                .filter(e -> e.salary > 90_000)
                .forEach(e -> System.out.println("  " + e.name + " ₹" + e.salary));
    }

    static void mapDemo() {
        List<String> names = Arrays.asList("alice", "bob", "charlie");

        // String transformation
        System.out.println("Uppercase:");
        names.stream()
                .map(String::toUpperCase)
                .forEach(n -> System.out.println("  " + n));

        // Object → field
        System.out.println("Employee names:");
        employees().stream()
                .map(e -> e.name)
                .forEach(n -> System.out.println("  " + n));

        // Object → object
        System.out.println("Employee → DTO:");
        employees().stream()
                .map(e -> new EmployeeDTO(e.name, e.dept))
                .forEach(dto -> System.out.println("  " + dto));

        // Chained maps
        System.out.println("Length of names:");
        names.stream()
                .map(String::trim)
                .map(String::length)
                .forEach(l -> System.out.print(l + " "));
        System.out.println();
    }

    static void flatMapDemo() {
        // flatMap = map + flatten one level

        // List of lists → flat stream
        List<List<String>> nested = Arrays.asList(
                Arrays.asList("a","b","c"),
                Arrays.asList("d","e"),
                Arrays.asList("f","g","h","i")
        );
        System.out.print("Nested→flat    : ");
        nested.stream()
                .flatMap(Collection::stream)
                .forEach(s -> System.out.print(s + " "));
        System.out.println();

        // Split sentences into words
        List<String> sentences = Arrays.asList(
                "hello world",
                "java streams are powerful",
                "flat map is useful"
        );
        System.out.println("All unique words:");
        sentences.stream()
                .flatMap(s -> Arrays.stream(s.split(" ")))
                .distinct()
                .sorted()
                .forEach(w -> System.out.print(w + " "));
        System.out.println();

        // flatMap vs map — the difference
        // map:     Stream<String[]>  — each element is an array
        // flatMap: Stream<String>    — each element is a string
        System.out.println("\nmap gives Stream<String[]>:");
        sentences.stream()
                .map(s -> s.split(" "))     // Stream<String[]>
                .map(Arrays::toString)
                .forEach(s -> System.out.println("  " + s));

        System.out.println("flatMap gives Stream<String>:");
        sentences.stream()
                .flatMap(s -> Arrays.stream(s.split(" "))) // Stream<String>
                .limit(5)
                .forEach(w -> System.out.print(w + " "));
        System.out.println();
    }

    static void slicingDemo() {
        List<Integer> nums = Arrays.asList(5,3,1,4,1,5,9,2,6,5,3,5);

        System.out.println("distinct           : " +
                nums.stream().distinct().collect(Collectors.toList()));

        System.out.println("sorted             : " +
                nums.stream().sorted().collect(Collectors.toList()));

        System.out.println("sorted desc        : " +
                nums.stream().sorted(Comparator.reverseOrder())
                        .collect(Collectors.toList()));

        System.out.println("limit(3)           : " +
                nums.stream().sorted().limit(3).collect(Collectors.toList()));

        System.out.println("skip(9)            : " +
                nums.stream().sorted().skip(9).collect(Collectors.toList()));

        System.out.println("skip(3)+limit(4)   : " +
                nums.stream().sorted().skip(3).limit(4).collect(Collectors.toList()));

        // Pagination pattern
        int page = 1, pageSize = 3;
        System.out.println("Page " + page + " (size " + pageSize + "):");
        nums.stream()
                .sorted()
                .skip((long) page * pageSize)
                .limit(pageSize)
                .forEach(n -> System.out.print(n + " "));
        System.out.println();
    }

    static void peekDemo() {
        // peek — for DEBUG only, do not use for side effects
        // peek sees each element as it passes through the pipeline

        List<String> result = employees().stream()
                .peek(e -> System.out.println("  before filter : " + e.name))
                .filter(e -> e.salary > 80_000)
                .peek(e -> System.out.println("  after  filter : " + e.name))
                .map(e -> e.name)
                .peek(n -> System.out.println("  after  map    : " + n))
                .collect(Collectors.toList());

        System.out.println("Result: " + result);
    }

    static void numericMapDemo() {
        // mapToInt / mapToLong / mapToDouble — avoid boxing Integer→int
        System.out.println("mapToInt sum    : " +
                employees().stream().mapToInt(e -> e.salary).sum());

        System.out.println("mapToInt avg    : " +
                employees().stream().mapToInt(e -> e.salary).average().getAsDouble());

        System.out.println("mapToInt max    : " +
                employees().stream().mapToInt(e -> e.salary).max().getAsInt());

        // boxed() — IntStream → Stream<Integer>
        List<Integer> salaries = employees().stream()
                .mapToInt(e -> e.salary)
                .boxed()
                .collect(Collectors.toList());
        System.out.println("boxed salaries  : " + salaries);
    }

    // ── Data ───

    static List<Employee> employees() {
        return Arrays.asList(
                new Employee("Alice",   "Engineering", 95_000),
                new Employee("Bob",     "Engineering", 85_000),
                new Employee("Charlie", "Marketing",   72_000),
                new Employee("Diana",   "Engineering", 98_000),
                new Employee("Eve",     "Marketing",   68_000),
                new Employee("Frank",   "HR",          60_000)
        );
    }

    static class Employee {
        String name, dept; int salary;
        Employee(String n, String d, int s) { name=n; dept=d; salary=s; }
        public String toString() { return name; }
    }

    static class EmployeeDTO {
        String name, dept;
        EmployeeDTO(String n, String d) { name=n; dept=d; }
        public String toString() { return name + "(" + dept + ")"; }
    }

}