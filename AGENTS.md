# AGENTS.md

## Stack

* Java 25
* Spring Boot 4.x
* Maven
* Spring MVC
* PostgreSQL
* JPA/Hibernate
* JUnit 5 + AssertJ
* Mockito, used sparingly
* Testcontainers for infrastructure integration tests
* ArchUnit for architecture enforcement

Start with a single Maven module. Split into modules or bounded contexts only when complexity justifies it.

## Architecture

Use hexagonal architecture, initially organized as:

```text id="xdxshq"
domain/
application/
adapter/
  in/
  out/
configuration/
```

Prefer package-by-feature/bounded-context with this structure inside each context when the application becomes large enough to justify it.

Dependencies point inward.

* `domain` is pure Java and depends on no other application layer.
* `application` may depend on `domain`.
* `adapter.in` may depend on `application` and `domain`, never `adapter.out`.
* `adapter.out` may depend on `application` and `domain`, never `adapter.in`.
* `configuration` is the composition root and may depend on all layers for wiring.
* Adapters never bypass the application layer to communicate with each other.
* Framework and infrastructure concerns stay outside the application core.

### Enforce architecture

Architectural boundaries MUST be enforced with ArchUnit, including:

* layer/package dependency direction;
* isolation of inbound and outbound adapters;
* domain independence from Spring, JPA/Hibernate, Jakarta Validation, Jackson, and other framework/infrastructure dependencies;
* application independence from adapters and configuration.

When an architectural constraint can reasonably be mechanically enforced, prefer an ArchUnit rule over documentation alone.

Never weaken, suppress, exclude, or work around an architecture test merely to make an implementation pass. Discuss changing the architectural decision first.

## Domain

Apply DDD rigorously while avoiding unnecessary ceremony.

* Keep the domain completely framework-independent.
* Model behaviour, not data containers. Avoid anemic domain models.
* Aggregates protect their own invariants.
* Modify aggregates through meaningful domain behaviour, not public setters.
* Do not expose mutable internal collections.
* Prefer value objects for concepts with domain meaning or invariants; avoid primitive obsession.
* Use entities, aggregates, value objects, domain services, factories, and domain events only where the domain warrants them.
* Domain services contain domain behaviour that does not naturally belong to one entity/value object.
* Domain events represent meaningful facts that occurred and should normally be named in the past tense.
* Make invalid domain states difficult or impossible to represent.
* Do not invent business rules when requirements are unclear.

## Application

The application layer orchestrates domain behaviour and contains as little business logic as possible.

Prefer explicit, narrow inbound use cases:

```java id="09nuy5"
interface PlaceOrderUseCase {
    OrderId placeOrder(PlaceOrderCommand command);
}
```

* Prefer use-case-specific interfaces over large generic `*Service` APIs.
* Name a use case interface after the action, in the imperative (`SubmitTimesheetUseCase`, with a
  `SubmitTimesheetCommand`), and its implementation after the action in progress, as a present participle
  (`SubmittingTimesheet`). This keeps implementations from reading like domain objects or commands.
* Separate commands and queries conceptually, without introducing CQRS infrastructure unless justified.
* Define outbound ports in the application core for external capabilities it requires.
* The application core owns abstractions; adapters implement them.
* Do not create interfaces without a meaningful boundary, role, or reason for substitution.

## Adapters

### Inbound

Controllers are thin and responsible for HTTP concerns only.

Typical flow:

```text id="ovvh0j"
Request DTO → validation → mapping → command/query → use case
Response    ← response DTO ← result
```

* Keep API DTOs separate from domain models.
* Never put business logic in controllers.
* Never access persistence repositories directly from controllers.
* Never expose persistence entities through the API.

### Outbound / persistence

PostgreSQL + JPA/Hibernate is an infrastructure implementation detail.

* Keep JPA/Hibernate out of the domain and application core.
* Keep persistence entities separate from domain entities.
* Map explicitly between persistence and domain models.
* Spring Data repositories remain inside the persistence adapter.
* Persistence adapters implement outbound application ports.
* Do not return JPA entities across the adapter boundary.
* Avoid bidirectional JPA relationships unless clearly justified.
* Accept mapping overhead when it protects architectural boundaries.

Persistence should be replaceable without redesigning the application core.

## Validation

Validate at the layer responsible for the rule:

* **Transport/API constraints:** inbound DTO using Jakarta Validation where appropriate.
* **Domain value validity:** domain/value-object construction.
* **Business invariants:** domain model.

API validation improves feedback but MUST NOT be the only protection for domain invariants.

The domain must remain valid regardless of which inbound adapter invokes it.

## Error Handling

Handle errors at the earliest layer with enough context to handle them correctly.

* Domain errors express domain meaning, never HTTP semantics.
* Infrastructure-specific exceptions must not leak into the application core.
* Translate infrastructure failures at adapter boundaries when necessary.
* Map application/domain errors to HTTP centrally, e.g. with `@RestControllerAdvice`.
* Do not catch exceptions merely to log and rethrow them.
* Do not use exceptions to compensate for unclear domain modeling.

## SOLID & Code Design

Apply SOLID as design guidance, not ceremony.

* Prefer cohesive classes with clear responsibilities.
* Prefer composition over inheritance.
* Prefer narrow interfaces.
* Dependencies point toward abstractions owned by the application core.
* Do not create an interface for every class.
* Do not create speculative extension points.
* Prefer immutability where practical.
* Use records for suitable immutable data carriers/value objects, not mechanically.
* Use domain language in names.
* Avoid vague `Manager`, `Helper`, `Utils`, `Common`, and giant `*Service` classes.
* Prefer explicit dependencies and constructor injection.
* No field injection.
* Keep domain and application classes unaware of Spring where plain Java suffices.
* Wire application components in `configuration`.

Every abstraction should protect a boundary, represent a domain concept, invert a dependency, support meaningful variability, or materially improve reasoning/testing. Otherwise, it is probably unnecessary.

## Testing

Test behaviour and observable outcomes rather than implementation details.

### Domain

* Plain unit tests without Spring.
* Test invariants, behaviour, state transitions, value objects, domain events, and edge cases.
* Use real domain objects.

### Application

* Test use cases with real domain objects.
* Mock as little as practical.
* Prefer simple fakes/in-memory implementations when clearer than mocks.
* Use Mockito when interaction with an external dependency is relevant.

### Adapters

* Test adapters against their real technology where practical.
* Prefer Testcontainers + PostgreSQL for persistence integration tests.
* Test HTTP mapping, validation, serialization, status/error mapping, and use-case invocation.
* Prefer narrow test slices over `@SpringBootTest` when sufficient.

### Test style

* Prefer Given / When / Then structure.
* Focus each test on one behavioural outcome.
* Prefer one assertion per test where practical, but multiple assertions are fine when they describe one behaviour.
* Do not test private methods, trivial getters, or irrelevant internal interactions.
* Refactoring implementation details should not break behaviour-focused tests.

## Avoid

Unless explicitly justified, avoid:

* anemic domain models;
* business logic outside the domain when it belongs in the domain;
* business logic in controllers or persistence adapters;
* JPA entities leaking outside persistence adapters;
* Spring/JPA/Jackson/validation annotations in the domain;
* direct controller → repository dependencies;
* public setters on domain entities;
* field injection;
* unnecessary interfaces or mapping layers;
* primitive obsession;
* excessive mocking;
* unnecessary `@SpringBootTest`;
* speculative abstractions;
* cross-layer shortcuts;
* shared mutable state;
* framework convenience dictating domain design.

## Agent Workflow

Before implementing:

1. Read the relevant existing code and tests.
2. Understand the domain language, use case, and affected invariants.
3. Identify the architectural layers involved.
4. Follow existing conventions unless they conflict with these guardrails.
5. Ask rather than invent when domain rules or important architectural decisions are ambiguous.

Prefer small, coherent changes.

For new behaviour, generally work from the domain outward:

```text id="iip44f"
domain → use case → ports → adapters → configuration
```

Add/update tests and ArchUnit rules as part of the change.

Before adding a dependency, check whether the JDK or existing dependencies already solve the problem. New dependencies require a clear justification.

Do not perform unrelated architectural refactoring while implementing a feature.

Run relevant tests during development and the complete Maven build before declaring the work finished. Prefer the Maven wrapper when present:

```bash id="4y9plq"
./mvnw verify
```

## Architectural Changes

Discuss significant architectural changes before implementing them, including:

* changing aggregate boundaries;
* creating/splitting bounded contexts;
* splitting Maven modules;
* adding major frameworks;
* introducing messaging, event sourcing, or another database;
* changing persistence strategy;
* breaking existing ports.

Explain the problem, proposed change, alternatives, and trade-offs.

## Guardrail Priority

When concerns conflict, prioritize:

1. Correct domain behaviour and invariants
2. Architectural boundaries
3. Clarity and maintainability
4. Testability
5. Simplicity
6. Reuse
7. Framework convenience

Be strict about boundaries and pragmatic about implementation.

> Simplify inside the boundaries, not across them.

If a task appears to require violating these guardrails, stop and discuss it first. Explain the rule being broken, why, the boundary-preserving alternatives, and the trade-off.

Do not break an architectural boundary without explicit agreement and a strong technical reason.
