[![Build Status](https://travis-ci.org/moleculer-java/moleculer-java-httpclient.svg?branch=master)](https://travis-ci.org/moleculer-java/moleculer-java-httpclient)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/407240ec7ee34a70bc4d5eb4517273fd)](https://www.codacy.com/manual/berkesa/moleculer-java-httpclient?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=moleculer-java/moleculer-java-httpclient&amp;utm_campaign=Badge_Grade)
[![codecov](https://codecov.io/gh/moleculer-java/moleculer-java-httpclient/branch/master/graph/badge.svg)](https://codecov.io/gh/moleculer-java/moleculer-java-httpclient)

# Non-blocking HTTP client for Moleculer

## Description

The "moleculer-java-httpclient" is an asynchronous HTTP client API,
specially designed for Java-based
[Moleculer](https://moleculer-java.github.io/moleculer-java/)
ecosystem. The client can receive WebSocket messages from the server. The built-in Heartbeat function automatically checks if a connection has been lost. If a connection is lost, the client automatically rebuilds the connection.

## Download

**Maven**

```xml
<dependencies>
	<dependency>
		<groupId>com.github.berkesa</groupId>
		<artifactId>moleculer-java-httpclient</artifactId>
		<version>1.0.0</version>
		<scope>runtime</scope>
	</dependency>
</dependencies>
```

**Gradle**

```gradle
dependencies {
	compile group: 'com.github.berkesa', name: 'moleculer-java-httpclient', version: '1.0.0' 
}
```

## Examples

### Blocking usage

Blocking is not recommended, but you can use the "waitFor" method to wait for the server response if necessary. The "Tree" object is practically a JSON structure. Its contents can be retrieved using "get" function calls, as in a "Map" object.

```java
Tree rsp = client.get("http://host/rest").waitFor();
```

### Invoking a REST service

In the example below, we call a REST service that provides a JSON response to a JSON request.

```java
// Create HTTP client (connection pool and timeout handler)
HttpClient client = new HttpClient();
client.start();

// Build JSON request
Tree req = new Tree();
req.put("key1", "value1");
req.put("key2", 123);
req.put("key3", true);

client.post("http://host/path", req).then(rsp -> {

    // Success (process JSON response)
    String value4 = rsp.get("key4", "defaultValue");

}).catchError(err -> {

    // Failed
    err.printStackTrace();

});
```

### Invoke HTTP method with custom parameters

```java
client.get("http://host/rest", params -> {
	
    // Set parameters of the request
    params.addHeader("HttpHeader", "value");
    params.setCharset(StandardCharsets.ISO_8859_1);
			
}).then(rsp -> {

    // Parsing response ("rsp" is a "Tree" object)
    System.out.println(rsp.get("key", "defaultValue"));
			
});
```

### Receiving WebSocket messages

In the following case, the client connects to the server immediately. The heartbeat timer checks the connection every minute.

```java
client.ws("http://localhost:3000/ws/jmx", payload -> {

    // Message received!
    System.out.println("RECEIVED: " + payload);
    
});
```

### Advanced WebSocket handling with connection events

In the example below, you can create your own event listeners for the WebSocket connection and also specify connection parameters.

```java
WebSocketConnection ws =
    client.ws("http://localhost:3000/ws/jmx", new WebSocketHandler() {

    public void onOpen(WebSocket webSocket) {

        // WebSocket connection opened
    }

    public void onMessage(Tree payload) {

        // Receiving message
        System.out.println("RECEIVED:\r\n" + payload);				
    }

    public void onError(Throwable t) {

        // Error occured
    }

    public void onClose(WebSocket webSocket, int code, String reason) {

        // WebSocket connection closed
    }
			
}, params -> {
			
    // Set connection parameters
    params.setHeader("HttpHeaderName", "value");
    params.setHeartbeatInterval(120); // 2 mins
			
}, false);
		
// Connect to server
ws.connect();
		
// Close connection
ws.disconnect();
```

### Redirecting response into a Stream or Channel

The 'transferTo' method is used to redirect the response to an OutputStream, WritableByteChannel or PacketStream.

```java
// Create OutputStream
FileOutputStream out = new FileOutputStream("/target.txt");

client.get("http://host/path", params -> {
			
    // Redirect response into OutputStream
    params.transferTo(out);
			
}).then(rsp -> {
			
    // Transfer finished
			
});
```

### Processing response without parsing

As a result of the "returnAsByteArray" method, the client API will not attempt to process the response as JSON, but will return it in raw byte-array format.

```java
client.get("http://host/path", params -> {
			
    // Do not parse the response
    params.returnAsByteArray();
			
}).then(rsp -> {
			
    // Get response body
    byte[] bytes = rsp.asBytes();
			
});
```

### Processing status code and headers of the response

The "returnStatusCode" and "returnHttpHeaders" methods cause the client API to copy the status code and http headers into the "meta" block of the response Tree.

```java
client.get("http://host/path", params -> {
			
    // Instructs the client to copy the values
    // into the "meta" block
    params.returnStatusCode();
    params.returnHttpHeaders();
			
}).then(rsp -> {
			
    // Get status code
    Tree meta = rsp.getMeta();
    int status = meta.get("$status", 0);
			
    // Get headers
    for (Tree header: meta.get("$headers")) {
        String headerName = header.getName();
        String headerValue = header.asString();
    }
			
});
```

## License
This project is available under the [MIT license](https://tldrlegal.com/license/mit-license).
