# ⚖️ Comparable vs Comparator

> Covers the two ways to define ordering in Java.
> `Comparable` bakes ordering into the class itself.
> `Comparator` defines ordering from outside — multiple strategies, no class ownership needed.

---

## 🧠 Mental Model

```
Comparable                             Comparator
──────────────────────────────         ──────────────────────────────────
class Employee                         Comparator<Employee> bySalary =
  implements Comparable<Employee>        Comparator.comparingInt(Employee::getSalary);
{
  public int compareTo(Employee o) {   Comparator<Employee> byName =
    return Integer.compare(              Comparator.comparing(Employee::getName);
      this.id, o.id);
  }                                    // Chain them
}                                      Comparator<Employee> master =
                                         Comparator.comparing(Employee::getDept)
                                                   .thenComparingInt(Employee::getSalary)
                                                   .thenComparing(Employee::getName);

One ordering. Baked in.                Multiple orderings. External. Flexible.
Part of the class contract.  

Used by:                               Used by:
  Collections.sort(list)                 list.sort(comparator)
  new TreeSet<>()                        new TreeSet<>(comparator)
  new PriorityQueue<>()                  new PriorityQueue<>(comparator)
  list.sort(null)                        stream.sorted(comparator)
```

---

## 📄 Classes in this Module

### `ComparableVsComparatorSamples.java`

| Example | What it covers |
|---------|----------------|
| Comparable Demo | Employee natural order by id — Collections.sort, TreeSet, PriorityQueue |
| Comparator Demo | Multiple sort strategies — salary asc/desc, name alpha, TreeSet with Comparator |
| Senior Level | 4-level Comparator chain, null-safe sort, strategy map, stream sorted() |

---

## 🔑 `compareTo()` and `compare()` Return Convention

```
Returns negative  →  first  < second  →  first comes BEFORE second
Returns zero      →  first == second  →  same position
Returns positive  →  first  > second  →  first comes AFTER second

Memory trick: think of it as  this - other
  negative = this is smaller = this goes first
  positive = this is bigger  = this goes later
```

---

## ⚡ Key Methods

```
// ── Comparable ──
class Employee implements Comparable<Employee> {
    @Override
    public int compareTo(Employee other) {
        return Integer.compare(this.id, other.id); // NEVER this.id - other.id
    }
}

// ── Comparator basics ──
Comparator.comparingInt(Employee::getSalary)           // int field
Comparator.comparingDouble(Product::getRating)         // double field
Comparator.comparingLong(Event::getTimestamp)          // long field
Comparator.comparing(Employee::getName)                // Comparable field

// ── Modifiers ──
comparator.reversed()                                  // flip order
Comparator.nullsFirst(comparator)                      // nulls sort before all
Comparator.nullsLast(comparator)                       // nulls sort after all

// ── Chaining ──
Comparator.comparing(Employee::getDept)                // primary
          .thenComparingInt(Employee::getSalary)       // secondary
          .thenComparing(Employee::getName)            // tertiary

// ── Natural / reverse natural ──
Comparator.naturalOrder()                              // uses compareTo()
Comparator.reverseOrder()                              // reverse of compareTo()
```

---

## ⚡ Comparable vs Comparator — When to Choose

| | Comparable | Comparator |
|---|---|---|
| Where defined | Inside the class | Outside the class |
| Number of orderings | One (natural) | Unlimited |
| Own the class? | Required | Not required |
| Used by default in | TreeSet, TreeMap, PriorityQueue, Collections.sort | list.sort(), stream.sorted(), TreeSet(comp) |
| Consistency with equals | Strongly recommended | Optional |
| Null handling | Manual in compareTo | nullsFirst / nullsLast |

---

## 🔑 Common Mistakes

```
// ❌ WRONG — subtraction overflows for large negatives
public int compareTo(Employee other) {
    return this.id - other.id;  // overflow: MIN_VALUE - 1 = positive!
}

// ✅ CORRECT — Integer.compare handles all cases safely
public int compareTo(Employee other) {
    return Integer.compare(this.id, other.id);
}

// ❌ WRONG — TreeSet drops elements where compareTo returns 0
// even if equals() says they are different
TreeSet<Product> set = new TreeSet<>(
    Comparator.comparingDouble(Product::getPrice)); // price only
set.add(new Product("Phone",  45_000));
set.add(new Product("Tablet", 45_000)); // same price → compareTo=0 → dropped!

// ✅ CORRECT — add tiebreaker so compareTo=0 only for truly equal objects
TreeSet<Product> set = new TreeSet<>(
    Comparator.comparingDouble(Product::getPrice)
              .thenComparing(Product::getName));

// ❌ WRONG — forgetting nullsFirst/nullsLast causes NPE
list.sort(Comparator.comparing(Employee::getDept)); // NPE if any dept is null

// ✅ CORRECT — handle nulls explicitly
list.sort(Comparator.comparing(Employee::getDept,
          Comparator.nullsLast(Comparator.naturalOrder())));
// or
list.sort(Comparator.nullsLast(Comparator.comparing(Employee::getDept)));
```

---