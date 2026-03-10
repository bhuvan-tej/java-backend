# 🎯 Interview Questions — Default & Static Methods

---

**Q1. Why were default methods introduced in Java 8?**

> The primary reason was backward compatibility in the JDK itself.
>
> The Stream API required new methods on `Collection` — `stream()`,
> `parallelStream()`, `forEach()`, `removeIf()`. Before Java 8, adding
> any method to an interface was a breaking change — every implementing
> class had to add it. There are thousands of `Collection` implementations
> across the Java ecosystem. Making them all break was not an option.
>
> Default methods solved this: new methods could be added to interfaces
> with a default implementation. Existing implementations inherit the default
> and keep working. Those that want different behavior override it:
>
> ```
> // Added to Collection in Java 8 — zero existing code broke
> default Stream<E> stream() {
>     return StreamSupport.stream(spliterator(), false);
> }
> ```
>
> The secondary benefit — new design patterns like mixins — was a consequence,
> not the original motivation.

---

**Q2. What is the difference between a `default` method and a `static` method in an interface?**

> `default` method — instance method with a body. Inherited by implementing
> classes. Can be overridden. Can call other instance methods (abstract or default)
> via `this`:
> ```
> interface Greeter {
>     String greet(String name);                        // abstract
>     default String greetLoudly(String name) {
>         return greet(name).toUpperCase();             // calls abstract method
>     }
> }
> ```
>
> `static` method — belongs to the interface itself. NOT inherited by
> implementing classes. Called via the interface name, not an instance:
> ```
> interface Validator {
>     static Validator of(Predicate<String> p, String msg) { ... } // factory
> }
>
> Validator.of(s -> s.contains("@"), "invalid email"); // ✅
> // myValidator.of(...) would not compile — static not inherited
> ```
>
> Use `default` for inheritable behavior. Use `static` for utility or factory
> methods that logically belong to the interface but don't need an instance.

---

**Q3. What is the diamond problem with default methods? How do you resolve it?**

> When a class implements two interfaces that both define a `default` method
> with the same signature, the compiler cannot decide which to use — it
> forces you to resolve the ambiguity explicitly:
>
> ```
> interface A { default String hello() { return "A"; } }
> interface B { default String hello() { return "B"; } }
>
> // Compile error without override — ambiguous default
> class C implements A, B {
>     @Override
>     public String hello() {
>         return A.super.hello(); // ← InterfaceName.super.method() syntax
>     }
> }
> ```
>
> `InterfaceName.super.method()` is the only way to explicitly invoke
> a specific interface's default method from within a class. You can also
> combine them:
> ```
> return A.super.hello() + " + " + B.super.hello();
> ```
>
> If only one interface provides the default and the other doesn't, there
> is no conflict — the single default is inherited without any override needed.

---

**Q4. What are the priority rules when a method exists in both a class and an interface?**

> Three rules in order of priority:
>
> 1. **Class always wins over interface default** — a method in a class
     >    (concrete or abstract) takes precedence over any interface default:
> ```
> interface Logged { default String log() { return "interface"; } }
> abstract class Base implements Logged {
>     public String log() { return "class"; } // wins
> }
> class Child extends Base { } // inherits "class" version
> ```
>
> 2. **Most specific interface wins** — a default in a child interface
     >    overrides the same default in a parent interface:
> ```
> interface Animal  { default String sound() { return "..."; } }
> interface Dog extends Animal { default String sound() { return "woof"; } } // wins
> class Labrador implements Dog { } // gets "woof"
> ```
>
> 3. **Explicit override required** when two unrelated interfaces conflict —
     >    as shown in the diamond problem above.
>
> The simple mental model: **class > specific interface > general interface**.

---

**Q5. Can default methods have state? Can they access fields?**

> No. Interfaces cannot have instance fields — any field declared in an
> interface is implicitly `public static final` (a constant). Default methods
> cannot store or access per-instance state directly:
>
> ```
> interface Counter {
>     int count = 0; // public static final — shared constant, not state
>     default void increment() { count++; } // compile error — final field
> }
> ```
>
> The pattern to work around this: define an abstract method that exposes
> state, and have default methods operate through it. The implementing class
> holds the actual state:
>
> ```
> interface Auditable {
>     Map<String, Object> getMetadata(); // abstract — implementor provides storage
>
>     default void markCreated() {
>         getMetadata().put("createdAt", Instant.now()); // operates through hook
>     }
> }
>
> class Order implements Auditable {
>     private final Map<String, Object> metadata = new HashMap<>(); // actual state
>     public Map<String, Object> getMetadata() { return metadata; }
> }
> ```
>
> This "hook method" pattern is how all meaningful default methods operate —
> they define behaviour in terms of abstract methods that subclasses provide.

---

**Q6. What is the mixin pattern and how do default methods enable it?**

> A mixin is a set of methods that can be "mixed into" a class to add
> capability — without requiring the class to extend a specific parent.
>
> Before Java 8, interfaces had no behaviour — you could not mix in methods.
> Default methods change this. A class can now pick up concrete behaviour
> from multiple interfaces:
>
> ```
> interface Auditable {
>     Map<String, Object> meta();
>     default void markCreated() { meta().put("createdAt", Instant.now()); }
>     default void markUpdated() { meta().put("updatedAt", Instant.now()); }
> }
>
> interface Exportable {
>     default String toJson() { return "{ ... }"; }
>     default String toCsv()  { return "...,"; }
> }
>
> // Order gains both capabilities — no inheritance hierarchy needed
> class Order implements Auditable, Exportable {
>     private final Map<String, Object> meta = new HashMap<>();
>     public Map<String, Object> meta() { return meta; }
> }
> ```
>
> The advantage over inheritance: `Order` does not have to extend
> `AuditableBase` or `ExportableBase` — Java's single inheritance limit
> does not constrain it. Multiple interfaces can be composed freely.

---

**Q7. Name five default methods added to existing JDK interfaces in Java 8.**

> ```
> // Collection
> collection.forEach(System.out::println);       // Iterable.forEach
> collection.removeIf(s -> s.isEmpty());         // Collection.removeIf
> collection.stream()                            // Collection.stream
> collection.spliterator()                       // Collection.spliterator
>
> // List
> list.sort(Comparator.naturalOrder());          // List.sort
> list.replaceAll(String::toUpperCase);          // List.replaceAll
>
> // Map
> map.forEach((k, v) -> log(k, v));             // Map.forEach
> map.getOrDefault("key", "default");           // Map.getOrDefault
> map.putIfAbsent("key", value);                // Map.putIfAbsent
> map.merge(key, 1, Integer::sum);              // Map.merge
> map.computeIfAbsent(key, k -> new List());    // Map.computeIfAbsent
> map.replaceAll((k, v) -> v.toUpperCase());    // Map.replaceAll
> ```
>
> All of these were added to existing interfaces without breaking any
> code — the defining demonstration of why default methods exist.

---

**Q8. When should you use a default method vs an abstract class?**

> Use a **default method** when:
> - Adding optional behaviour to an interface without forcing implementors to change
> - Creating mixins — adding a capability that any class can opt into
> - The behaviour is a natural extension of the interface's contract
> - Multiple inheritance of behaviour is needed
>
> Use an **abstract class** when:
> - You need instance fields and state
> - You need constructors with initialisation logic
> - The behaviour is tightly coupled to shared state
> - You want to enforce a specific class hierarchy
>
> ```
> // Default method — right choice: stateless behaviour added to interface
> interface Printable {
>     String content();
>     default void print() { System.out.println(content()); }
> }
>
> // Abstract class — right choice: shared state + template method
> abstract class Report {
>     private final String title; // state — needs abstract class
>     Report(String title) { this.title = title; }
>     abstract String body();
>     final void generate() { System.out.println(title + "\n" + body()); }
> }
> ```
>
> In practice: prefer interfaces with default methods for capability mixins,
> use abstract classes when shared state or constructors are needed.