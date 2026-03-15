package com.javabackend.java15to16;

/**
 *
 * Java 15-16 — Sealed Classes
 *
 * Java 15: Sealed classes preview
 * Java 16: Sealed classes second preview
 * Java 17: Sealed classes stable ✅
 *
 * A sealed class restricts which classes can extend it.
 * Enables exhaustive pattern matching — compiler knows
 * all possible subtypes.
 *
 */
public class SealedClasses {

    public static void main(String[] args) {
        System.out.println("━━━ EXAMPLE 1 — Sealed Class Basics ━━━\n");
        sealedBasics();

        System.out.println("\n━━━ EXAMPLE 2 — Sealed Interfaces ━━━\n");
        sealedInterfaces();

        System.out.println("\n━━━ EXAMPLE 3 — With Pattern Matching ━━━\n");
        withPatternMatching();

        System.out.println("\n━━━ EXAMPLE 4 — Real World — Result Type ━━━\n");
        resultType();

        System.out.println("\n━━━ EXAMPLE 5 — Real World — Payment ━━━\n");
        paymentDomain();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — Sealed Class Basics
    // ─────────────────────────────────────────────

    // sealed — restricts who can extend
    // permits — lists the allowed subtypes
    sealed abstract class Vehicle permits Car, Truck, Motorcycle {
        abstract String type();
        abstract int seats();
    }

    // Permitted subclasses must be one of:
    // final      — cannot be extended further
    // sealed     — can be extended but with its own permits list
    // non-sealed — open for extension by anyone

    final class Car extends Vehicle {
        private final int doors;
        Car(int doors) { this.doors = doors; }
        @Override public String type()  { return "Car"; }
        @Override public int seats()    { return doors <= 2 ? 2 : 5; }
    }

    final class Truck extends Vehicle {
        private final double payload; // tonnes
        Truck(double payload) { this.payload = payload; }
        @Override public String type()  { return "Truck"; }
        @Override public int seats()    { return 2; }
        public double payload()         { return payload; }
    }

    // non-sealed — open for anyone to extend
    non-sealed class Motorcycle extends Vehicle {
        @Override public String type()  { return "Motorcycle"; }
        @Override public int seats()    { return 2; }
    }

    // Anyone can extend Motorcycle since it's non-sealed
    class Scooter extends Motorcycle {
        @Override public String type() { return "Scooter"; }
    }

    static void sealedBasics() {
        // Sealed classes are best demonstrated with their hierarchy
        System.out.println("  sealed Vehicle permits: Car, Truck, Motorcycle");
        System.out.println("  Car        — final (no further extension)");
        System.out.println("  Truck      — final (no further extension)");
        System.out.println("  Motorcycle — non-sealed (anyone can extend)");
        System.out.println("  Scooter    — extends Motorcycle (allowed via non-sealed)");

        // ── Why sealed? ──
        // Before: any class could extend Vehicle — unpredictable hierarchy
        // After:  compiler knows ALL possible subtypes — enables exhaustive checks

        // ── Rules ──
        // 1. Permitted subclass must be in same package (or module)
        // 2. Permitted subclass must directly extend the sealed class
        // 3. Permitted subclass must be final, sealed, or non-sealed
        System.out.println("  rules: same package · direct extend · final/sealed/non-sealed");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — Sealed Interfaces
    // ─────────────────────────────────────────────

    // Sealed interfaces work the same way
    sealed interface Expr permits Num, Add, Mul, Neg {
        int eval();
    }

    record Num(int value)          implements Expr { public int eval() { return value; } }
    record Add(Expr left, Expr right) implements Expr { public int eval() { return left.eval() + right.eval(); } }
    record Mul(Expr left, Expr right) implements Expr { public int eval() { return left.eval() * right.eval(); } }
    record Neg(Expr expr)          implements Expr { public int eval() { return -expr.eval(); } }

    static void sealedInterfaces() {
        // Build: (2 + 3) * -(4)
        Expr expr = new Mul(
                new Add(new Num(2), new Num(3)),
                new Neg(new Num(4))
        );

        System.out.println("  (2+3) * -(4) = " + expr.eval()); // -20

        // Sealed interface + records = algebraic data types
        // Common in: expression trees, AST, state machines, error types
        System.out.println("  sealed interface + records = algebraic data types ✓");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — With Pattern Matching
    // ─────────────────────────────────────────────

    sealed interface Notification permits EmailNotification, SmsNotification, PushNotification {}
    record EmailNotification(String to, String subject, String body) implements Notification {}
    record SmsNotification(String phone, String message)             implements Notification {}
    record PushNotification(String deviceId, String title)           implements Notification {}

    static void withPatternMatching() {
        // Sealed + pattern matching = exhaustive dispatch
        // Compiler knows all subtypes — warns if a case is missing

        var notifications = java.util.List.of(
                new EmailNotification("alice@example.com", "Hello", "Welcome!"),
                new SmsNotification("+1234567890", "Your OTP is 1234"),
                new PushNotification("device-abc", "New message")
        );

        for (Notification n : notifications) {
            String result = describe(n);
            System.out.println("  " + result);
        }
    }

    static String describe(Notification n) {
        // instanceof pattern matching with sealed type
        if (n instanceof EmailNotification e) {
            return "Email → " + e.to() + " | " + e.subject();
        } else if (n instanceof SmsNotification s) {
            return "SMS → " + s.phone() + " | " + s.message();
        } else if (n instanceof PushNotification(String deviceId, String title)) {
            return "Push → " + deviceId + " | " + title;
        }
        // No else needed — sealed ensures exhaustiveness
        throw new AssertionError("unreachable");
        // In Java 21 pattern switch, compiler enforces this automatically
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — Real World: Result Type
    // Sealed class as a typed error handling alternative to exceptions
    // ─────────────────────────────────────────────

    sealed interface Result<T> permits Result.Success, Result.Failure {
        record Success<T>(T value)       implements Result<T> {}
        record Failure<T>(String error)  implements Result<T> {}

        static <T> Result<T> success(T value)  { return new Success<>(value); }
        static <T> Result<T> failure(String e) { return new Failure<>(e); }

        default boolean isSuccess() { return this instanceof Success<T>; }

        default T getOrElse(T defaultValue) {
            return this instanceof Success<T> s ? s.value() : defaultValue;
        }
    }

    static Result<Integer> divide(int a, int b) {
        if (b == 0) return Result.failure("Division by zero");
        return Result.success(a / b);
    }

    static void resultType() {
        var r1 = divide(10, 2);
        var r2 = divide(10, 0);

        // Pattern match on result
        if (r1 instanceof Result.Success<Integer> s) {
            System.out.println("  10/2 = " + s.value());
        }

        if (r2 instanceof Result.Failure<Integer> f) {
            System.out.println("  10/0 error: " + f.error());
        }

        System.out.println("  getOrElse: " + r2.getOrElse(-1));

        // Result type pattern:
        // - No exceptions for expected failures
        // - Caller MUST handle both cases — forced by sealed type
        // - Composable — can chain with map/flatMap in functional style
        System.out.println("  Result<T>: type-safe error handling without exceptions ✓");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — Real World: Payment Domain
    // ─────────────────────────────────────────────

    sealed interface PaymentMethod permits CreditCard, BankTransfer, Crypto {}

    record CreditCard(String cardNumber, String expiry, String cvv)
            implements PaymentMethod {
        // Mask card number for display
        String masked() { return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4); }
    }

    record BankTransfer(String accountNumber, String routingNumber, String bankName)
            implements PaymentMethod {}

    record Crypto(String walletAddress, String currency)
            implements PaymentMethod {}

    static double processPayment(PaymentMethod method, double amount) {
        // Fee calculation based on payment type
        if (method instanceof CreditCard cc) {
            System.out.println("  processing card: " + cc.masked());
            return amount * 1.02; // 2% fee
        } else if (method instanceof BankTransfer bt) {
            System.out.println("  processing bank transfer via: " + bt.bankName());
            return amount + 0.50; // flat fee
        } else if (method instanceof Crypto c) {
            System.out.println("  processing " + c.currency() + " to: " + c.walletAddress());
            return amount * 1.01; // 1% fee
        }
        throw new AssertionError("unknown payment method");
    }

    static void paymentDomain() {
        var card     = new CreditCard("1234567890123456", "12/26", "123");
        var transfer = new BankTransfer("ACC123", "RTG456", "HDFC Bank");
        var crypto   = new Crypto("0xABCDEF", "ETH");

        double amount = 100.0;
        System.out.printf("  card total    : %.2f%n", processPayment(card,     amount));
        System.out.printf("  transfer total: %.2f%n", processPayment(transfer, amount));
        System.out.printf("  crypto total  : %.2f%n", processPayment(crypto,   amount));

        // Sealed hierarchy means:
        // - Adding a new PaymentMethod subtype requires updating permits list
        // - All switch/if-instanceof chains get compile warnings if incomplete
        // - No surprise subtypes at runtime
        System.out.println("  sealed: no surprise subtypes, exhaustive dispatch ✓");
    }

}