## Code style

- Functions: 4-20 lines. Split if longer.
- Files: under 500 lines. Split by responsibility.
- One thing per function, one responsibility per module (SRP).
- Names: specific and unique. Avoid `data`, `handler`, `Manager`.
  Prefer names that return <5 grep hits in the codebase.
- Types: explicit. No `any`, no `Dict`, no untyped functions.
- Early returns over nested ifs. Max 2 levels of indentation.
- Exception messages must include the offending value and expected shape.
- Exception classes must be named ending 'Exception'.
- Never serialize/deserialize Lists.
- The code must be like a natural language – So you can use prepositions like 'in','of', 'from', etc.

### Demeter Law

- Try to preserve Demeter Law.

#### 1. Identifying Violations (Anti-patterns)

- Watch out for long method chains (commonly known as "train wrecks").
- Violation example: `order.getCustomer().getWallet().deductBalance(amount);`.
- Identify when an object excessively exposes its internal structure through chained getters and setters.

#### 2. Refactoring Strategy (How to Fix)

- Always apply the "Tell, Don't Ask" principle. Instead of asking an object for its data to make a decision, tell the
  object what to do.
- Create delegatory methods in the intermediate class to encapsulate behavior.
- Fixed example: `order.chargeCustomer(amount);` (Where the `Order` class internally forwards the command without
  exposing the `Customer` or `Wallet` classes).

#### 3. Allowed Exceptions

- **Fluent Interfaces / Builders**: Method chains that return the object itself (e.g.,
  `StringBuilder.append().append()`) do not violate the law.

## Comments

- Try to not use comments.

## Tests

- Run tests with a single command: `mvn clean verify`.
- Run lint with a single command: `mvn compile checkstyle:check`.
- Every new function gets a test. Bug fixes get a regression test.
- Mock external I/O (API, DB, filesystem) with named fake classes, not inline stubs.
- Tests must be F.I.R.S.T: fast, independent, repeatable, self-validating, timely.
- Use AAA (Arrange, Act, Asserts) without comments.
- Use private auxiliary methods at the end of the class.
- Use @ParameterizedTests ever where possible or necessary.
- Try to use only assertJ, unless you have a good reason not to.
- Try to mock with Mockito without annotations like `@Mock`, `@InjectMocks`, `@MockBean`, unless you have a good reason not to.
- The min test coverage is 100%.

## Dependencies

- Try to inject dependencies through constructor/parameter, try to don't use global/import.

## Structure

- Prefer small focused modules over god files.

## Logging

- Structured JSON when logging for debugging / observability.
- Never log sensitive data (LGPD data, passwords, keys, etc).
- Don't use logs to be like comments.
- Plain text only for user-facing CLI output.
- Use logs only when it makes sense.