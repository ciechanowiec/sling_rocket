# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

_Sling Rocket_ is a custom build of [Apache Sling](https://sling.apache.org/) distributed in two forms:

1. **SR Parent POM** (`eu.ciechanowiec:sling.rocket.parent`) — published to Maven Central. Business applications inherit from it to get the exact dependency/configuration set of the matching SR Instance.
2. **Docker images** (`ciechanowiec/rocket-base`, `ciechanowiec/rocket-instance`, `ciechanowiec/rocket-nginx`) — published to Docker Hub.

The Parent POM version and the SR Instance image tag **must match** within a deployment (currently `26.0.0-SNAPSHOT`). The persistence layer is Apache Jackrabbit Oak (Segment Node Store, JCR).

## Repository layout

- `docker-compose.yml` (repo root) — public deployment template; pulls prebuilt images.
- `src/docker-compose.yml` — internal build composition; builds the three images locally with `docker buildx`.
- `src/1_rocket-base/` — `eclipse-temurin:25-jdk` + shared apt setup (`commons/apt-installer.sh`).
- `src/2_rocket-instance/`
  - `Dockerfile` — copies the assembled OSGi feature archive (`*-oak_tar.far`) plus starter/installer scripts onto `rocket-base`.
  - `rocket-instance-installer.sh` — build-time: downloads the Sling Feature Launcher, warms up bundles, installs `oak-run` for backup/export.
  - `rocket-instance-starter.sh` — runtime entrypoint: assembles `JAVA_OPTS`, **purges the OSGi Installer cache** (this is intentional — prevents config conflicts across restarts), then `exec`s the Java launcher as PID 1.
  - `maven-project/` — the Java codebase (see below).
- `src/3_rocket-nginx/` — Nginx reverse proxy with ModSecurity (rules enabled), OWASP CRS, Ultimate Bad Bot Blocker, daily certbot renewal cron, logrotate.
- `src/4_rocket-sync/rocket-sync.sh` — local→remote JCR content sync via Composum Package Manager (see "Working with rocket-sync" below).
- `.github/workflows/release-maven-artifacts.yaml` — `mvn deploy -P release` to Maven Central.
- `.github/workflows/release-docker-images.yaml` — multi-arch (`linux/amd64,linux/arm64`) `docker buildx bake` to Docker Hub.
- `README.adoc` — authoritative docs; lints with Vale.

## Maven project structure (`src/2_rocket-instance/maven-project/`)

Java 25, packaging is OSGi-bundle-per-module aggregated into a single Sling Feature archive.

| Module              | Packaging | Role                                                                                       |
| ------------------- | --------- | ------------------------------------------------------------------------------------------ |
| `engine`            | jar       | The actual Java code (OSGi bundle). Domain packages: `asset`, `auth`, `calendar`, `commons`, `favicon`, `google`, `identity`, `jcr`, `job`, `llm`, `mail`, `network`, `observation`, `privilege`, `test`, `unit` |
| `feature`           | pom       | Aggregates all OSGi bundles + repoinit + configs into `*-oak_tar.far` via `slingfeature-maven-plugin`. Also writes `JACKRABBIT_OAK_VERSION` for the installer to consume. Source feature JSONs live in `feature/src/main/features/`. |
| `login-form`        | jar       | Static login form resources.                                                               |
| `oak-auth-external` | jar       | OSGi **fragment** (shaded with `Fragment-Host: org.apache.jackrabbit.oak-auth-external`) that re-exports the `...impl.jmx` package. |
| `sources`           | pom       | Bundles `git archive HEAD` as `sling.rocket.sources-<version>.zip` for the Maven Central release. |

The parent POM defines the canonical version/dependency set the SR Instance runs with — this is the same set business applications get when they declare it as their parent.

### Build conventions

- **Lombok** with `lombok.extern.findbugs.addSuppressFBWarnings = true` (so generated code is tagged for SpotBugs).
- **bnd-maven-plugin** generates `META-INF/MANIFEST.MF`; bnd directives (incl. `Sling-Nodetypes`, `Import-Package` exclusions for `lombok`/test scopes) are inlined per module.
- **Quality gates on the `engine` module** — all run during `package`:
  - Checkstyle, PMD, SpotBugs (`effort=Max`, `threshold=Low`) — gated by `${fail-build-on-static-code-analysis-errors}` (default `true`). Configs in `engine/src/main/resources/static_code_analysis/`.
  - JaCoCo — minimum 0.80 instruction **and** branch coverage, gated by `${enforce-tests-coverage}` (default `true`).
  - Both gates auto-disable when `-DskipTests=true` (via parent profiles `fail-build-on-static-code-analysis-errors-when-no-tests` and `enforce-tests-coverage-when-no-tests`). **Don't manually toggle the gate properties; flip `-DskipTests` instead.**
- **Vale linting of `README.adoc`** is bound to the parent build through OS-specific profiles (`vale-macos`, `vale-linux`) that download the Vale binary, plus a JVM-based `vale-asciidoctor-wrapper` so machines without a system asciidoctor can still lint.

## Common commands

Run all Maven commands from `src/2_rocket-instance/maven-project/`.

```bash
# Full local build: Maven artifacts → ~/.m2 (parent POM + engine bundle + feature archive)
mvn clean install

# Build all Docker images locally (relies on artifacts in target/ from the step above)
cd src && docker compose --progress=plain build

# Run the deployed stack (uses prebuilt images from Docker Hub)
docker compose up -d   # from repo root, NOT src/

# Run a single test
mvn -pl engine test -Dtest=ClassName#methodName

# Skip tests AND auto-relax static-analysis/coverage gates
mvn clean install -DskipTests

# Install the engine bundle into a running local Sling instance (admin:admin @ localhost:8080)
mvn -pl engine install -P installBundle

# Release to Maven Central (CI only, normally)
mvn clean deploy -P release

# Lint README.adoc directly (without going through Maven)
vale README.adoc
```

The bundle install profile uses `<sling.host>`/`<sling.port>`/`<sling.user>`/`<sling.password>` parent properties, which default to `localhost:8080` / `admin:admin`.

## Architecture notes

- **Three-container production stack** (see `docker-compose.yml`): `rocket-instance` (OSGi container, ports 8080 HTTP / 8081 JDWP) ← `rocket-nginx` (80/443, ModSecurity WAF) → outside world, with `rocket-backuper` (offen/docker-volume-backup) as a sidecar driven by `BACKUP_CRON_EXPRESSION`.
- **Three named volumes**:
  - `rocket-data-raw` → `/opt/sling/launcher/repository/segmentstore` (live Oak segments — *the* database; must be persisted across upgrades).
  - `rocket-data-dump` → `/var/rocket-data-dump` — populated by `dump-rocket-data.sh` (an `oak-run backup` + `oak-run export`) which the backuper triggers via `archive-pre`. The instance container's `stop_grace_period` is 300s so the JCR closes cleanly.
  - `rocket-instance-logs` → `/opt/sling/launcher/logs`.
- **Run modes** are comma-separated via the `RUN_MODES` env var. **Multi-version support** is opt-in via `ENABLE_MULTI_VERSION_SUPPORT`. **Debug** is enabled when `JAVA_DEBUG_PORT` is set.
- **Default credentials are admin/admin**; production deployments must change them and follow the README's "Production Deployments" checklist (referrer filter, request size limits, `client_max_body_size`, ZAP/Nuclei scans, etc.).

## Web API and JCR conventions

When writing or reviewing business code in `engine/`, follow the conventions documented in `README.adoc`:

- **APIs** live at `/api/<domain>/<api-name>` and the path **must exist as JCR nodes** with `sling:resourceType` set on the terminal node. Bind servlets via `sling.servlet.resourceTypes`, **not** `sling.servlet.paths`.
- **JCR root layout** (recommended): `/api`, `/apps/<domain>/{application,install,osgiconfig}`, `/content/<domain>`, `/home/{users,groups}`, `/libs`, `/oak:index`, `/var/{eventing,packages}`. The `/apps/<domain>/install` and `/apps/<domain>/osgiconfig` paths are auto-installed by Sling on deployment.

## Working with rocket-sync

`src/4_rocket-sync/rocket-sync.sh put [path]` zips a FileVault subtree (anything under `jcr_root/...`) and POSTs it to Composum's `/bin/cpm/package.service.html`. **The operation overwrites everything within the filter scope on the server** — the script refuses to run on the repo root for that reason. Configurable via `.rocket-sync` files searched upward from `pwd` (`server=`, `credentials=`).

## Documentation

`README.adoc` is the source of truth (HTML/PDF/DOCX exports are checked-in artifacts). Editing the README requires rebuilding those exports — see "Export" in the README for the asciidoctor/asciidoctor-pdf/pandoc commands.
