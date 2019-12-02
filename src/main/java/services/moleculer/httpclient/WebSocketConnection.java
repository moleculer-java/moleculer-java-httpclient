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

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.util.CheckedTree;

public class WebSocketConnection {

	// --- LOGGER ---

	protected static final Logger logger = LoggerFactory.getLogger(WebSocketConnection.class);

	// --- VARIABLES ---

	protected final HttpClient httpClient;
	protected final String url;

	protected final AtomicReference<WebSocket> webSocket = new AtomicReference<>();
	protected final AtomicReference<ScheduledFuture<?>> reconnectTimer = new AtomicReference<>();

	protected final AtomicReference<Promise> connected = new AtomicReference<>();
	protected final AtomicReference<Promise> disconnected = new AtomicReference<>();
	protected final AtomicBoolean closed = new AtomicBoolean();

	protected final AtomicLong submittedAt = new AtomicLong();
	protected final AtomicLong receivedAt = new AtomicLong();

	protected int heartbeatInterval = 30;
	protected int heartbeatTimeout = 10;

	// --- LISTENERS ---

	protected final HashMap<Consumer<Tree>, WebSocketListener> messageListeners = new HashMap<>();
	protected final HashSet<WebSocketListener> connectionListeners = new HashSet<>();

	// --- CONSTRUCTOR ---

	protected WebSocketConnection(HttpClient httpClient, String url) {
		this.httpClient = httpClient;
		this.url = url;
	}

	// --- ADD / REMOVE LISTENER ---

	public WebSocketConnection addWebSocketListener(WebSocketListener listener) {
		checkConnection();
		connectionListeners.add(listener);
		return this;
	}

	public WebSocketConnection removeWebSocketListener(WebSocketListener listener) {
		checkConnection();
		connectionListeners.remove(listener);
		return this;
	}

	public WebSocketConnection addMessageListener(Consumer<Tree> listener) {
		checkConnection();
		if (!messageListeners.containsKey(listener)) {
			messageListeners.put(listener, new WebSocketListener() {

				StringBuilder buffer = new StringBuilder();

				@Override
				public final void onOpen(WebSocket websocket) {

					// Do nothing
				}

				@Override
				public final void onError(Throwable t) {

					// Do nothing
				}

				@Override
				public final void onClose(WebSocket websocket, int code, String reason) {

					// Do nothing
				}

				@Override
				public final void onTextFrame(String payload, boolean finalFragment, int rsv) {
					if (closed.get()) {
						return;
					}
					if (payload != null) {
						buffer.append(payload);
					}
					if (finalFragment) {
						String content = buffer.toString();
						buffer.setLength(0);
						Tree data = null;
						if (content.length() > 0) {
							char c = content.charAt(0);
							if (c == '!') {
								return;
							}
							if (c == '{' || c == '[') {
								try {
									data = new Tree(content);
								} catch (Exception cause) {
									logger.warn("Unable to parse JSON!", cause);
								}
							}
							if (data == null) {
								data = new CheckedTree(content);
							}
							listener.accept(data);
						}
					}
				}

			});
		}
		return this;
	}

	public WebSocketConnection removeMessageListener(Consumer<Tree> listener) {
		checkConnection();
		messageListeners.remove(listener);
		return this;
	}

	protected void checkConnection() {
		if (webSocket.get() != null) {
			throw new IllegalStateException("The connection has already been opened!");
		}
	}

	// --- CONNECT ---

	public Promise connect() {
		closed.set(false);
		closeConnection(webSocket.getAndSet(null));
		Promise current = new Promise();
		Promise previous = connected.getAndSet(current);
		if (previous != null) {
			previous.complete(new InterruptedException());
		}
		openConnection();
		return current;
	}

	protected void openConnection() {
		logger.info("Connecting to " + url + "...");
		WebSocketUpgradeHandler.Builder upgradeHandlerBuilder = new WebSocketUpgradeHandler.Builder();
		for (WebSocketListener listener : connectionListeners) {
			upgradeHandlerBuilder.addWebSocketListener(listener);
		}
		for (WebSocketListener listener : messageListeners.values()) {
			upgradeHandlerBuilder.addWebSocketListener(listener);
		}
		upgradeHandlerBuilder.addWebSocketListener(new WebSocketListener() {

			@Override
			public final void onOpen(WebSocket websocket) {
				if (closed.get()) {
					closeConnection(websocket);
					return;
				}
				WebSocket previousSocket = webSocket.getAndSet(websocket);
				if (previousSocket != null) {
					closeConnection(previousSocket);
				}
				reconnectTimer.set(httpClient.getScheduler().scheduleAtFixedRate(() -> {

					long now = System.currentTimeMillis();
					if (now - submittedAt.get() > heartbeatInterval * 1000L) {
						WebSocket ws = webSocket.get();
						if (ws != null) {
							submittedAt.set(now);
							ws.sendTextFrame("!");
						}
						return;
					}
					long heartbeatTimeoutSec = heartbeatTimeout * 1000L;
					long submitted = submittedAt.get();
					if ((submitted - receivedAt.get()) >= heartbeatTimeoutSec
							&& (now - submitted) >= heartbeatTimeoutSec) {
						logger.warn("Heartbeat response message timeouted. Reconnecting...");
						reconnect();
					}

				}, heartbeatInterval / 3, heartbeatInterval / 3, TimeUnit.SECONDS));
				Promise previousPromise = connected.getAndSet(null);
				if (previousPromise != null) {
					previousPromise.complete();
				}
				logger.info("WebSocket channel opened.");
			}

			@Override
			public final void onError(Throwable cause) {
				if (closed.get()) {
					return;
				}
				if (cause != null) {
					Throwable t = cause.getCause();
					if (t != null && t instanceof IllegalStateException) {

						// Scheduler interrupted
						return;
					}
				}
				String msg = cause.getMessage();
				if (msg == null || msg.isEmpty()) {
					msg = "Unexpected error occured!";
				}
				logger.error(msg, cause);
				Promise previousPromise = connected.getAndSet(null);
				if (previousPromise != null) {
					previousPromise.complete(cause);
				}
				reconnect();
			}

			@Override
			public final void onClose(WebSocket websocket, int code, String reason) {
				Promise previousPromise = disconnected.getAndSet(null);
				if (previousPromise != null) {
					previousPromise.complete();
				}
				logger.info("WebSocket channel closed.");
			}

			@Override
			public final void onTextFrame(String payload, boolean finalFragment, int rsv) {
				if (closed.get()) {
					return;
				}
				if (finalFragment && payload != null && payload.equals("!")) {
					receivedAt.set(System.currentTimeMillis());
				}
			}

		});
		httpClient.client.prepareGet(url).execute(upgradeHandlerBuilder.build());
	}

	// --- DISCONNECT ---

	public Promise disconnect() {
		closed.set(true);
		Promise currentPromise = new Promise();
		Promise previousPromise = disconnected.getAndSet(currentPromise);
		if (previousPromise != null) {
			previousPromise.complete(new InterruptedException());
		}
		closeConnection(webSocket.getAndSet(null));
		return currentPromise;
	}

	protected void closeConnection(WebSocket targetSocket) {
		try {
			ScheduledFuture<?> future = reconnectTimer.getAndSet(null);
			if (future != null) {
				future.cancel(false);
			}
		} catch (Throwable ignored) {
		}
		if (targetSocket != null) {
			try {
				targetSocket.sendCloseFrame();
			} catch (Throwable ignored) {
			}
		}
		Promise previous = connected.getAndSet(null);
		if (previous != null) {
			previous.complete(new InterruptedException());
		}
	}

	@Override
	protected void finalize() throws Throwable {
		closeConnection(webSocket.getAndSet(null));
	}

	// --- RECONNECT ---

	protected void reconnect() {
		closeConnection(webSocket.getAndSet(null));
		if (!closed.get()) {
			reconnectTimer.set(httpClient.getScheduler().schedule(this::openConnection, 3, TimeUnit.SECONDS));
		}
	}

	// --- SETTERS ---

	public void setHeartbeatInterval(int heartbeatInterval) {
		this.heartbeatInterval = heartbeatInterval;
	}

	public void setHeartbeatTimeout(int heartbeatTimeout) {
		this.heartbeatTimeout = heartbeatTimeout;
	}

}