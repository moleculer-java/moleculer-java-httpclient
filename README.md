[![Build Status](https://travis-ci.org/moleculer-java/moleculer-java-httpclient.svg?branch=master)](https://travis-ci.org/moleculer-java/moleculer-java-httpclient)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/407240ec7ee34a70bc4d5eb4517273fd)](https://www.codacy.com/manual/berkesa/moleculer-java-httpclient?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=moleculer-java/moleculer-java-httpclient&amp;utm_campaign=Badge_Grade)
[![codecov](https://codecov.io/gh/moleculer-java/moleculer-java-httpclient/branch/master/graph/badge.svg)](https://codecov.io/gh/moleculer-java/moleculer-java-httpclient)

# (WIP) moleculer-java-httpclient
Non-blocking, Promise-based HTTP client for Moleculer Applications.

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