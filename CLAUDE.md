# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`moleculer-java-httpclient` is a small, asynchronous HTTP/WebSocket client **library** (not an application) for the Java Moleculer ecosystem. It wraps [AsyncHttpClient (AHC)](https://github.com/AsyncHttpClient/async-http-client) `3.0.7` and exposes a Promise-based API that speaks Moleculer's data types. Published to Maven Central as `com.github.berkesa:moleculer-java-httpclient`, version **`2.0.0`**. Bytecode target **Java 17** (`<release>17</release>`); minimum consumer runtime: **JDK 17** (Spring 6 transitive). Build JDK 17+ (JDK 25 in use). Single package: `services.moleculer.httpclient`.

## Build & test commands

The project builds with **Maven** (Java 17). There is no wrapper — use a locally installed `mvn`.

- Build + run tests + install to `~/.m2`: `mvn clean install`
- Full check (compile + tests): `mvn clean verify`
- Compile only: `mvn clean compile`
- Run tests: `mvn test`
- Run the single test class: `mvn test -Dtest=HttpClientTest`
- Release build (sources + javadoc + GPG sign + Central Portal publish): `mvn -Prelease deploy` (see `coordination/MAVEN-CENTRAL-PUBLISHING.md`)

### Build gotchas

1. **Netty version is pinned to `4.2.15.Final`.** `moleculer-java-web` brings a standalone Netty server at `4.2.15.Final`, while AHC `3.0.7` bundles Netty `4.2.9.Final`. The `pom.xml` imports `io.netty:netty-bom:4.2.15.Final` in `<dependencyManagement>` so every `io.netty:*` artifact resolves to one version — the embedded `ApiGateway`/`NettyServer` and the HTTP client share a single Netty on the port-8080 integration test. Verify with `mvn dependency:tree`.
2. **`HttpClientTest` is an end-to-end integration test, not a unit test.** Its `@BeforeEach setUp()` boots a real embedded Moleculer app — a `ServiceBroker` with a Netty `ApiGateway` listening on **port 8080** — then exercises the client against it over real HTTP and WebSocket; `@AfterEach tearDown()` stops both. Port 8080 must be free: `setUp()` probes it and `assumeTrue(...)`-skips the whole test (keeping `mvn verify` green) when it cannot bind. One large method covers all HTTP verbs, streaming, the `transferTo(...)` targets, and WebSockets. The `nio-multipart-parser` dependency is declared at **test scope** because the embedded `ApiGateway` needs it at runtime (web ships it as `optional`).

## Architecture

The whole library funnels through one class and one execution path. Read these to understand it: `HttpClient`, `RequestParams`, `ResponseHandler`, `WebSocketConnection`.

### Data types (not the JDK ones you'd expect)

- Requests/responses are `io.datatree.Tree` (a JSON-like dynamic structure), **not** POJOs or `JsonNode`.
- Async results are `io.datatree.Promise` (`.then(...).catchError(...)`, plus blocking `.waitFor(...)`), **not** `CompletableFuture`.
- Binary streaming uses Moleculer's `services.moleculer.stream.PacketStream` (from the `moleculer-java-web` / core dependency), **not** `InputStream`.

### HttpClient — the entry point

`HttpClient extends DefaultAsyncHttpClientConfig.Builder`. This is the key, non-obvious design choice: **the client *is* the AHC config builder.** You configure transport options by calling inherited builder methods directly on the `HttpClient` instance, then call `start()` to build the underlying `DefaultAsyncHttpClient`. Always `start()` before use and `stop()` to release resources (a `finalize()` safety net also closes them).

All HTTP verb methods (`get`/`post`/`put`/`delete`/`patch`/`head`/`options`/`connect`/`trace`) are thin overloads that converge on the single private `execute(url, method, Consumer<RequestParams>)`. `execute()`:
1. Builds a `RequestParams` (which extends AHC's `RequestBuilderBase`), applies the signature calculator and the caller's configurator.
2. Picks a default `ResponseHandler` if none was set — `ResponseToJson` normally, `ResponseToBytes` if `returnAsByteArray()` was requested.
3. Wraps AHC's callback-style `AsyncHandler` in a `Promise`, bridging callbacks → resolve/reject.

### Request configuration: the two configurators

The verb overloads wrap the user's `Consumer<RequestParams>` in one of two adapters before passing it to `execute()`:
- `TreeConfigurator` — for `Tree` bodies. For POST-like verbs it serializes the Tree to a JSON body; for GET-like verbs it converts the Tree into URL query params.
- `PacketStreamConfigurator` — for `PacketStream` bodies (chunked upload, or fixed length if a content-length is given via `PacketStreamBodyGenerator`).

`RequestParams` also offers output redirection via `transferTo(...)` overloads (`OutputStream`, `WritableByteChannel`, `PacketStream`, or a raw AHC `AsyncHandler`) and flags `returnStatusCode()` / `returnHttpHeaders()` / `returnAsByteArray()`.

### Response handling

`ResponseHandler` (abstract, `implements AsyncHandler<Tree>`) is the base for the response-parsing strategies: `ResponseToBytes` → `ResponseToJson` (default), plus `ResponseToOutputStream` and `ResponseToPacketStream` for streaming responses elsewhere. When `returnStatusCode`/`returnHttpHeaders` are enabled, status and headers are written into the response Tree's **Meta** section as `$status` (int) and `$headers` (map) — e.g. `rsp.getMeta().get("$status", 0)`. Header keys keep the server's casing (the gateway sends `Content-Type` / `Content-Length` capitalized). The body Tree itself stays clean otherwise.

> **AHC 3.x note:** unlike AHC 2.x, AsyncHttpClient 3.x does **not** call `onBodyPartReceived` for an empty response body, so `ResponseToBytes.bytes` can be `null` at `onCompleted()`. `ResponseToBytes`/`ResponseToJson` treat a `null` buffer as an empty body (empty `Tree` / empty `byte[]`).

### WebSocket client with heartbeat

`HttpClient.ws(...)` returns a `WebSocketConnection` and auto-rewrites `http(s)://` URLs to `ws(s)://`. Behavior lives in `WebSocketConnection`:
- A **built-in heartbeat** sends `"!"` text frames and detects dead connections, auto-reconnecting on timeout. The scheduler runs at `heartbeatInterval / 3`. Defaults (`WebSocketParams`, in seconds): `heartbeatInterval=60`, `heartbeatTimeout=10`, `reconnectDelay=3`.
- Incoming text frames are reassembled across fragments; `"!"` heartbeat frames are filtered out; JSON payloads (starting `{`/`[`) are parsed to `Tree`, other content wrapped in a `CheckedTree`, then delivered to `WebSocketHandler.onMessage`.
- `WebSocketHandler` is a `@FunctionalInterface` (only `onMessage` is required; `onOpen`/`onError`/`onClose` have defaults). State is managed with atomics; `connect()`/`disconnect()` return Promises and `waitForConnection(timeout, unit)` blocks until open.

## Conventions

- Every source file carries the MIT license header block — preserve it when creating new files.
- Public API methods are heavily Javadoc'd with usage `<pre>` examples; match that style for new public methods.
- Indentation is tabs.
