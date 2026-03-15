# 🎯 Interview Questions — Java 12-14

---

**Q1. What is the difference between a switch statement and a switch expression?**

> A switch **statement** executes code — it has no value, uses fall-through
> by default, and requires `break` to prevent unintended fall-through bugs:
>
> ```
> switch (day) {
>     case "MONDAY":
>         result = "start";
>         break; // forget this → falls into TUESDAY case
>     case "TUESDAY":
>         result = "continue";
>         break;
> }
> ```
>
> A switch **expression** produces a value — it can be assigned, returned,
> or passed as an argument. The arrow (`->`) syntax has no fall-through,
> multiple labels are comma-separated, and the compiler enforces exhaustiveness:
>
> ```
> String result = switch (day) {
>     case "MONDAY"  -> "start";
>     case "TUESDAY" -> "continue";
>     default        -> "other";
> };
> ```
>
> Key differences:
> - Expression returns a value, statement does not
> - Arrow syntax has no fall-through — each case is independent
> - Non-exhaustive switch expression is a compile error
> - `yield` returns a value from a block case — `break` is not used

---

**Q2. What is `yield` in switch expressions and when do you need it?**

> `yield` returns a value from a multi-statement block case in a switch
> expression. When a case needs more than one statement, you use a block
> `{ }` and `yield` to specify the return value:
>
> ```
> int result = switch (input) {
>     case "A" -> 1;           // single expression — no yield needed
>     case "B" -> {
>         log("processing B"); // multiple statements
>         yield 2;             // yield returns from block
>     }
>     default -> 0;
> };
> ```
>
> `yield` is only valid inside switch expressions — not switch statements.
> `return` inside a switch block returns from the enclosing method, not
> from the switch — a common mistake:
>
> ```
> int result = switch (x) {
>     case 1 -> { return 42; } // compile error — return exits method, not switch
>     case 1 -> { yield 42; }  // correct
> };
> ```

---

**Q3. How do text blocks handle indentation? What determines how much whitespace is stripped?**

> Text blocks strip common leading whitespace from all lines. The amount
> stripped is determined by the position of the closing `"""`:
>
> ```
> // Closing """ on its own line — strips 8 spaces (the indent of content)
> String s = """
>         hello
>         world
>         """;
> // result: "hello\nworld\n"
>
> // Closing """ further left — strips less, preserves relative indent
> String s2 = """
>         hello
>         world
>     """; // 4 spaces indent on closing """
> // result: "    hello\n    world\n"
> ```
>
> The algorithm: find the minimum indentation across all non-empty lines
> and the closing `"""` line, then strip that many characters from the
> start of every line.
>
> Practical rule: align closing `"""` with the left margin of the content
> you want fully stripped. Move it left to preserve indentation.

---

**Q4. What is pattern matching instanceof and what problem does it solve?**

> Before Java 16, every `instanceof` check required an explicit cast on
> the next line — a redundant operation the compiler already knew was safe:
>
> ```
> if (obj instanceof String) {
>     String str = (String) obj; // redundant — compiler knows it's a String
>     process(str);
> }
> ```
>
> Pattern matching binds the cast result to a variable in one step:
>
> ```
> if (obj instanceof String str) {
>     process(str); // str is bound and cast — no redundant cast
> }
> ```
>
> The binding variable `str` is only in scope where the pattern is
> guaranteed to match — inside the `if` block and in `&&` conditions:
>
> ```
> if (obj instanceof String str && str.length() > 5) { // str in scope ✓
>     ...
> }
> if (obj instanceof String str || str.isEmpty()) { // compile error — str not in scope for ||
>     ...
> }
> ```
>
> Most valuable in `equals()` implementations and polymorphic processing
> where you check type then immediately operate on the typed value.

---

**Q5. What are helpful NullPointerExceptions and why are they significant for debugging?**

> Before Java 14, NPE messages contained almost no useful information —
> just the exception type and stack trace line number. In chained method
> calls, you had no idea which part of the chain was null:
>
> ```
> user.getAddress().getCity().toUpperCase();
> // NullPointerException (which of the three was null?)
> ```
>
> Java 14 introduced descriptive NPE messages that identify exactly what
> was null and what operation failed:
>
> ```
> Cannot invoke "String.toUpperCase()" because the return value of
> "Address.getCity()" is null
> ```
>
> This is significant because:
> - Chained calls are common in Java — builders, streams, method chains
> - Previously debugging required breaking chains into separate lines
> - Production logs now pinpoint the exact null without reproducing locally
> - Saves significant debugging time for deeply nested object graphs
>
> The feature is enabled by default from Java 15. Before that it required
> the JVM flag `-XX:+ShowCodeDetailsInExceptionMessages`. The improvement
> is in the JVM itself — no code changes needed to benefit from it.