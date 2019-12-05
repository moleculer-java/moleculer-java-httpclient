/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2019 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer.httpclient;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.asynchttpclient.ws.WebSocket;
import org.junit.Test;

import io.datatree.Promise;
import io.datatree.Tree;
import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.Context;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.service.Action;
import services.moleculer.service.Service;
import services.moleculer.stream.PacketStream;
import services.moleculer.web.ApiGateway;
import services.moleculer.web.netty.NettyServer;

public class HttpClientTest extends TestCase {

	// --- CONSTANTS ---

	private static final String TEST_URL = "http://127.0.0.1:8080/test.action";
	private static final String RECEIVER_URL = "http://127.0.0.1:8080/streamReceiver.receive";

	// --- VARIABLES ---

	protected AtomicReference<Context> lastCtx = new AtomicReference<>();
	protected HttpClient cl = new HttpClient();
	protected ServiceBroker br;

	// --- INIT / DESTROY TEST ---

	@Override
	@SuppressWarnings("unused")
	protected void setUp() throws Exception {

		// Start server
		ServiceBrokerConfig cfg = new ServiceBrokerConfig();
		cfg.setMonitor(new ConstantMonitor());
		br = new ServiceBroker(cfg);
		br.createService(new NettyServer(8080));
		ApiGateway gw = new ApiGateway("*");
		gw.setDebug(true);
		gw.setBeforeCall((route, req, rsp, data) -> {
			if (req == null || data == null) {
				return;
			}
			Tree meta = data.getMeta();
			meta.put("method", req.getMethod());
			meta.put("path", req.getPath());
			meta.put("query", req.getQuery());
			meta.put("multipart", req.isMultipart());
			meta.put("address", req.getAddress());
			Iterator<String> headers = req.getHeaders();
			while (headers.hasNext()) {
				String header = headers.next();
				meta.put(header, req.getHeader(header));
			}
		});
		br.createService(gw);
		br.createService(new Service("test") {

			Action action = ctx -> {
				lastCtx.set(ctx);
				return ctx.params;
			};

		});
		br.createService(new StreamReceiver());
		br.start();

		// Start client
		cl.start();
	}

	@Override
	protected void tearDown() throws Exception {
		if (cl != null) {
			cl.stop();
		}
		if (br != null) {
			br.stop();
		}
	}

	protected Context reset() {
		return lastCtx.getAndSet(null);
	}

	protected void assertOk(Tree rsp) {
		assertEquals(200, rsp.get("_meta.$status", 0));
	}

	protected void assertRestResponse(Tree rsp) {
		Tree meta = rsp.getMeta();
		Tree headers = meta.get("$headers");
		assertTrue(headers.get("Cache-Control", "").contains("no-cache"));
		assertTrue(headers.get("Content-Type", "").contains("application/json"));
	}

	protected static class StreamReceiver extends Service {

		public ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		public AtomicBoolean closed = new AtomicBoolean();
		public AtomicReference<Throwable> error = new AtomicReference<>();

		public Action receive = ctx -> {
			Promise res = new Promise();
			if (ctx.stream == null) {
				res.complete(new IllegalArgumentException("missing stream"));
			} else {
				ctx.stream.onPacket((bytes, error, close) -> {
					if (bytes != null) {
						this.buffer.write(bytes);
					}
					if (error != null) {
						this.error.set(error);
					}
					if (close) {
						this.closed.set(true);
						res.complete();
					}
				});
			}
			return res;
		};

	}

	// ---------------- TESTS ----------------

	@Test
	public void testHttpClientAPI() throws Exception {

		Tree req = new Tree().put("a", 1).put("b", true).put("c", "d");
		Tree rsp = cl.get(TEST_URL, req, params -> {
			params.returnHttpHeaders().returnStatusCode();
		}).waitFor();

		Context ctx = reset();
		assertOk(rsp);
		assertRestResponse(rsp);
		assertFalse(rsp.isEmpty());
		assertFalse(ctx.params.getMeta().isEmpty());
		assertEquals("GET", ctx.params.getMeta().get("method", ""));

		rsp = cl.post(TEST_URL, req, params -> {
			params.returnHttpHeaders().returnStatusCode();
		}).waitFor();

		ctx = reset();
		assertOk(rsp);
		assertRestResponse(rsp);

		assertEquals("POST", ctx.params.getMeta().get("method", ""));
		assertEquals(1, rsp.get("a", 0));
		assertEquals(true, rsp.get("b", false));
		assertEquals("d", rsp.get("c", ""));
		assertEquals(1, ctx.params.get("a", 0));
		assertEquals(true, ctx.params.get("b", false));
		assertEquals("d", ctx.params.get("c", ""));
		assertTrue(rsp.get("_meta.$headers.Content-Length", 0) > 2);

		Tree[] arr = new Tree[1];
		boolean[] con = new boolean[1];

		// Connect via WebSocket
		WebSocketConnection ws = cl.ws("http://127.0.0.1:8080/ws/test", new WebSocketHandler() {

			@Override
			public void onMessage(Tree message) {
				System.out.println("WebSocket message received: " + message);
				arr[0] = message;
			}

			@Override
			public void onOpen(WebSocket webSocket) {
				con[0] = true;
				System.out.println("Connected.");
			}

			@Override
			public void onError(Throwable t) {
				con[0] = false;
			}

			@Override
			public void onClose(WebSocket webSocket, int code, String reason) {
				con[0] = false;
				System.out.println("Disconnected");
			}

		}, params -> {

			params.setHeartbeatInterval(30);
			params.setHeartbeatTimeout(10);
			params.setReconnectDelay(5);

			params.setHeader("CustomHeader", "CustomValue");

		});
		ws.waitForConnection(20, TimeUnit.SECONDS);
		assertTrue(con[0]);

		// Send a test WebSocket packet (server -> client)
		Tree packet = new Tree();
		packet.put("path", "ws/test");
		packet.putMap("data").put("a", 123).put("b", 456);

		br.broadcast("websocket.send", packet);
		Thread.sleep(1000);

		Tree check = arr[0];
		assertEquals(123, check.get("a", 0));
		assertEquals(456, check.get("b", 0));

		ws.disconnect().waitFor(2000);
		assertFalse(con[0]);

		// Stream test
		StreamReceiver receiver = (StreamReceiver) br.getLocalService("streamReceiver");
		PacketStream sender = br.createStream();

		cl.post(RECEIVER_URL, sender);

		sender.sendData("123".getBytes());
		Thread.sleep(1000);
		assertEquals("123", new String(receiver.buffer.toByteArray()));
		assertFalse(receiver.closed.get());
		assertNull(receiver.error.get());

		sender.sendData("456".getBytes());
		Thread.sleep(1000);
		assertEquals("123456", new String(receiver.buffer.toByteArray()));
		assertFalse(receiver.closed.get());
		assertNull(receiver.error.get());

		sender.sendData("789".getBytes());
		Thread.sleep(1000);
		assertEquals("123456789", new String(receiver.buffer.toByteArray()));
		assertFalse(receiver.closed.get());
		assertNull(receiver.error.get());

		sender.sendClose();
		Thread.sleep(1000);
		assertEquals("123456789", new String(receiver.buffer.toByteArray()));
		assertTrue(receiver.closed.get());
		assertNull(receiver.error.get());

		receiver.buffer.reset();
		receiver.closed.set(false);

		Consumer<RequestParams> returnAll = new Consumer<RequestParams>() {

			@Override
			public void accept(RequestParams params) {
				params.returnStatusCode().returnHttpHeaders();
			}

		};

		// GET
		check("GET", null, false, cl.get(TEST_URL));
		check("GET", req, false, cl.get(TEST_URL, req));
		check("GET", null, true, cl.get(TEST_URL, returnAll));
		check("GET", req, true, cl.get(TEST_URL, req, returnAll));

		// CONNECT
		check("CONNECT", null, false, cl.connect(TEST_URL));
		check("CONNECT", null, true, cl.connect(TEST_URL, returnAll));

		// OPTIONS
		check("OPTIONS", null, false, cl.options(TEST_URL));
		check("OPTIONS", req, false, cl.options(TEST_URL, req));
		check("OPTIONS", null, true, cl.options(TEST_URL, returnAll));
		check("OPTIONS", req, true, cl.options(TEST_URL, req, returnAll));

		// HEAD
		check("HEAD", null, false, cl.head(TEST_URL));
		check("HEAD", null, true, cl.head(TEST_URL, returnAll));

		// POST
		check("POST", null, false, cl.post(TEST_URL));
		check("POST", req, false, cl.post(TEST_URL, req));
		check("POST", null, true, cl.post(TEST_URL, returnAll));
		check("POST", req, true, cl.post(TEST_URL, req, returnAll));

		// PUT
		check("PUT", null, false, cl.put(TEST_URL));
		check("PUT", req, false, cl.put(TEST_URL, req));
		check("PUT", null, true, cl.put(TEST_URL, returnAll));
		check("PUT", req, true, cl.put(TEST_URL, req, returnAll));

		// DELETE
		check("DELETE", null, false, cl.delete(TEST_URL));
		check("DELETE", req, false, cl.delete(TEST_URL, req));
		check("DELETE", null, true, cl.delete(TEST_URL, returnAll));
		check("DELETE", req, true, cl.delete(TEST_URL, req, returnAll));

		// PATCH
		check("PATCH", null, false, cl.patch(TEST_URL));
		check("PATCH", req, false, cl.patch(TEST_URL, req));
		check("PATCH", null, true, cl.patch(TEST_URL, returnAll));
		check("PATCH", req, true, cl.patch(TEST_URL, req, returnAll));

		// TRACE
		check("TRACE", null, false, cl.trace(TEST_URL));
		check("TRACE", req, false, cl.trace(TEST_URL, req));
		check("TRACE", null, true, cl.trace(TEST_URL, returnAll));
		check("TRACE", req, true, cl.trace(TEST_URL, req, returnAll));

	}

	private void check(String method, Tree request, boolean returnAll, Promise responsePromise) throws Exception {
		long start = System.currentTimeMillis();
		System.out.println("Testing " + method + " method...");
		Tree rsp = responsePromise.waitFor(1000);
		long duration = System.currentTimeMillis() - start;
		assertTrue(duration < 100);
		if ("CONNECT".equals(method)) {
			assertEquals("SERVICE_NOT_FOUND_ERROR", rsp.get("type", ""));
			return;
		}
		Context ctx = reset();
		if (request != null) {
			String s = request.toString(null, false, false).replace("\"", "");
			assertEquals(s, rsp.toString(null, false, false).replace("\"", ""));
			assertEquals(s, ctx.params.toString(null, false, false).replace("\"", ""));
		}
		if (returnAll) {
			assertOk(rsp);
			assertRestResponse(rsp);
		} else {
			assertNull(rsp.getMeta(false));
		}
		if (request == null) {
			assertTrue(rsp.isEmpty());
		} else {
			assertFalse(rsp.isEmpty());
		}
		Tree meta = ctx.params.getMeta();
		assertFalse(meta.isEmpty());
		assertEquals(method, meta.get("method", ""));
		assertTrue(meta.get("path", "").startsWith("/test.action"));
		if (request != null && ("GET".equals(method) || "CONNECT".equals(method) || "HEAD".equals(method)
				|| "OPTIONS".equals(method) || "TRACE".equals(method))) {
			assertFalse(meta.get("query", "").isEmpty());
		} else {
			assertNull(meta.get("query", (String) null));
		}
		assertFalse(meta.get("multipart", true));
		assertFalse(meta.get("address", "").isEmpty());
	}

}