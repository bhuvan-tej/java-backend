package com.javabackend.java8.lambdas;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * LAMBDAS
 *
 * WHAT IS A LAMBDA?
 *  A lambda is an anonymous function — a block of code that can be passed around like a value.
 *
 * SYNTAX
 *  (parameters) -> expression
 *  (parameters) -> { statements; }
 *
 * A lambda can only be assigned to a FUNCTIONAL INTERFACE
 *  — an interface with exactly ONE abstract method.
 *  The lambda is the implementation of that one method.
 *
 * WHY LAMBDAS?
 *  Before Java 8: pass behavior via Anonymous Inner Classes (verbose)
 *  After  Java 8: pass behavior via Lambdas (concise, readable)
 *
 * VARIABLE CAPTURE
 *  Lambdas can capture local variables from enclosing scope but
 *  ONLY if they are effectively final (not reassigned after capture)
 *
 */
public class LambdaSamples {

    public static void main(String[] args) {
        System.out.println("━━━ EXAMPLE 1 — Syntax Evolution ━━━\n");
        syntaxEvolution();

        System.out.println("\n━━━ EXAMPLE 2 — Passing Lambdas ━━━\n");
        passingLambdas();

        System.out.println("\n━━━ EXAMPLE 3 — Variable Capture ━━━\n");
        variableCapture();

        System.out.println("\n━━━ EXAMPLE 4 — Senior Level ━━━\n");
        advLevel();
    }

    // Syntax evolution
    // Anonymous class → Lambda → Method reference
    static void syntaxEvolution() {
        List<String> names = Arrays.asList("Charlie", "Alice", "Bob", "Diana");

        // ── Pre Java 8 — Anonymous Inner Class ──
        Collections.sort(names, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return a.compareTo(b);
            }
        });
        System.out.println("Anonymous class sort : " + names);

        // ── Java 8 — Lambda (explicit types) ──
        names.sort((String a, String b) -> a.compareTo(b));
        System.out.println("Lambda (typed)       : " + names);

        // ── Java 8 — Lambda (inferred types) ──
        names.sort((a, b) -> a.compareTo(b));
        System.out.println("Lambda (inferred)    : " + names);

        // ── Java 8 — Method reference (cleanest) ──
        names.sort(String::compareTo);
        System.out.println("Method reference     : " + names);

        // ── Single expression vs block body ──
        // Single expression — no return, no braces
        Runnable greet = () -> System.out.println("Hello from lambda!");

        // Block body — braces required, explicit return if needed
        Runnable greetBlock = () -> {
            String msg = "Hello";
            System.out.println(msg + " from block lambda!");
        };

        greet.run();
        greetBlock.run();
    }

    // Passing lambdas as behavior
    // Strategy pattern without boilerplate
    static void passingLambdas() {

        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        // ── Predicate<T> — test a condition ──
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> isGt5  = n -> n > 5;

        System.out.println("Even numbers  : " + filter(numbers, isEven));
        System.out.println("Greater than 5: " + filter(numbers, isGt5));

        // Compose predicates — and / or / negate
        Predicate<Integer> evenAndGt5 = isEven.and(isGt5);
        Predicate<Integer> evenOrGt5  = isEven.or(isGt5);
        Predicate<Integer> notEven    = isEven.negate();

        System.out.println("Even AND > 5  : " + filter(numbers, evenAndGt5));
        System.out.println("Even OR  > 5  : " + filter(numbers, evenOrGt5));
        System.out.println("Not even      : " + filter(numbers, notEven));

        // ── Function<T,R> — transform a value ──
        Function<String, Integer> strLen  = String::length;
        Function<Integer, String> intToHex = Integer::toHexString;

        // andThen — compose left to right
        Function<String, String> lenToHex = strLen.andThen(intToHex);

        List<String> words = Arrays.asList("hi", "hello", "java");
        words.forEach(w -> System.out.printf(
                "  %-8s len=%d hex=%s%n", w, strLen.apply(w), lenToHex.apply(w)));

        // ── Consumer<T> — consume a value, return nothing ──
        Consumer<String> print  = System.out::println;
        Consumer<String> upper  = s -> System.out.println(s.toUpperCase());
        Consumer<String> both   = print.andThen(upper); // chain consumers

        System.out.println("Consumer chain:");
        both.accept("hello");

        // ── Supplier<T> — supply a value, take nothing ──
        Supplier<List<String>> listFactory = ArrayList::new;
        List<String> list1 = listFactory.get();
        List<String> list2 = listFactory.get(); // fresh instance each time
        list1.add("A"); list2.add("B");
        System.out.println("list1=" + list1 + " list2=" + list2);
    }

    // Variable capture rules
    static void variableCapture() {

        // ── Effectively final — can be captured ──
        String prefix = "Hello"; // never reassigned → effectively final
        Runnable r = () -> System.out.println(prefix + " World");
        r.run();

        // ── Instance/static fields — always capturable ──
        // (Unlike local variables, fields don't need to be final)

        // ── this reference — captured implicitly ──
        // Inside an instance lambda, 'this' refers to the enclosing class

        // ── The effectively final rule in practice ──
        int base = 10; // effectively final — never reassigned
        Function<Integer, Integer> adder = n -> n + base;
        System.out.println("adder(5) = " + adder.apply(5));

        // Common workaround when you need a "mutable" captured variable
        // Use a single-element array or AtomicInteger
        int[] counter = {0}; // array reference is final, element is mutable
        List<String> items = Arrays.asList("a", "b", "c");
        items.forEach(item -> counter[0]++);
        System.out.println("Counter via array: " + counter[0]);

        // ── Closure semantics demo ──
        List<Runnable> actions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final int captured = i; // must capture a new final each iteration
            actions.add(() -> System.out.println("Action " + captured));
        }
        System.out.println("Closure values:");
        actions.forEach(Runnable::run);
    }


    // Adv Level
    //  Strategy pattern, lazy evaluation,
    //  lambda composition, currying
    static void advLevel() {

        // ── Strategy Pattern — behavior injection ──
        // Validator takes a Predicate — swap rules without changing class
        System.out.println("── Validator (Strategy Pattern) ──");
        Validator<String> emailValidator = new Validator<>(
                s -> s.contains("@") && s.contains(".") && s.length() > 5);
        Validator<Integer> ageValidator = new Validator<>(
                n -> n >= 18 && n <= 120);

        System.out.println("test@co.com valid : " + emailValidator.validate("test@co.com"));
        System.out.println("invalid valid     : " + emailValidator.validate("invalid"));
        System.out.println("age 25 valid      : " + ageValidator.validate(25));
        System.out.println("age 15 valid      : " + ageValidator.validate(15));

        // ── Lazy evaluation ──
        // Supplier defers computation until actually needed
        System.out.println("\n── Lazy Evaluation ──");
        Supplier<String> expensiveOp = () -> {
            System.out.println("  [computing expensive result...]");
            return "result";
        };
        System.out.println("Supplier created — no computation yet");
        System.out.println("Getting result: " + expensiveOp.get()); // computed here

        // ── Function composition ──
        System.out.println("\n── Function Composition ──");
        Function<Integer, Integer> times2  = x -> x * 2;
        Function<Integer, Integer> plus10  = x -> x + 10;
        Function<Integer, Integer> squared = x -> x * x;

        // andThen → left to right: times2 then plus10
        Function<Integer, Integer> times2ThenPlus10 = times2.andThen(plus10);
        // compose → right to left: plus10 then times2
        Function<Integer, Integer> plus10ThenTimes2 = times2.compose(plus10);

        System.out.println("times2.andThen(plus10).apply(3) = "
                + times2ThenPlus10.apply(3));  // (3×2)+10 = 16
        System.out.println("times2.compose(plus10).apply(3) = "
                + plus10ThenTimes2.apply(3));  // (3+10)×2 = 26

        // Chain of 3
        Function<Integer, Integer> pipeline = times2.andThen(plus10).andThen(squared);
        System.out.println("times2→plus10→squared(3) = "
                + pipeline.apply(3));          // ((3×2)+10)² = 256

        // ── Currying ──
        // Transform a 2-arg function into a chain of 1-arg functions
        System.out.println("\n── Currying ──");
        Function<Integer, Function<Integer, Integer>> curriedAdd =
                a -> b -> a + b;

        Function<Integer, Integer> add5  = curriedAdd.apply(5);  // partial application
        Function<Integer, Integer> add10 = curriedAdd.apply(10);

        System.out.println("add5.apply(3)  = " + add5.apply(3));   // 8
        System.out.println("add10.apply(3) = " + add10.apply(3));  // 13

        // Real use case: partially applied discount calculator
        Function<Double, Function<Double, Double>> discount =
                rate -> price -> price * (1 - rate);

        Function<Double, Double> tenPercOff  = discount.apply(0.10);
        Function<Double, Double> twentyPercOff = discount.apply(0.20);

        System.out.println("10% off ₹1000  = ₹" + tenPercOff.apply(1000.0));
        System.out.println("20% off ₹1000  = ₹" + twentyPercOff.apply(1000.0));
    }

    // ── Helper method — accepts lambda as Predicate ──
    static <T> List<T> filter(List<T> list, Predicate<T> predicate) {
        return list.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    // ── Validator — Strategy Pattern with Lambda ──
    static class Validator<T> {
        private final Predicate<T> rule;
        Validator(Predicate<T> rule) { this.rule = rule; }
        boolean validate(T value)    { return rule.test(value); }
    }

}
