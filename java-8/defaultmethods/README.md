# 🔧 Default & Static Methods in Interfaces

> Java 8 allows interfaces to carry concrete behavior via `default` and
> `static` methods — without breaking existing implementations.

---

## 🧠 Mental Model

```
BEFORE Java 8
  interface Shape { double area(); }   ← abstract only
  Adding a new method = breaking change — every implementor must update

AFTER Java 8
  interface Shape {
      double area();                          ← abstract — must implement
      default String describe() { ... }      ← concrete — optional override
      static Shape circle(double r) { ... }  ← utility — on the interface itself
  }

WHY IT EXISTS
  Primary reason: backward compatibility in the JDK
  Collection.stream(), List.sort(), Map.forEach(),
  Map.getOrDefault() — all added as default methods in Java 8
  without breaking a single existing Collection implementation
```

---

## 📄 Classes in this Module

### `DefaultMethodSamples.java`

| Example | What it covers |
|---------|----------------|
| Basic Default Method | Defining, inheriting, calling from implementor |
| Static Methods | Interface-level utilities, static factory pattern |
| Diamond Problem | Two interfaces, same default method, resolution with `Interface.super` |
| Override Default | Class overrides default, abstract class always wins |
| Real World | Mixin pattern, JDK examples: `List.sort`, `Map.forEach`, `Map.getOrDefault` |

---

## ⚡ Syntax

```
interface Payment {
    // Abstract — implementor must provide
    void process(double amount);

    // Default — concrete, inheritable, overridable
    default void validate(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Invalid amount");
    }

    // Static — utility on the interface, NOT inherited
    static Payment noOp() {
        return amount -> System.out.println("no-op: " + amount);
    }
}

class CreditCardPayment implements Payment {
    public void process(double amount) {
        validate(amount); // call default method
        System.out.println("charging card: " + amount);
    }
    // validate() inherited — no need to override
}
```

---

## ⚡ Diamond Problem Resolution

```
interface A { default String hello() { return "A"; } }
interface B { default String hello() { return "B"; } }

// Compile error if hello() not overridden — ambiguous default
class C implements A, B {
    @Override
    public String hello() {
        return A.super.hello(); // explicitly choose A's version
        // or combine: A.super.hello() + B.super.hello()
    }
}
```

---

## ⚡ Priority Rules

```
When multiple sources provide the same method:

  1. Class method            — always wins (concrete or abstract)
  2. Most specific interface — child interface wins over parent interface
  3. Explicit override       — required when two interfaces conflict

abstract class > interface default — always
```

```
interface Logged      { default String log() { return "interface"; } }
abstract class Base implements Logged {
    public String log() { return "abstract class"; } // wins
}
class Child extends Base implements Logged {
    // Base.log() wins — no override needed
}
```

---

## ⚡ Mixin Pattern

```
// Small focused interfaces — each adds one capability
interface Auditable {
    Map<String, Object> getMetadata(); // one hook method
    default void markCreated() { getMetadata().put("createdAt", Instant.now()); }
    default void markUpdated() { getMetadata().put("updatedAt", Instant.now()); }
}

interface Exportable {
    default String toJson() { ... }
    default String toCsv()  { ... }
}

// Compose behaviours — no inheritance hierarchy needed
class Order implements Auditable, Exportable {
    private final Map<String, Object> metadata = new HashMap<>();
    public Map<String, Object> getMetadata() { return metadata; }
}
```

---

## 🔑 Common Mistakes

```
// ❌ Overusing default methods — interfaces should define contracts, not behaviour
interface UserService {
    default User findById(String id) { /* business logic here */ } // wrong
}
// ✅ Default methods for optional extension points, not core logic

// ❌ Forgetting static methods are NOT inherited
interface MathOps { static int square(int n) { return n * n; } }
class Calc implements MathOps { }
Calc.square(5);    // compile error — static not inherited
MathOps.square(5); // ✅ call on the interface itself

// ❌ State in default methods — interfaces have no fields
interface Stateful {
    int count = 0; // implicitly public static final — shared constant, not state
    default void increment() { count++; } // compile error
}
// ✅ Default methods can only operate on what abstract methods expose
```

---