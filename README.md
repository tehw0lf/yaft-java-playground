<p align="center"><img src="logo.svg" width="120" alt="YaFT"></p>

# yaft-java-playground

Proves that [`de.tehwolf:yaft`](https://central.sonatype.com/artifact/de.tehwolf/yaft),
exactly as Maven Central serves it, works inside a Spring Boot application
against a running YaFT backend — not just against a local stub.

The library's own suite runs against an HTTP stub, and
[yaft-conformance](https://github.com/tehw0lf/yaft-conformance) runs against
case data. Neither can see the library disagree with the backend, or with
Spring. This repository can, and it did before its first commit landed:

- **Every annotated toggle read off.** The backend keys toggles as
  `<group uuid>|<name>`, and a Java annotation value must be a compile-time
  constant, so `@FeatureToggle` could never name the full key. Against the
  published 0.2.2 all six seeded toggles evaluate off. Fixed in 0.2.3: the
  API provider resolves a bare name within its group.
- **Toggles on Spring-proxied beans were silently ignored.** `YaFT.wrap`
  around a bean Spring had already proxied (CGLIB or JDK) returned it
  unchanged. Since 0.2.3 it refuses, pointing at the order that works.

It is the Java counterpart of [yaft-playground](https://github.com/tehw0lf/yaft-playground)
(TypeScript) and seeds the same toggles, so both ports are checked against the
same data.

## What it shows

| | How | Evaluated |
|---|---|---|
| Toggles from the backend | `ApiFeatureProvider`, parsing with Spring's own Jackson `JsonMapper` | locally, against the clock |
| Class toggle | `@Bean Checkout checkout() { return YaFT.create(...); }` | once, when the bean is created (R14) |
| Method toggle | `@Bean Greeter greeter() { return YaFT.wrap(...); }` | on every call (R15) |

`GET /toggles` answers every seeded toggle by name, `GET /demo` calls the two
toggled beans, and `POST /refresh` pulls the group immediately.

### Spring proxies

| Order | Result |
|---|---|
| `YaFT.wrap` first, Spring advises the YaFT proxy | ✅ both apply |
| Spring proxies first, then `YaFT.wrap` | refused since 0.2.3 (silently ignored before) |

So create toggled beans with `YaFT.wrap` / `YaFT.create` in a `@Bean` method;
aspects, `@Transactional` and friends then wrap the result as usual.
`SpringProxyInteractionTest` checks both orders against real Spring proxies.

## Running it

```bash
./scripts/backend.sh up      # the published backend images, on 127.0.0.1:8080
./scripts/seed.sh            # creates the toggles, writes seed.properties
./gradlew bootRun            # http://localhost:8081
```

```bash
./gradlew check              # unit tests, Spring proxies included; no backend needed
./gradlew e2eTest            # against the running, seeded backend
./scripts/backend.sh down    # stop it and drop the volume
```

The e2e suite also flips two toggles **in the backend**: the method toggle
follows on the next call, the class toggle keeps what it decided at startup.
It puts them back afterwards.

`-PuseMavenLocal` builds against a yaft-java version published only with
`./gradlew publishToMavenLocal`, to try a fix before it is released. CI never
uses it.

### Against the deployed instance

```bash
API_URL=https://yaft.tehwolf.de ./scripts/seed.sh
./gradlew e2eTest
```

In CI this is `e2e-live.yml`, started by hand: every run creates a toggle group
in production.

## License

MIT
