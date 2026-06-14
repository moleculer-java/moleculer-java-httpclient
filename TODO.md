# TODO — Modernize `moleculer-java-httpclient` to 2.0.0

> **You are the per-project Claude Code instance for `moleculer-java-httpclient`.** Self-contained.
> Goal: Gradle/Java 8 → **Maven + JDK 21**, upgrade the async HTTP stack, tests green on **JUnit 5**,
> legacy files removed, version **2.0.0**. This is an async, Promise-based HTTP/WebSocket **client**
> library that speaks Moleculer data types (`Tree`/`Promise`/`PacketStream`). Single package:
> `services.moleculer.httpclient`. Built **last** (depends on the web gateway).

## Coordinates & facts
- Maven: `com.github.berkesa:moleculer-java-httpclient`, `jar`, license **MIT**.
- `name`: *Promise-based HTTP client for the Moleculer Applications* · `inceptionYear`: 2019
- `url`: https://moleculer-java.github.io/moleculer-java-httpclient/ · `scm`: https://github.com/moleculer-java/moleculer-java-httpclient.git
- developer: `berkesa` / Andras Berkes / andras.berkes@programmer.net
- **Version → `2.0.0`** (old build: `version` + `jar` block → single Maven `<version>`).

## Inter-project dependency (PIN to 2.0.0)
- `com.github.berkesa:moleculer-java-web:2.0.0` (was **1.3.6** — actual web is 1.3.12; now unified).
  Build the whole chain up to `moleculer-java-web` first. While developing, pin
  **`2.0.0-SNAPSHOT`** (`${moleculer.version}` style), flipped to `2.0.0` at release.

> **🔔 Orchestrator note (2026-06-13): the whole upstream chain is DONE and installed.** Every
> dependency you need is already in the local repo as `2.0.0-SNAPSHOT` — `moleculer-java-web` (✅
> just finished, 33 tests green), and through it `moleculer-java`, `moleculer-java-repl`,
> `datatree-templates/-adapters/-promise/-core`. You can resolve them immediately; no need to build
> anything upstream yourself. **You are the last code project.**
>
> **⚠ Watch for a Netty version clash on the port-8080 integration test.** `moleculer-java-web`
> brings **Netty `4.2.15.Final`** transitively (its standalone `ApiGateway`/`NettyServer`, which your
> `HttpClientTest` boots). If you take **AHC 3.x (Option A)**, AHC **bundles its own Netty** — if the
> two disagree you get `NoSuchMethodError`/`NoClassDefFoundError` at runtime on the test. Reconcile to
> a single Netty: prefer letting web's **4.2.15.Final** win (add it to `<dependencyManagement>` or
> exclude AHC's transitive `io.netty:*` and depend on web's), and verify with `mvn dependency:tree`.
> Choosing **Option B (JDK `java.net.http.HttpClient`)** sidesteps AHC's Netty entirely — only web's
> Netty remains, no clash. Either way, keep **Netty at `4.2.15.Final`** for workspace lockstep.
>
> Shared versions already locked across the workspace — **reuse, do not re-pick:** `slf4j-*`
> **2.0.18**, `junit-jupiter` **5.14.4** (via `junit-bom`), compiler **3.15.0**, surefire **3.5.4**,
> source **3.3.1** / javadoc **3.11.2** / gpg **3.2.7** / central-publishing **0.9.0**.

## ⚠ Biggest task: the AsyncHttpClient (AHC) upgrade
The current `HttpClient` **extends `org.asynchttpclient.DefaultAsyncHttpClientConfig.Builder`** —
i.e. "the client *is* the AHC config builder", and `RequestParams` extends AHC's
`RequestBuilderBase`. AHC 2.10.4 is old; choose one:
- **Option A (recommended): AHC 3.x** (`org.asynchttpclient:async-http-client:3.x`). Same library
  family, but 3.x changed/relocated APIs — fix the `Builder`/`RequestBuilderBase`/`AsyncHandler`
  usages, the signature calculator, and the `Response`/`HttpResponseBodyPart` callbacks.
- **Option B: JDK `java.net.http.HttpClient`** (Java 11+). Removes the AHC dependency entirely but
  is a larger rewrite of `HttpClient`/`RequestParams`/`ResponseHandler`/`WebSocketConnection`.

Either way, **preserve the public API and behavior:** verb overloads converging on
`execute(url, method, Consumer<RequestParams>)`; `TreeConfigurator` vs `PacketStreamConfigurator`;
`ResponseToJson` (default) / `ResponseToBytes` / streaming handlers; `$status`/`$headers` written
into the response Tree's **Meta**; and the `ws(...)` `WebSocketConnection` with its heartbeat
(`"!"` frames, `heartbeatInterval=60`, `heartbeatTimeout=10`, `reconnectDelay=3`, reconnect logic).

## Dependency actions
| Dependency | Current | Target | Scope |
|---|---|---|---|
| `org.asynchttpclient:async-http-client` | 2.10.4 | **3.x** (or drop for JDK HttpClient) | compile |
| `com.github.berkesa:moleculer-java-web` | 1.3.6 | **2.0.0** | compile |
| `org.slf4j:*` | 1.7.30 | **2.0.18** | compile/runtime |
| `junit:junit` 4.12 | → `junit-jupiter` | **5.x** | test |
| Eclipse `ecj` 4.4.2 | — | **remove** | — |
| Java | 1.8 | **21** | — |

## Steps
1. **`pom.xml`** (metadata + MIT + `release=21`). Deps per table; `moleculer-java-web:2.0.0-SNAPSHOT`.
   Build plugins: compiler **3.15.0**, surefire **3.5.4** (lockstep), (release profile) sources/javadoc/gpg +
   central-publishing 0.9.0.
2. **Remove ECJ** → javac.
3. **Upgrade AHC** (Option A) or migrate to JDK HttpClient (Option B); keep the public API/behavior.
4. **Tests → JUnit 5.** `HttpClientTest` is an **integration test**: `setUp()` boots a real
   `ServiceBroker` + Netty `ApiGateway` on **port 8080** and exercises every HTTP verb + streaming +
   WebSocket; `tearDown()` stops both. Port 8080 must be free — keep it as one end-to-end test; if
   the env can't bind 8080, tag/`@Disabled` it so `mvn verify` stays green. Migrate to Jupiter.
5. **Cleanup — delete:** `build.gradle`, `settings.gradle`, `gradlew`, `gradlew.bat`, `gradle/`,
   `.gradle/`, `.travis.yml`, `.classpath`, `.project`, `.settings/`. (No `.codacy.yaml` here.)
6. **VSCode + .gitignore** (library — no `launch.json`; the test class is the entry point).
7. **Build & install:** `mvn clean install`, then `mvn clean verify`.
8. **Update `CLAUDE.md`:** Maven commands; the AHC upgrade choice; version `2.0.0`; web dep now
   `2.0.0` (fixes the old 1.3.6 lag).

## Definition of done
- `mvn clean verify` green on JDK 21 (8080-bound integration test disabled if port unavailable).
- HTTP stack upgraded (AHC 3.x or JDK HttpClient) with public API/behavior preserved.
- JUnit 5; legacy files gone; VSCode + .gitignore; version `2.0.0`; depends on
  `moleculer-java-web:2.0.0`; publishing configured.
