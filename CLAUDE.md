# CLAUDE.md

Spring Boot app that consumes `de.tehwolf:yaft` **from Maven Central** and runs
it against a real YaFT backend. Its job is to find what the library's own
tests cannot: disagreements with the backend and with Spring.

## Rules

- The yaft version in `gradle/libs.versions.toml` must be a released one. CI
  resolves only from Maven Central; `-PuseMavenLocal` is for trying an
  unreleased fix locally and never belongs in CI.
- Toggled beans are created with `YaFT.create` / `YaFT.wrap` inside `@Bean`
  methods that take the `ApiFeatureProvider` as a parameter, so the provider
  is loaded before any class toggle is decided.
- Seeded toggles match `yaft-playground` (TypeScript). Change both together.
- `seed.properties` holds the group secret; it is gitignored and written 0600.

## Pre-commit validation

```bash
./gradlew build
./scripts/backend.sh up && ./scripts/seed.sh && ./gradlew e2eTest
```

The backend images are private on GHCR. If `docker pull` is denied, build them
from `Go/YaFT` (`docker build -t ghcr.io/tehw0lf/yaft:latest .` and
`docker build -f db/Dockerfile -t ghcr.io/tehw0lf/yaft-db:latest .`), as the
CI does.
