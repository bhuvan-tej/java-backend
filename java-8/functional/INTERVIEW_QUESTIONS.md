# 🎯 Interview Questions — Functional Interfaces & Method References

---

**Q1. What is a functional interface? What does `@FunctionalInterface` do?**

> A functional interface is an interface with exactly one abstract method.
> Lambdas and method references can be assigned to functional interfaces
> because the lambda provides the implementation of that single method.
>
> `@FunctionalInterface` is an optional annotation that tells the compiler
> to enforce the single-abstract-method contract. If you accidentally add
> a second abstract method, the compiler rejects it immediately:
> ```
> @FunctionalInterface
> interface Transformer {
>     String transform(String input); // OK — one abstract method
>     String another(String input);   // compile error — two abstract methods!
> }
> ```
>
> Default and static methods are allowed and don't count toward the limit.
> Without the annotation the interface still works as a functional interface —
> the annotation just adds compile-time safety.

---

**Q2. What are the four core functional interfaces and when do you use each?**

> | Interface | Signature | Use when |
> |-----------|-----------|----------|
> | `Predicate<T>` | `T → boolean` | Testing a condition — filter, validation |
> | `Function<T,R>` | `T → R` | Transforming a value — map, convert |
> | `Consumer<T>` | `T → void` | Consuming a value with side effect — print, save |
> | `Supplier<T>` | `() → T` | Producing a value — factory, lazy init |
>
> ```
> Predicate<String> isEmail  = s -> s.contains("@");
> Function<String, Integer> length = String::length;
> Consumer<String> log       = s -> logger.info(s);
> Supplier<Connection> conn  = () -> dataSource.getConnection();
> ```
>
> `UnaryOperator<T>` and `BinaryOperator<T>` are specialisations of Function
> for when input and output types are the same — common for transformations
> that return the same type (`String::trim`, `Integer::max`).

---

**Q3. What are the four types of method references? Give an example of each.**

> ```
> // 1. Static method reference — ClassName::staticMethod
> Function<String, Integer> parse = Integer::parseInt;
> // equivalent lambda: s -> Integer.parseInt(s)
>
> // 2. Unbound instance method ref — ClassName::instanceMethod
> Function<String, String> upper = String::toUpperCase;
> // equivalent lambda: s -> s.toUpperCase()
> // "unbound" — the instance comes from the stream/caller
>
> // 3. Bound instance method ref — instance::instanceMethod
> Consumer<String> print = System.out::println;
> // equivalent lambda: s -> System.out.println(s)
> // "bound" — System.out is a specific captured instance
>
> // 4. Constructor reference — ClassName::new
> Supplier<List<String>> newList = ArrayList::new;
> // equivalent lambda: () -> new ArrayList<>()
> ```
>
> The unbound/bound distinction is the one that trips people up. Unbound
> means the instance the method is called on is provided at the call site
> (first parameter). Bound means the instance is already captured in the reference.

---

**Q4. When should you use a method reference vs a lambda?**

> Use a method reference when the lambda does nothing but call a single
> existing method — it is shorter and removes noise:
> ```
> // Method reference — cleaner
> list.stream().map(String::toUpperCase).forEach(System.out::println);
>
> // Lambda — same thing, more verbose
> list.stream().map(s -> s.toUpperCase()).forEach(s -> System.out.println(s));
> ```
>
> Use a lambda when:
> - The logic is more than just delegating to one method
> - The method reference would be ambiguous or confusing
> - Parameters need to be reordered or transformed before the call
>
> ```
> // Lambda is clearer here — condition is explicit
> list.stream().filter(s -> s.length() > 3 && !s.isEmpty())
>
> // This method reference would require a separate method — not worth it
> list.stream().filter(MyUtil::isValidAndLong)
> ```
>
> Method references are purely cosmetic — they compile to the same bytecode
> as the equivalent lambda. Never sacrifice clarity for brevity.

---

**Q5. What is the difference between `Function.andThen()` and `Function.compose()`?**

> Both compose two functions but in opposite directions:
>
> `f.andThen(g)` — apply f first, then g. Left to right: `g(f(x))`.
>
> `f.compose(g)` — apply g first, then f. Right to left: `f(g(x))`.
>
> ```
> Function<Integer, Integer> times2 = x -> x * 2;
> Function<Integer, Integer> plus10 = x -> x + 10;
>
> times2.andThen(plus10).apply(3);  // (3×2)+10 = 16  — times2 first
> times2.compose(plus10).apply(3);  // (3+10)×2 = 26  — plus10 first
> ```
>
> `andThen` is used far more often — it reads in execution order like English.
> `compose` matches mathematical notation f∘g but reads right to left.

---

**Q6. Why do primitive functional interfaces exist? When do you use them?**

> Generic functional interfaces like `Function<Integer, Integer>` require boxing —
> converting primitive `int` to `Integer` objects on every call. In tight loops
> or high-throughput code this creates significant GC pressure.
>
> Primitive variants work directly with primitives — zero boxing:
> ```
> // Function<Integer, Integer> — boxes int → Integer on every call
> Function<Integer, Integer> dbl = n -> n * 2;
>
> // IntUnaryOperator — no boxing
> IntUnaryOperator dbl = n -> n * 2;
>
> // ToIntFunction<T> — extract int from object without boxing
> ToIntFunction<String> len = String::length;
> employees.stream().mapToInt(e -> e.salary).sum(); // IntStream — no boxing
> ```
>
> Use primitive variants whenever:
> - Working with numeric data in streams (`mapToInt`, `mapToLong`, `mapToDouble`)
> - The functional interface is used in a tight loop
> - You are accumulating or computing numeric results

---

**Q7. How do you create a custom functional interface? When would you need one?**

> ```
> @FunctionalInterface
> interface TriFunction<A, B, C, R> {
>     R apply(A a, B b, C c);
> }
>
> TriFunction<String, Integer, Boolean, String> format =
>     (name, age, active) -> name + " | " + age + " | " + active;
> ```
>
> You need a custom functional interface when:
>
> 1. Built-in interfaces don't match your signature (3+ parameters, specific primitives)
> 2. You need to declare checked exceptions — built-in interfaces don't throw checked exceptions:
> ```
> @FunctionalInterface
> interface ThrowingFunction<T, R> {
>     R apply(T t) throws Exception; // allows checked exceptions
> }
> ```
> 3. You want a domain-specific name for clarity:
> ```
> @FunctionalInterface
> interface PriceCalculator {
>     double calculate(double base, double taxRate); // clearer than BiFunction<Double,Double,Double>
> }
> ```

---

**Q8. What is a higher-order function? Give a production example.**

> A higher-order function either takes a function as a parameter or returns
> a function as a result (or both).
>
> Java's `Stream.filter()`, `map()`, `sorted()` are all higher-order functions
> — they take functional interfaces as parameters.
>
> Production example — a factory that produces Predicates:
> ```
> // Returns a Predicate configured with the given prefix
> Function<String, Predicate<String>> startsWith =
>     prefix -> s -> s.startsWith(prefix);
>
> Predicate<String> startsA = startsWith.apply("a");
> Predicate<String> startsB = startsWith.apply("b");
>
> words.stream().filter(startsA).collect(Collectors.toList());
> words.stream().filter(startsB).collect(Collectors.toList());
> ```
>
> Real use case — a configurable validation service where each field has
> its own Predicate generated from config at startup, rather than hardcoded
> `if` statements scattered through the code.

---

**Q9. How do you inject behaviour into a method using functional interfaces?**

> Define method parameters as functional interfaces — callers pass the
> behaviour as lambdas or method references:
>
> ```
> // Generic process method — behaviour fully injected
> static <T, R> List<R> process(
>         List<T> data,
>         Predicate<T>    filter,
>         Function<T, R>  mapper,
>         Comparator<T>   sorter) {
>     return data.stream()
>         .filter(filter)
>         .sorted(sorter)
>         .map(mapper)
>         .collect(Collectors.toList());
> }
>
> // Caller controls all behaviour at call site
> List<String> result = process(
>     employees,
>     e -> "Engineering".equals(e.dept),
>     e -> e.name + "(₹" + e.salary/1000 + "k)",
>     Comparator.comparingInt(e -> e.salary));
> ```
>
> This is the Strategy Pattern implemented with lambdas — no extra classes,
> no interface implementations, behaviour passed inline. The `process` method
> is reusable for any filtering, mapping, and sorting combination.

---

**Q10. What is `Function.identity()` and where is it useful?**

> `Function.identity()` returns a function that always returns its input
> unchanged — equivalent to `x -> x`.
>
> ```
> Function<String, String> identity = Function.identity();
> identity.apply("hello"); // "hello"
> ```
>
> Useful in three scenarios:
>
> 1. **`Collectors.toMap` when key = value:**
> ```
> // Collect strings into a Map<String, String> where key = value
> Map<String, String> map = strings.stream()
>     .collect(Collectors.toMap(Function.identity(), String::toUpperCase));
> ```
>
> 2. **Pipeline neutral element** — when building a pipeline dynamically
     >    and you need a starting point that does nothing:
> ```
> Function<String, String> pipeline = Function.identity();
> if (shouldTrim)  pipeline = pipeline.andThen(String::trim);
> if (shouldUpper) pipeline = pipeline.andThen(String::toUpperCase);
> ```
>
> 3. **Grouping where the key is the element itself:**
> ```
> // Group strings by themselves (count frequency)
> Map<String, Long> freq = words.stream()
>     .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
> ```