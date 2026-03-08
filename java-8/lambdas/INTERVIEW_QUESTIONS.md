# 🎯 Interview Questions — Lambdas

---

> Lambda questions at we gain experience, we should go beyond syntax. Interviewers expect
> you to explain the effectively final rule, how lambdas differ from
> anonymous classes at the bytecode level, checked exception handling,
> and functional composition patterns used in production.

---

## Core Concepts

**Q1. What is a lambda expression and what problem does it solve?**

> A lambda is an anonymous function — a block of code that can be passed
> around as a value and executed later. It implements the single abstract
> method of a functional interface.
>
> The problem it solves: before Java 8, passing behaviour required anonymous
> inner classes — verbose boilerplate for what is often one line of logic:
> ```
> // Pre Java 8 — 6 lines for one comparison
> Collections.sort(names, new Comparator<String>() {
>     @Override
>     public int compare(String a, String b) {
>         return a.compareTo(b);
>     }
> });
>
> // Java 8 — same thing, one line
> names.sort((a, b) -> a.compareTo(b));
> ```
>
> Beyond conciseness, lambdas enable functional programming patterns:
> passing behaviour as arguments, returning behaviour from methods,
> composing functions, and lazy evaluation — none of which were natural
> in pre-Java-8 code.

---

**Q2. What is a functional interface? Can you create your own?**

> A functional interface is an interface with exactly one abstract method.
> Lambdas can only be assigned to functional interfaces — the lambda
> provides the implementation of that one method.
>
> ```
> @FunctionalInterface  // optional annotation — enforced by compiler
> interface StringTransformer {
>     String transform(String input); // exactly one abstract method
>
>     // default and static methods are allowed — they don't count
>     default StringTransformer andThen(StringTransformer next) {
>         return input -> next.transform(this.transform(input));
>     }
> }
>
> StringTransformer upper  = s -> s.toUpperCase();
> StringTransformer trim   = s -> s.trim();
> StringTransformer both   = trim.andThen(upper);
>
> System.out.println(both.transform("  hello  ")); // "HELLO"
> ```
>
> `@FunctionalInterface` is optional but recommended — the compiler rejects
> the interface if you accidentally add a second abstract method, protecting
> the contract.

---

**Q3. How does a lambda differ from an anonymous inner class at the JVM level?**

> They look similar but are compiled very differently:
>
> **Anonymous inner class:**
> - Compiled to a separate `.class` file (`Outer$1.class`)
> - Creates a new object on every use — heap allocation
> - Has its own `this` reference — refers to the anonymous class instance
> - Can implement interfaces with state (instance fields)
>
> **Lambda:**
> - Compiled using `invokedynamic` bytecode instruction (introduced in Java 7)
> - At runtime, the JVM links the lambda to an implementation via
    >   `LambdaMetafactory` — decided at first call, cached afterwards
> - May or may not allocate an object depending on whether it captures variables
    >   — a non-capturing lambda is often a singleton (zero allocation after first call)
> - `this` inside a lambda refers to the **enclosing class**, not the lambda itself
>
> Practical consequence: lambdas that don't capture variables are more memory-
> efficient than equivalent anonymous classes. This is why `list.sort(String::compareTo)`
> is preferred over `list.sort(new Comparator<String>() {...})` in hot paths.

---

**Q4. Explain the "effectively final" rule. Why does it exist?**

> A local variable captured by a lambda must be **effectively final** — never
> reassigned after the point of capture. This includes both explicit `final`
> and variables that happen to never be reassigned.
>
> ```
> String prefix = "Hello"; // effectively final — never reassigned
> Runnable r = () -> System.out.println(prefix); // OK
>
> String prefix = "Hello";
> prefix = "Hi";            // reassignment → no longer effectively final
> Runnable r = () -> System.out.println(prefix); // compile error
> ```
>
> **Why the rule exists:**
> Local variables live on the stack. Lambdas may outlive the method that
> created them — the lambda could be stored and executed later after the
> stack frame is gone. To handle this, Java captures a **copy** of the
> variable's value into the lambda. If the variable were mutable, the copy
> would go stale — the lambda would see a different value than the caller.
> Rather than risk this silent inconsistency, Java requires the variable to
> be effectively final so the copy is always valid.
>
> Instance fields and static fields don't have this restriction because they
> live on the heap — the lambda captures a reference to the object, not a
> copy of the field.

---

**Q5. How do you handle checked exceptions inside a lambda?**

> Lambdas cannot throw checked exceptions unless the functional interface
> declares them. `Function<T,R>`, `Predicate<T>`, `Consumer<T>` — none
> declare checked exceptions, so code that throws them won't compile:
>
> ```
> // Compile error — IOException is checked
> Function<String, byte[]> reader = path -> Files.readAllBytes(Paths.get(path));
> ```
>
> **Three approaches:**
>
> **Option 1 — wrap in RuntimeException (quick, loses type info):**
> ```
> Function<String, byte[]> reader = path -> {
>     try { return Files.readAllBytes(Paths.get(path)); }
>     catch (IOException e) { throw new RuntimeException(e); }
> };
> ```
>
> **Option 2 — custom functional interface that declares the exception:**
> ```
> @FunctionalInterface
> interface ThrowingFunction<T, R> {
>     R apply(T t) throws Exception;
> }
> ThrowingFunction<String, byte[]> reader = path -> Files.readAllBytes(Paths.get(path));
> ```
>
> **Option 3 — utility wrapper method (cleanest for production):**
> ```
> static <T, R> Function<T, R> wrap(ThrowingFunction<T, R> f) {
>     return t -> {
>         try { return f.apply(t); }
>         catch (Exception e) { throw new RuntimeException(e); }
>     };
> }
> // Usage
> list.stream().map(wrap(path -> Files.readAllBytes(Paths.get(path)))).collect(...)
> ```
>
> Libraries like Vavr and Lombok provide `Try` and `SneakyThrows` for this.

---

## Variable Capture & Closures

**Q6. What is a closure and how does Java's lambda implement it?**

> A closure is a function that captures variables from its enclosing scope
> and carries them along for use when the function executes — even after
> the enclosing scope has exited.
>
> Java lambdas are closures — they capture effectively final local variables
> by copying their values into the lambda instance:
>
> ```
> static Supplier<Integer> makeCounter(int start) {
>     // 'start' is captured — makeCounter's stack frame will be gone
>     // when the Supplier is later called, but start's value is preserved
>     return () -> start + 1;
> }
>
> Supplier<Integer> counter = makeCounter(10);
> System.out.println(counter.get()); // 11 — captured value still accessible
> ```
>
> Classic gotcha — capturing loop variable:
> ```
> List<Runnable> tasks = new ArrayList<>();
> for (int i = 0; i < 3; i++) {
>     final int copy = i;            // new final copy each iteration
>     tasks.add(() -> System.out.println(copy)); // captures 0, 1, 2 separately
> }
> tasks.forEach(Runnable::run); // prints 0, 1, 2
> ```
>
> Without `final int copy = i`, the loop variable `i` is not effectively
> final (it is reassigned each iteration) — compile error.

---

**Q7. Why does `this` inside a lambda refer to the enclosing class, not the lambda?**

> A lambda is not an object — it has no identity of its own. It is a behaviour
> attached to its enclosing context. `this` always refers to the enclosing
> class instance, making it consistent with regular method code:
>
> ```
> class Service {
>     String name = "UserService";
>
>     void demo() {
>         Runnable r = () -> System.out.println(this.name); // this = Service instance
>         r.run(); // prints "UserService"
>     }
> }
> ```
>
> In contrast, an anonymous inner class has its own `this`:
> ```
> class Service {
>     String name = "UserService";
>
>     void demo() {
>         Runnable r = new Runnable() {
>             String name = "AnonymousRunnable";
>             @Override public void run() {
>                 System.out.println(this.name);      // "AnonymousRunnable"
>                 System.out.println(Service.this.name); // "UserService"
>             }
>         };
>         r.run();
>     }
> }
> ```
>
> This difference matters when using lambdas inside Spring beans or servlets
> — `this` in a lambda always gives you the Spring bean, which is what you want.

---

## Design & Production

**Q8. Explain the Strategy Pattern using lambdas. How does it improve on the class-based approach?**

> Classic Strategy Pattern (pre-Java 8) — one interface, one class per strategy:
> ```
> interface SortStrategy { void sort(List<Integer> list); }
> class BubbleSort implements SortStrategy { ... }  // separate class file
> class QuickSort  implements SortStrategy { ... }  // separate class file
> ```
>
> With lambdas — strategies are just values:
> ```
> Consumer<List<Integer>> bubbleSort = list -> { /* logic */ };
> Consumer<List<Integer>> quickSort  = list -> { /* logic */ };
>
> // Inject at call site — no extra class files
> void process(List<Integer> data, Consumer<List<Integer>> strategy) {
>     strategy.accept(data);
> }
> process(numbers, bubbleSort);
> process(numbers, list -> Collections.sort(list)); // inline strategy
> ```
>
> Improvements:
> 1. No separate class file per strategy
> 2. Strategies can be composed: `strategy1.andThen(strategy2)`
> 3. Strategies can be stored in maps, passed through APIs, returned from methods
> 4. Inline strategies for one-off use — no boilerplate
>
> Production example: a validation pipeline where each field has its own
> `Predicate<String>` rule, composable with `.and()` and `.or()`.

---

**Q9. What is lazy evaluation and how do lambdas enable it?**

> Lazy evaluation means deferring computation until the result is actually needed.
> `Supplier<T>` is the primary vehicle — it wraps a computation that runs only
> when `.get()` is called:
>
> ```
> // Eager — connection opened immediately, even if never used
> Connection conn = openDatabaseConnection(); // expensive!
>
> // Lazy — connection opened only when first needed
> Supplier<Connection> lazyConn = () -> openDatabaseConnection();
> // ... much later, only if needed:
> if (needsDb) conn = lazyConn.get();
> ```
>
> Production uses:
> 1. **Conditional logging** — avoid building expensive log messages that
     >    will be discarded by the log level:
> ```
> // Eager — toString() called even if DEBUG is off
> log.debug("State: " + expensiveObject.toString());
>
> // Lazy — lambda only executed if DEBUG level is enabled
> log.debug("State: {}", () -> expensiveObject.toString());
> ```
>
> 2. **Default value computation** — `Optional.orElseGet()` vs `orElse()`:
> ```
> // orElse — always evaluates fallback
> user.orElse(createDefaultUser()); // createDefaultUser() called even if user present!
>
> // orElseGet — lazy, only called if Optional is empty
> user.orElseGet(() -> createDefaultUser());
> ```
>
> 3. **Lazy initialisation** — initialise expensive fields on first access.

---

**Q10. What is currying and partial application? Give a real java example.**

> **Currying** — transforming a multi-argument function into a chain of
> single-argument functions. Each application returns a new function waiting
> for the next argument.
>
> **Partial application** — fixing some arguments of a function, producing
> a new function with fewer arguments.
>
> ```
> // Curried add: instead of add(a, b), use add(a)(b)
> Function<Integer, Function<Integer, Integer>> add = a -> b -> a + b;
>
> // Partial application — fix first argument
> Function<Integer, Integer> add5  = add.apply(5);  // a=5 fixed
> Function<Integer, Integer> add10 = add.apply(10); // a=10 fixed
>
> add5.apply(3);   // 8
> add10.apply(3);  // 13
> ```
>
> Real production use — configurable discount calculator:
> ```
> Function<Double, Function<Double, Double>> discount =
>     rate -> price -> price * (1 - rate);
>
> Function<Double, Double> memberDiscount   = discount.apply(0.15);
> Function<Double, Double> premiumDiscount  = discount.apply(0.25);
>
> // Apply to any price
> memberDiscount.apply(1000.0);  // 850.0
> premiumDiscount.apply(1000.0); // 750.0
> ```
>
> Currying is used in functional pipelines where you build specialised
> functions from general ones — common in configuration-driven processing,
> discount engines, and rule-based systems.

---

**Q11. What is `Function.andThen()` vs `Function.compose()`? How do you remember which is which?**

> Both compose two functions but in opposite directions:
>
> `f.andThen(g)` — apply `f` first, then `g`: `g(f(x))`. Left to right.
>
> `f.compose(g)` — apply `g` first, then `f`: `f(g(x))`. Right to left.
>
> ```
> Function<Integer, Integer> times2 = x -> x * 2;
> Function<Integer, Integer> plus10 = x -> x + 10;
>
> times2.andThen(plus10).apply(3);  // times2 first: (3×2)+10 = 16
> times2.compose(plus10).apply(3);  // plus10 first: (3+10)×2 = 26
> ```
>
> **Memory trick:** `andThen` = "do this, AND THEN do that" — reads left to right
> like English. `compose` = mathematical function composition f∘g — reads right to left.
>
> In practice, `andThen` is used far more often because it reads in execution
> order. `compose` is useful when building pipelines from right to left, which
> matches mathematical notation.

---

**Q12. Can a lambda be serialised? When would you need this?**

> A lambda can be serialised if the functional interface extends `Serializable`:
> ```
> Comparator<String> comp = (Serializable & Comparator<String>)
>         (a, b) -> a.compareTo(b);
> ```
>
> Or define the functional interface as serialisable:
> ```
> interface SerializableComparator<T>
>         extends Comparator<T>, Serializable {}
> ```
>
> **When you need serialisable lambdas:**
> 1. Distributed computing frameworks (Apache Spark, Flink) — lambdas are
     >    sent over the network to worker nodes for execution
> 2. Java EE / Jakarta EE stateful session beans — state may be passivated
     >    to disk, requiring all captured fields to be serialisable
> 3. Caching frameworks where cached values include behaviour
>
> **Risks:** serialised lambdas are brittle — a code change that reorders
> lambdas in a class changes the serialised form. Lambda serialisation is
> generally avoided in favour of named strategy classes when persistence
> is required.

---

**Q13. How would you implement a pipeline of transformations using lambdas that can be built dynamically at runtime?**

> ```
> class Pipeline<T> {
>     private Function<T, T> steps = Function.identity(); // start: do nothing
>
>     Pipeline<T> addStep(Function<T, T> step) {
>         steps = steps.andThen(step);
>         return this; // fluent
>     }
>
>     T execute(T input) { return steps.apply(input); }
> }
>
> // Build pipeline dynamically based on config
> Pipeline<String> pipeline = new Pipeline<String>()
>     .addStep(String::trim)
>     .addStep(String::toLowerCase)
>     .addStep(s -> s.replace(" ", "_"));
>
> pipeline.execute("  Hello World  "); // "hello_world"
> ```
>
> `Function.identity()` returns a function that returns its input unchanged —
> the neutral element for `andThen` composition. Starting with identity means
> you can add zero or more steps and the pipeline always works correctly.
>
> Production use: request/response processing pipelines, ETL transformations,
> middleware chains — anywhere the processing steps are driven by configuration
> rather than hardcoded logic.

---

**Q14. What are the performance implications of using lambdas in tight loops?**

> For **non-capturing lambdas** (no variables from enclosing scope):
> - The JVM creates one instance and reuses it — zero allocation per call
> - Effectively a static method reference — no GC pressure
>
> For **capturing lambdas** (closed over variables):
> - A new object is allocated each time the lambda is created in a tight loop
> - This adds GC pressure in hot paths
>
> ```
> // Non-capturing — one instance reused (good)
> list.sort((a, b) -> a.compareTo(b));
>
> // Capturing — new lambda object each iteration (avoid in tight loops)
> for (int i = 0; i < 1_000_000; i++) {
>     int threshold = i;
>     list.removeIf(n -> n < threshold); // new lambda each iteration
> }
>
> // Better — extract outside loop
> for (int i = 0; i < 1_000_000; i++) {
>     final int threshold = i;
>     Predicate<Integer> pred = n -> n < threshold; // still captures, but cleaner
>     list.removeIf(pred);
> }
> ```
>
> In practice, lambda allocation cost is minor compared to the work being
> done. Profile before optimising. JIT compilation often inlines lambdas
> entirely, eliminating the allocation. The real performance concern is
> using lambdas to wrap expensive operations that run unnecessarily — which
> is solved by lazy evaluation, not by avoiding lambdas.

---

**Q15. How do lambdas interact with generics and type inference?**

> The compiler infers lambda parameter types from the target functional
> interface's generic type. This works well in simple cases:
> ```
> Function<String, Integer> f = s -> s.length(); // s inferred as String
> ```
>
> It breaks down in complex chained inference:
> ```
> // Compile error — type inference fails for chained stream operations
> var result = list.stream()
>     .map(s -> s.trim())           // inferred OK
>     .map(s -> s.split(","))       // inferred OK
>     .flatMap(Arrays::stream)
>     .sorted((a, b) -> ...)        // fails — can't infer type here
>     .collect(Collectors.toList());
>
> // Fix — add explicit type to one intermediate step
> .sorted((String a, String b) -> a.compareTo(b))
> // or break the chain
> Stream<String> intermediate = list.stream().map(...).flatMap(...);
> intermediate.sorted(...).collect(...);
> ```
>
> Also: generic methods returning lambdas require explicit type witnesses:
> ```
> // Ambiguous — compiler can't determine T
> Function<String, ?> f = identity(); // which T?
>
> // Explicit type witness
> Function<String, String> f = LambdaSamples.<String>identity();
> ```
>
> When type inference fails, add explicit parameter types to the lambda —
> the compiler error message usually points to the right place.