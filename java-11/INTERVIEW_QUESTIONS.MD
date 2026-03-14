# 🎯 Interview Questions — Java 11

---

**Q1. What is the difference between `strip()` and `trim()` in Java 11?**

> `trim()` — removes characters with ASCII value ≤ 32. This covers
> standard whitespace like space, tab, newline but misses Unicode
> whitespace characters like `\u2000` (en quad), `\u3000` (ideographic space):
>
> ```
> "\u2000hello\u2000".trim()  // "\u2000hello\u2000" — not stripped
> ```
>
> `strip()` — uses `Character.isWhitespace()` which understands the full
> Unicode whitespace definition:
> ```
> "\u2000hello\u2000".strip() // "hello" — correctly stripped
> ```
>
> `stripLeading()` and `stripTrailing()` strip only one side.
>
> Rule: always use `strip()` over `trim()` in new code. `trim()` exists
> for backwards compatibility. In any application handling user input,
> international text, or content from external systems, `trim()` can
> silently leave whitespace that causes comparison or parsing bugs.

---

**Q2. What does `String.lines()` return and how is it different from `split("\\n")`?**

> `lines()` returns a `Stream<String>` by splitting on `\n`, `\r`, and `\r\n`.
> It handles all three line ending styles automatically:
>
> ```
> "a\nb\r\nc\rd".lines().collect(toList()); // [a, b, c, d]
> ```
>
> `split("\\n")` — only splits on `\n`. Windows line endings (`\r\n`) leave
> a trailing `\r` in each element:
> ```
> "a\r\nb".split("\\n") // ["a\r", "b"] — \r left in first element!
> ```
>
> Additional differences:
> - `lines()` returns a lazy `Stream` — integrates naturally with stream pipelines
> - `lines()` does not include a trailing empty string for a trailing newline
> - `split()` returns an array — requires conversion for stream operations
>
> Use `lines()` when processing multi-line text: config parsing, log analysis,
> CSV headers, any text block processing.

---

**Q3. How does the Java 11 `HttpClient` differ from `HttpURLConnection`?**

> `HttpURLConnection` — the pre-Java 11 standard. Verbose, stateful, not
> thread-safe, no HTTP/2, no async support. Every aspect requires manual setup:
>
> ```
> // HttpURLConnection — verbose
> URL url = new URL("https://api.example.com");
> HttpURLConnection conn = (HttpURLConnection) url.openConnection();
> conn.setRequestMethod("GET");
> conn.setRequestProperty("Accept", "application/json");
> BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
> // ... read line by line
> ```
>
> `HttpClient` (Java 11) — fluent, immutable, thread-safe, HTTP/2, WebSocket,
> built-in async via `CompletableFuture`:
>
> ```
> // HttpClient — clean
> HttpClient client = HttpClient.newHttpClient();
> HttpRequest request = HttpRequest.newBuilder()
>     .uri(URI.create("https://api.example.com"))
>     .build();
> HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
> ```
>
> Key production points:
> - Create `HttpClient` once and reuse — it manages a connection pool internally
> - `sendAsync()` returns `CompletableFuture` — non-blocking pipeline
> - Does NOT throw on 4xx/5xx — always check `response.statusCode()`
> - Supports HTTP/2 by default with fallback to HTTP/1.1

---

**Q4. What is `Predicate.not()` and why was it needed?**

> Java does not allow the `!` operator on method references — you cannot
> write `!String::isBlank`. Before Java 11, negating a method reference
> required wrapping it in a lambda:
>
> ```
> // Java 8/9/10 — forced to use lambda
> stream.filter(s -> !s.isBlank())
> stream.filter(s -> !Objects.isNull(s))
> ```
>
> `Predicate.not(predicate)` wraps any predicate (including method references)
> with negation:
>
> ```
> // Java 11 — clean method reference negation
> stream.filter(Predicate.not(String::isBlank))
> stream.filter(Predicate.not(Objects::isNull))
> ```
>
> It is a static factory method: `Predicate.not(p)` returns a `Predicate<T>`
> that is the logical negation of `p`. The primary benefit is readability
> in stream pipelines — keeping method references consistent rather than
> mixing references and lambdas.

---

**Q5. What is `var` in lambda parameters (Java 11) and what practical problem does it solve?**

> Java 10 added `var` for local variables. Java 11 extended it to lambda
> parameters. Without annotations the two are equivalent:
>
> ```
> (s) -> s.toUpperCase()        // Java 8+
> (var s) -> s.toUpperCase()    // Java 11 — same thing
> ```
>
> The practical purpose is **annotations on lambda parameters**. Without
> `var`, lambda parameters cannot be annotated:
>
> ```
> // Cannot annotate without var
> (@NonNull String s) -> s.toUpperCase()  // compile error
>
> // var enables annotation
> (@NonNull var s) -> s.toUpperCase()     // Java 11 ✓
> ```
>
> This matters for static analysis tools — Checkstyle, SpotBugs, Lombok,
> and null-safety frameworks that use `@NonNull`, `@NotNull`, `@Nullable`
> to enforce contracts at the parameter level.
>
> Rules: all lambda parameters must consistently use `var` or none —
> `(var x, y)` is a compile error. And `var` in lambda params cannot
> infer generic type bounds — if inference fails, use explicit types.