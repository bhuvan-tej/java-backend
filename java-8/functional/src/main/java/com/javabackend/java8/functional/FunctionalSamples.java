package com.javabackend.java8.functional;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 *
 * Functional Interfaces & Method References
 *
 * BUILT-IN FUNCTIONAL INTERFACES (java.util.function)
 *
 *   Predicate<T>         T → boolean         test()
 *   Function<T,R>        T → R               apply()
 *   BiFunction<T,U,R>    T,U → R             apply()
 *   Consumer<T>          T → void            accept()
 *   BiConsumer<T,U>      T,U → void          accept()
 *   Supplier<T>          () → T              get()
 *   UnaryOperator<T>     T → T               apply()
 *   BinaryOperator<T>    T,T → T             apply()
 *
 *   Primitive variants (no boxing):
 *   IntPredicate, LongPredicate, DoublePredicate
 *   IntFunction<R>, IntUnaryOperator, IntBinaryOperator
 *   ToIntFunction<T>, ToLongFunction<T>, ToDoubleFunction<T>
 *   IntConsumer, LongConsumer, DoubleConsumer
 *   IntSupplier, LongSupplier, DoubleSupplier
 *
 * METHOD REFERENCE TYPES
 *   ClassName::staticMethod       → Static method ref
 *   instance::instanceMethod      → Bound instance method ref
 *   ClassName::instanceMethod     → Unbound instance method ref
 *   ClassName::new                → Constructor ref
 *
 */
public class FunctionalSamples {

    public static void main(String[] args) {
        System.out.println("━━━ EXAMPLE 1 — Core Functional Interfaces ━━━\n");
        coreInterfaces();

        System.out.println("\n━━━ EXAMPLE 2 — Method References ━━━\n");
        methodReferences();

        System.out.println("\n━━━ EXAMPLE 3 — Composition ━━━\n");
        composition();

        System.out.println("\n━━━ EXAMPLE 4 — Senior Level ━━━\n");
        advLevel();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — Core Functional Interfaces
    // ─────────────────────────────────────────────
    static void coreInterfaces() {
        // ── Predicate<T> — T → boolean ──
        Predicate<String> isLong  = s -> s.length() > 5;
        Predicate<String> hasAt   = s -> s.contains("@");
        Predicate<String> isEmail = isLong.and(hasAt);

        System.out.println("isEmail(a@b.com)  : " + isEmail.test("a@b.com"));
        System.out.println("isEmail(nope)     : " + isEmail.test("nope"));

        // ── Function<T,R> — T → R ──
        Function<String, Integer> length = String::length;
        Function<Integer, String> stars  = n -> "*".repeat(n);
        Function<String, String>  bar    = length.andThen(stars);

        System.out.println("bar(hello)        : " + bar.apply("hello"));

        // ── BiFunction<T,U,R> — T,U → R ──
        BiFunction<String, Integer, String> repeat = (s, n) -> s.repeat(n);
        System.out.println("repeat(ha,3)      : " + repeat.apply("ha", 3));

        // ── Consumer<T> — T → void ──
        Consumer<String> print = System.out::println;
        Consumer<String> upper = s -> System.out.println(s.toUpperCase());
        Consumer<String> both  = print.andThen(upper);
        both.accept("hello");

        // ── BiConsumer<T,U> — T,U → void ──
        BiConsumer<String, Integer> printEntry =
                (k, v) -> System.out.println("  " + k + " = " + v);
        Map.of("a", 1, "b", 2).forEach(printEntry);

        // ── Supplier<T> — () → T ──
        Supplier<List<String>> listFactory = ArrayList::new;
        List<String> l1 = listFactory.get();
        List<String> l2 = listFactory.get();
        l1.add("A"); l2.add("B");
        System.out.println("l1=" + l1 + " l2=" + l2);

        // ── UnaryOperator<T> — T → T ──
        UnaryOperator<String> trim  = String::trim;
        UnaryOperator<String> upper2 = String::toUpperCase;
        UnaryOperator<String> clean = trim.andThen(upper2)::apply;
        System.out.println("clean( hello )    : " + clean.apply("  hello  "));

        // ── BinaryOperator<T> — T,T → T ──
        BinaryOperator<Integer> max = Integer::max;
        BinaryOperator<String>  longer = (a, b) ->
                a.length() >= b.length() ? a : b;
        System.out.println("max(3,7)          : " + max.apply(3, 7));
        System.out.println("longer            : " + longer.apply("hi", "hello"));

        // ── Primitive variants — avoid boxing ──
        IntPredicate isEven  = n -> n % 2 == 0;
        IntUnaryOperator dbl = n -> n * 2;
        IntBinaryOperator add = Integer::sum;

        System.out.println("isEven(4)         : " + isEven.test(4));
        System.out.println("dbl(5)            : " + dbl.applyAsInt(5));
        System.out.println("add(3,4)          : " + add.applyAsInt(3, 4));

        ToIntFunction<String> len = String::length;
        System.out.println("len(hello)        : " + len.applyAsInt("hello"));
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — Method References
    // ─────────────────────────────────────────────
    static void methodReferences() {
        List<String> names = Arrays.asList("Charlie", "Alice", "Bob", "Diana");

        // ── Static method reference: ClassName::staticMethod ──
        // Lambda:  n -> Integer.parseInt(n)
        // MRef:    Integer::parseInt
        Function<String, Integer> parse = Integer::parseInt;
        System.out.println("parseInt          : " + parse.apply("42"));

        // ── Unbound instance method ref: ClassName::instanceMethod ──
        // Lambda:  s -> s.toUpperCase()
        // MRef:    String::toUpperCase
        // "Unbound" — the instance (s) is supplied by the stream
        names.stream()
                .map(String::toUpperCase)
                .forEach(System.out::println);

        // ── Bound instance method ref: instance::instanceMethod ──
        // Lambda:  s -> System.out.println(s)
        // MRef:    System.out::println
        // "Bound" — System.out is a specific instance, already captured
        names.forEach(System.out::println);

        // ── Constructor reference: ClassName::new ──
        // Lambda:  () -> new ArrayList<>()
        // MRef:    ArrayList::new
        Supplier<List<String>> newList = ArrayList::new;
        List<String> list = newList.get();
        list.add("test");
        System.out.println("constructor ref   : " + list);

        // Constructor ref with argument
        // Lambda:  s -> new StringBuilder(s)
        Function<String, StringBuilder> newSB = StringBuilder::new;
        System.out.println("SB ref            : " + newSB.apply("hello"));

        // ── When NOT to use method reference ──
        // Use lambda when logic is more than just delegating to a method
        // Lambda is clearer here:
        names.stream()
                .filter(n -> n.length() > 3)          // condition — lambda
                .map(String::toLowerCase)              // simple delegation — ref
                .toList();

        // Comparing method ref vs lambda for same operation
        names.sort(String::compareTo);              // method ref
        names.sort((a, b) -> a.compareTo(b));       // equivalent lambda
        System.out.println("sorted            : " + names);
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — Composition
    // ─────────────────────────────────────────────
    static void composition() {
        // ── Predicate composition ──
        Predicate<Integer> isEven    = n -> n % 2 == 0;
        Predicate<Integer> isPositive = n -> n > 0;

        List<Integer> nums = Arrays.asList(-4, -3, -2, -1, 0, 1, 2, 3, 4, 5);
        System.out.println("even              : " +
                nums.stream().filter(isEven).toList());
        System.out.println("even AND positive : " +
                nums.stream().filter(isEven.and(isPositive)).toList());
        System.out.println("even OR positive  : " +
                nums.stream().filter(isEven.or(isPositive)).toList());
        System.out.println("not even          : " +
                nums.stream().filter(isEven.negate()).toList());

        // ── Function composition ──
        Function<Integer, Integer> times2  = x -> x * 2;
        Function<Integer, Integer> plus10  = x -> x + 10;

        // andThen — left to right
        System.out.println("times2.andThen(plus10)(3) : " +
                times2.andThen(plus10).apply(3));   // (3×2)+10 = 16

        // compose — right to left
        System.out.println("times2.compose(plus10)(3) : " +
                times2.compose(plus10).apply(3));   // (3+10)×2 = 26

        // Function.identity() — returns its input unchanged
        Function<String, String> identity = Function.identity();
        System.out.println("identity(hello)   : " + identity.apply("hello"));

        // ── Consumer composition ──
        Consumer<String> log   = s -> System.out.print("[LOG]  " + s);
        Consumer<String> audit = s -> System.out.println(" [AUDIT] " + s);
        Consumer<String> both  = log.andThen(audit);
        both.accept("user logged in");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — Adv Level
    //   Custom functional interfaces, higher-order
    //   functions, functional pipeline
    // ─────────────────────────────────────────────
    static void advLevel() {

        // ── Custom functional interface ──
        // When built-in interfaces don't fit (checked exceptions, 3+ args)
        TriFunction<String, Integer, Boolean, String> format =
                (name, age, active) ->
                        name + " | age=" + age + " | active=" + active;
        System.out.println("TriFunction       : " + format.apply("Alice", 30, true));

        // ── Higher-order functions ──
        // Function that returns a Function
        Function<String, Predicate<String>> startsWith =
                prefix -> s -> s.startsWith(prefix);

        List<String> words = Arrays.asList("apple","avocado","banana","apricot","blueberry");
        Predicate<String> startsA = startsWith.apply("a");
        Predicate<String> startsB = startsWith.apply("b");

        System.out.println("starts with a     : " +
                words.stream().filter(startsA).toList());
        System.out.println("starts with b     : " +
                words.stream().filter(startsB).toList());

        // ── Functional pipeline — behaviour injection ──
        List<Employee> employees = Arrays.asList(
                new Employee("Alice",   "Engineering", 95_000),
                new Employee("Bob",     "Marketing",   72_000),
                new Employee("Charlie", "Engineering", 85_000),
                new Employee("Diana",   "HR",          60_000)
        );

        // Process pipeline — filter, transform, sort — all injected
        List<String> result = process(
                employees,
                e -> "Engineering".equals(e.dept),    // which employees
                e -> e.name + "(₹" + e.salary/1000 + "k)", // how to display
                Comparator.comparingInt(e -> e.salary) // sort order
        );
        System.out.println("pipeline result   : " + result);

        // ── Memoization with Function + Map ──
        Map<Integer, Long> cache = new HashMap<>();
        Function<Integer, Long> fib = null; // forward declare
        // Note: true recursive memorized lambda needs a wrapper — shown conceptually
        Function<Integer, Long> fibMemo = n -> cache.computeIfAbsent(n, k ->
                k <= 1 ? k : cache.getOrDefault(k-1, 0L) + cache.getOrDefault(k-2, 0L));

        // Populate cache iteratively
        for (int i = 0; i <= 10; i++) fibMemo.apply(i);
        System.out.println("fib(10) cached    : " + cache.get(10));
    }

    // ── Higher-order process method ──
    static List<String> process(
            List<Employee> employees,
            Predicate<Employee> filter,
            Function<Employee, String> mapper,
            Comparator<Employee> sorter) {
        return employees.stream()
                .filter(filter)
                .sorted(sorter)
                .map(mapper)
                .collect(Collectors.toList());
    }

    // ── Custom functional interface ───
    @FunctionalInterface
    interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    // ── Model ──
    static class Employee {
        String name, dept; int salary;
        Employee(String n, String d, int s) { name=n; dept=d; salary=s; }
    }

}