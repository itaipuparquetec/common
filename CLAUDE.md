## Code style

- Language: all code is written in **English** — identifiers (classes, methods, variables, files), test names and descriptions, comments and JavaDoc. Only user-facing text (labels, messages, i18n) stays in Portuguese.
- Functions: 4-20 lines. Split if longer.
- Files: under 500 lines. Split by responsibility.
- One thing per function, one responsibility per module (SRP).
- Names: specific and unique. Avoid `data`, `handler`, `Manager`.
  Prefer names that return <5 grep hits in the codebase.
- Field, variable and mock names are the name of their type in camelCase, in main and in tests:
  `TokenBlacklist tokenBlacklist`, `mock(AuthenticationService.class)` in `authenticationService`.
  Never a shortened name (`blacklist`, `authService`, `repo`); a different name only when two of the same type live
  together.
- Injected fields form a pyramid: the shortest `private final ...` line first, then the next shortest, up to the longest.
  With `@RequiredArgsConstructor` the constructor follows the order of the fields, so the `new XxxImpl(...)` in the tests
  follows the pyramid too.
- Types: explicit. No `any`, no `Dict`, no untyped functions.
- Early returns over nested ifs. Max 2 levels of indentation.
- Exception messages must include the offending value and expected shape.
- Exception classes must be named ending 'Exception'.
- Never serialize/deserialize Lists.
- The code must be like a natural language – So you can use prepositions like 'in','of', 'from', etc.

### Layers and boundaries

- A use case never returns an object of the domain (entity, aggregate, value object or a record of `domain/model`). It
  returns its own nested `Output` record, also inside `Page<Output>` or `Optional<Output>`. The domain never leaves the
  application layer.
- A `*Rest` answers its own response record (`*Response`), converted from the `Output` of its use case. It never
  exposes the domain model or a JPA entity, and never reads a repository directly: always through a use case.
- A microservice never calls another microservice directly: every synchronous call goes through the gateway, including
  the authorization server (token, JWKS, discovery). The clients this library offers (e.g. the TenantService client,
  the JWKS of the issuers) receive addresses of the gateway from the configuration, never the port of a service.

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
- The code must explain itself.
- If you need to JavaDoc comment on something, use English Language.

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
- Container images (Dockerfile `FROM`, docker-compose `image:`, CI job `image:`) always pin an explicit version (e.g. `nginx:1.30`, `postgres:18.1-alpine`, `semgrep/semgrep:1.184.0`); never `latest` nor an image without a tag. Images published by our own pipelines keep the tag the CI publishes (`SNAPSHOT` or the release).

## Structure

- Prefer small focused modules over god files.

## Logging

- Structured JSON when logging for debugging / observability.
- Never log sensitive data (LGPD data, passwords, keys, etc).
- Don't use logs to be like comments.
- Plain text only for user-facing CLI output.
- Use logs only when it makes sense.