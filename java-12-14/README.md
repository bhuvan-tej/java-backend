# ☕ Java 12-14 Features

> Java 12 (March 2019), Java 13 (September 2019), Java 14 (March 2020) — all non-LTS.
> Key theme: switch expressions, text blocks, pattern matching — most as previews,
> stabilizing in later versions.

---

## 🧠 What Changed

- **Switch expressions** — arrow syntax, no fall-through, returns value, `yield` (stable Java 14)
- **Text blocks** — multiline strings with `"""`, indentation stripping (stable Java 15)
- **String additions** — `indent()`, `transform()`, `formatted()`
- **Pattern matching instanceof** — bind variable inline (preview Java 14, stable Java 16)
- **Helpful NullPointerExceptions** — precise null identification in NPE messages (Java 14)

---

## 📄 Classes in this Module

### `Java12to14Features.java`

| Example | What it covers |
|---------|----------------|
| Switch Expressions | Arrow syntax, yield, exhaustiveness, enum switch |
| Text Blocks | Multiline strings, indentation, `\s`, line continuation |
| String Additions | indent, transform, formatted |
| Pattern Matching instanceof | Bind variable, scope, in equals(), chained patterns |
| Helpful NPEs | Precise null messages, chained calls, method chains |

---

## ⚡ Switch Expressions

```
// Old switch — fall-through, verbose
switch (day) {
    case "MONDAY":
    case "FRIDAY":
        result = "special";
        break;           // forget → fall-through bug
    default:
        result = "normal";
}

// New switch expression — arrow syntax, no fall-through, returns value
String result = switch (day) {
    case "MONDAY", "FRIDAY" -> "special";  // multiple labels, comma-separated
    default                 -> "normal";
};

// With block and yield — when you need multiple statements
int value = switch (input) {
    case "A" -> 1;
    case "B" -> {
        System.out.println("processing B");
        yield 2;           // yield returns from block
    }
    default -> 0;
};

// Exhaustive enum switch — no default needed if all values covered
String desc = switch (season) {
    case SPRING -> "warm";
    case SUMMER -> "hot";
    case AUTUMN -> "cool";
    case WINTER -> "cold";
    // compiler error if any Season value is missing
};
```

**Evolution:** Java 12 preview → Java 13 preview + `yield` → Java 14 stable

---

## ⚡ Text Blocks

```
// JSON
String json = """
        {
          "name": "Alice",
          "role": "developer"
        }
        """;

// SQL
String sql = """
        SELECT name, email
        FROM   users
        WHERE  active = true
        """;

// HTML
String html = """
        <html>
            <body><h1>Hello</h1></body>
        </html>
        """;
```

**Indentation rules:**
- Common leading whitespace stripped based on closing `"""` position
- Closing `"""` on its own line → trailing newline included
- Closing `"""` on same line as last content → no trailing newline

**Escape sequences:**
```
\s   // trailing space — prevents IDE from stripping it
\    // line continuation — no newline inserted at that point
```

**Evolution:** Java 13 preview → Java 14 preview + `\s` and `\` → Java 15 stable

---

## ⚡ Pattern Matching instanceof

```
// Old — redundant cast after instanceof
if (obj instanceof String) {
    String str = (String) obj; // cast required
    str.toUpperCase();
}

// Java 14 — pattern binding, one step
if (obj instanceof String str) {
    str.toUpperCase(); // str already bound and cast
}

// With guard condition
if (obj instanceof String str && str.length() > 5) {
    // str in scope — both checks guaranteed
}

// In equals() — clean implementation
@Override
public boolean equals(Object o) {
    return o instanceof Point p
        && x == p.x
        && y == p.y;
}
```

**Evolution:** Java 14 preview → Java 15 preview → Java 16 stable

---

## ⚡ String Additions

```
// indent — add/remove leading spaces, normalise line endings
"hello\nworld".indent(4);   // adds 4 spaces to each line
indented.indent(-4);        // removes 4 spaces from each line

// transform — apply function inline, enables fluent chaining
String result = "  hello  "
    .transform(String::strip)
    .transform(s -> s + "!");

// formatted — instance version of String.format (Java 15)
"Hello %s, you have %d messages".formatted("Alice", 5);
// same as String.format("Hello %s...", "Alice", 5)
```

---

## ⚡ Helpful NullPointerExceptions

```
// Before Java 14 — useless message
// NullPointerException (no detail)

// Java 14+ — precise message
String name = null;
name.length();
// NullPointerException: Cannot invoke "String.length()"
//   because "name" is null

// Chained access — most valuable
user.address.city
// NullPointerException: Cannot read field "city"
//   because "user.address" is null

// Method chain
str.strip().toUpperCase()
// NullPointerException: Cannot invoke "String.strip()"
//   because "str" is null
```

Enabled by default from Java 15+. Previously required `-XX:+ShowCodeDetailsInExceptionMessages`.

---

## 🔑 Common Mistakes

```
// ❌ Missing default in non-exhaustive switch expression
String result = switch (str) {
    case "A" -> "first";
    // compile error — non-enum switch must have default
};
// ✅ Add default
String result = switch (str) {
    case "A" -> "first";
    default  -> "other";
};

// ❌ Using return instead of yield in switch block
int val = switch (x) {
    case 1 -> {
        return 42; // compile error — use yield, not return
    }
};
// ✅ Use yield
int val = switch (x) {
    case 1 -> { yield 42; }
};

// ❌ Opening """ on same line as content
String s = """hello
        world"""; // compile error — must have newline after opening """

// ❌ Using pattern variable outside its scope
if (!(obj instanceof String str)) {
    str.length(); // compile error — str not in scope here
}
```

---