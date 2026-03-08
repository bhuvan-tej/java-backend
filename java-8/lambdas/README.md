# λ Lambdas

> Covers Java 8 lambda expressions — the foundation of functional
> programming in Java. Understand the syntax, variable capture rules,
> functional interfaces, and how lambdas replaced anonymous inner classes.

---

## 🧠 Mental Model

```
Before Java 8 — Anonymous Inner Class
──────────────────────────────────────────────────
Collections.sort(names, new Comparator<String>() {
    @Override
    public int compare(String a, String b) {
        return a.compareTo(b);          // 6 lines for 1 line of logic
    }
});

Java 8 — Lambda
──────────────────────────────────────────────────
names.sort((a, b) -> a.compareTo(b)); // same thing, 1 line

Java 8 — Method Reference (when lambda just calls a method)
──────────────────────────────────────────────────
names.sort(String::compareTo);         // even cleaner


A Lambda IS the implementation of a Functional Interface's one method.

  Functional Interface        Lambda fills the one method
  ─────────────────────       ────────────────────────────
  Comparator<T>               (a, b) -> a.compareTo(b)
  Runnable                    () -> System.out.println("hi")
  Predicate<T>                n -> n % 2 == 0
  Function<T,R>               s -> s.length()
  Consumer<T>                 s -> System.out.println(s)
  Supplier<T>                 () -> new ArrayList<>()
```

---

## 📄 Classes in this Module

### `LambdaSamples.java`

| Example          | What it covers |
|------------------|----------------|
| Syntax Evolution | Anonymous class → Lambda (typed) → Lambda (inferred) → Method reference |
| Passing Lambdas  | Predicate compose (and/or/negate), Function andThen, Consumer andThen, Supplier |
| Variable Capture | Effectively final rule, array workaround, closure in loops |
| Adv Level        | Strategy pattern, lazy evaluation, Function composition (andThen/compose), currying |

---

## ⚡ Lambda Syntax

```
// Zero parameters
Runnable r = () -> System.out.println("hello");

// One parameter — parentheses optional
Predicate<String> empty = s -> s.isEmpty();
Predicate<String> empty = (s) -> s.isEmpty();      // also valid

// One parameter — explicit type
Predicate<String> empty = (String s) -> s.isEmpty();

// Multiple parameters
Comparator<String> comp = (a, b) -> a.compareTo(b);

// Block body — braces required, explicit return
Function<Integer, Integer> squared = (n) -> {
    int result = n * n;
    return result;              // return required in block body
};

// Single expression — no braces, no return
Function<Integer, Integer> squared = n -> n * n;
```

---

## ⚡ Variable Capture Rules

```
// ✅ Effectively final — never reassigned after capture point
String prefix = "Hello";
Runnable r = () -> System.out.println(prefix); // OK

// ❌ Not effectively final — reassigned after creation
String prefix = "Hello";
prefix = "Hi";                 // reassignment → not effectively final
Runnable r = () -> System.out.println(prefix); // compile error!

// ✅ Instance fields — always capturable (no final requirement)
class MyClass {
    String name = "Java";
    Runnable r = () -> System.out.println(name); // OK
}

// ✅ Workaround for mutable counter in lambda
int[] count = {0};             // array reference is final, element is mutable
list.forEach(item -> count[0]++);

// ✅ this in instance lambda — refers to enclosing class (not lambda itself)
class Foo {
    String msg = "hello";
    void demo() {
        Runnable r = () -> System.out.println(this.msg); // this = Foo instance
    }
}
```

---

## ⚡ Function Composition

```
Function<Integer, Integer> times2 = x -> x * 2;
Function<Integer, Integer> plus10 = x -> x + 10;

// andThen — left to right
times2.andThen(plus10).apply(3);   // (3×2)+10 = 16

// compose — right to left
times2.compose(plus10).apply(3);   // (3+10)×2 = 26

// Chain as pipeline
Function<Integer, Integer> pipeline =
    times2.andThen(plus10).andThen(x -> x * x);
pipeline.apply(3);                 // ((3×2)+10)² = 256

// Predicate composition
Predicate<Integer> isEven = n -> n % 2 == 0;
Predicate<Integer> isGt5  = n -> n > 5;
isEven.and(isGt5)                  // both must be true
isEven.or(isGt5)                   // either must be true
isEven.negate()                    // flip the condition
```

---

## ⚡ Common Patterns

```
// ── Strategy pattern ─────────────────────────────────────────
class Validator<T> {
    private final Predicate<T> rule;
    Validator(Predicate<T> rule) { this.rule = rule; }
    boolean validate(T v) { return rule.test(v); }
}
Validator<String> emailCheck = new Validator<>(
    s -> s.contains("@") && s.length() > 5);

// ── Lazy evaluation ───────────────────────────────────────────
Supplier<Connection> lazyConn = () -> openExpensiveConnection();
// Connection not opened until lazyConn.get() is called

// ── Currying (partial application) ───────────────────────────
Function<Double, Function<Double, Double>> discount =
    rate -> price -> price * (1 - rate);
Function<Double, Double> tenPercOff = discount.apply(0.10);
tenPercOff.apply(500.0);           // 450.0

// ── Behaviour injection ───────────────────────────────────────
void process(List<String> items, Consumer<String> action) {
    items.forEach(action);
}
process(names, System.out::println);
process(names, s -> log.info(s));
```

---