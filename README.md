## Non-blocking HTTP client for Moleculer

The "moleculer-java-httpclient" is an asynchronous HTTP client API,
specially designed for Java-based Moleculer Ecosystem.
The client is suitable for handling large numbers of REST requests,
and it can receive WebSocket messages from a Netty/J2EE-based Moleculer application.
The built-in Heartbeat function automatically checks if a connection has been lost.
If a connection is lost, the client automatically rebuilds the connection.

Requires **JDK 17**. Built on [AsyncHttpClient](https://github.com/AsyncHttpClient/async-http-client) 3.x.

## Documentation

[![Documentation](https://raw.githubusercontent.com/moleculer-java/site/master/docs/docs-button.png)](https://moleculer-java.github.io/site/http-client.html)

## Download

```xml
<dependency>
    <groupId>com.github.berkesa</groupId>
    <artifactId>moleculer-java-httpclient</artifactId>
    <version>2.0.0</version>
</dependency>
```

## License

This project is available under the [MIT license](https://tldrlegal.com/license/mit-license).