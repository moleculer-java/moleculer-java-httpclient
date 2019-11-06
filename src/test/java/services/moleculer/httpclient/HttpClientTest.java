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

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.junit.Test;

import io.datatree.Tree;
import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.context.Context;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.service.Action;
import services.moleculer.service.Service;
import services.moleculer.web.ApiGateway;
import services.moleculer.web.netty.NettyServer;

public class HttpClientTest extends TestCase {

	// --- CONSTANTS ---

	private static final String URL = "http://127.0.0.1:8080/test.action";

	// --- VARIABLES ---

	AtomicReference<Context> lastCtx = new AtomicReference<>();
	HttpClient cl = new HttpClient();
	ServiceBroker br;

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
		gw.setBeforeCall((route, req, rsp, data) -> {
			if (req == null || data == null) {
				return;
			}
			Tree meta = data.getMeta();
			meta.put("method", req.getMethod());
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

	// ---------------- TESTS ----------------

	@Test
	public void testHttpClientAPI() throws Exception {

		Tree req = new Tree().put("a", 1).put("b", true).put("c", "d");
		Tree rsp = cl.rest(URL).waitFor();
		Context ctx = reset();
		
		assertOk(rsp);
		assertRestResponse(rsp);
		assertTrue(rsp.isEmpty());
		assertFalse(ctx.params.getMeta().isEmpty());
		assertEquals("GET", ctx.params.getMeta().get("method", ""));
		
		rsp = cl.rest(URL, req).waitFor();
		ctx = reset();
		
		assertOk(rsp);
		assertRestResponse(rsp);
		System.out.println(ctx.params);
		// assertEquals("POST", ctx.params.getMeta().get("method", ""));
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
		WebSocketConnection ws = cl.ws("http://127.0.0.1:8080/ws/test");
		ws.addMessageListener(msg -> {
			System.out.println("WebSocket message received: " + msg);
			arr[0] = msg;
		});
		ws.addWebSocketListener(new WebSocketListener() {
			
			@Override
			public void onOpen(WebSocket websocket) {
				con[0] = true;
				System.out.println("Connected.");
			}
			
			@Override
			public void onError(Throwable t) {
				con[0] = false;
			}
			
			@Override
			public void onClose(WebSocket websocket, int code, String reason) {
				con[0] = false;
				System.out.println("Disconnected");
			}
			
		});
		ws.connect().waitFor(1000);
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
		
		ws.disconnect().waitFor(1000);
		assertFalse(con[0]);
	}

}