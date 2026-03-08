# ⚙️ Functional Interfaces & Method References

> Covers all built-in functional interfaces in `java.util.function`,
> the four method reference types, composition, and custom functional
> interfaces.

---

## 🧠 Mental Model

```
Functional Interface = interface with exactly ONE abstract method
Lambda               = implementation of that one method
Method Reference     = shorthand lambda that just calls an existing method

Built-in interfaces cover every combination of input/output:

  Takes something, returns something  → Function<T,R>
  Takes something, returns boolean    → Predicate<T>
  Takes something, returns nothing    → Consumer<T>
  Takes nothing,   returns something  → Supplier<T>
  Takes T,         returns T          → UnaryOperator<T>
  Takes T,T,       returns T          → BinaryOperator<T>
  Takes two args:  BiFunction / BiConsumer / BiPredicate
  No boxing:       IntPredicate / IntFunction / ToIntFunction / ...
```

---

## 📄 Classes in this Module

### `FunctionalSamples.java`

| Example           | What it covers |
|-------------------|----------------|
| Core Interfaces   | Predicate, Function, BiFunction, Consumer, BiConsumer, Supplier, UnaryOperator, BinaryOperator, primitive variants |
| Method References | Static, bound instance, unbound instance, constructor reference |
| Composition       | Predicate and/or/negate, Function andThen/compose, Function.identity, Consumer andThen |
| Adv Level         | Custom functional interface, higher-order functions, behaviour injection, memoisation |

---

## ⚡ Interface Quick Reference

```
// Predicate<T> — T → boolean
Predicate<String> isLong = s -> s.length() > 5;
isLong.test("hello world");               // true
isLong.and(other)                         // both true
isLong.or(other)                          // either true
isLong.negate()                           // flip result

// Function<T,R> — T → R
Function<String, Integer> len = String::length;
len.apply("hello");                       // 5
len.andThen(n -> n * 2)                   // compose left to right
len.compose(String::trim)                 // compose right to left
Function.identity()                       // returns input unchanged

// BiFunction<T,U,R> — T,U → R
BiFunction<String, Integer, String> rep = (s, n) -> s.repeat(n);
rep.apply("ha", 3);                       // "hahaha"

// Consumer<T> — T → void
Consumer<String> print = System.out::println;
print.accept("hello");
print.andThen(other)                      // chain consumers

// BiConsumer<T,U> — T,U → void
BiConsumer<String, Integer> log = (k, v) -> System.out.println(k + "=" + v);
map.forEach(log);

// Supplier<T> — () → T
Supplier<List<String>> newList = ArrayList::new;
newList.get();                            // fresh ArrayList each call

// UnaryOperator<T> — T → T  (specialised Function<T,T>)
UnaryOperator<String> trim = String::trim;

// BinaryOperator<T> — T,T → T  (specialised BiFunction<T,T,T>)
BinaryOperator<Integer> max = Integer::max;
```

---

## ⚡ Method Reference Types

```
// 1. Static method reference — ClassName::staticMethod
// Lambda equivalent: n -> Integer.parseInt(n)
Function<String, Integer> parse = Integer::parseInt;

// 2. Unbound instance method ref — ClassName::instanceMethod
// Lambda equivalent: s -> s.toUpperCase()
// The instance (s) is the first parameter, supplied by the caller
Function<String, String> upper = String::toUpperCase;
names.stream().map(String::toUpperCase)

// 3. Bound instance method ref — instance::instanceMethod
// Lambda equivalent: s -> System.out.println(s)
// A specific instance is already captured
Consumer<String> print = System.out::println;
names.forEach(System.out::println);

// 4. Constructor reference — ClassName::new
// Lambda equivalent: () -> new ArrayList<>()
Supplier<List<String>> newList = ArrayList::new;
// With argument:
// Lambda equivalent: s -> new StringBuilder(s)
Function<String, StringBuilder> newSB = StringBuilder::new;
```

---

## ⚡ Primitive Variants — No Boxing

```
// Use these instead of Predicate<Integer>, Function<Integer,R> etc.
// to avoid Integer boxing overhead in hot paths

IntPredicate    isEven  = n -> n % 2 == 0;    // int → boolean
IntUnaryOperator dbl    = n -> n * 2;           // int → int
IntBinaryOperator add   = Integer::sum;         // int,int → int
IntConsumer      print  = System.out::println;  // int → void
IntSupplier      rnd    = () -> 42;             // () → int
ToIntFunction<String> len = String::length;     // T → int
IntFunction<String>  str  = Integer::toString;  // int → T
```

---

## 🔑 Common Mistakes

```
// ❌ Using Function<Integer,Integer> instead of IntUnaryOperator
Function<Integer, Integer> dbl = n -> n * 2; // boxes int → Integer

// ✅ Use primitive variant in hot paths
IntUnaryOperator dbl = n -> n * 2; // no boxing

// ❌ Using method reference when lambda is clearer
list.stream().filter(FunctionalSamples::isValid) // where is isValid defined?

// ✅ Lambda is clearer for complex conditions
list.stream().filter(s -> s.length() > 3 && s.startsWith("A"))

// ❌ Forgetting @FunctionalInterface annotation
interface MyFunc { String apply(String s); } // works but no compile-time check

// ✅ Add annotation — compiler rejects if second abstract method added
@FunctionalInterface
interface MyFunc { String apply(String s); }
```

---