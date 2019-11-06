package services.moleculer.httpclient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Promise;
import io.datatree.Tree;

public class WebSocketConnection extends RequestSetter {

	// --- LOGGER ---

	protected static final Logger logger = LoggerFactory.getLogger(WebSocketConnection.class);

	// --- PROPERTIES ---

	protected final String url;
	protected final AsyncHttpClient client;
	protected final ScheduledExecutorService scheduler;

	protected final AtomicReference<WebSocket> webSocket = new AtomicReference<>();
	protected final AtomicReference<ScheduledFuture<?>> timer = new AtomicReference<>();

	protected final AtomicBoolean closed = new AtomicBoolean();

	protected final ConnectionListener connectionListener;

	// --- LISTENERS ---

	protected final HashMap<Consumer<Tree>, WebSocketListener> messageListeners = new HashMap<>();
	protected final HashSet<WebSocketListener> webSocketListeners = new HashSet<>();

	// --- VARIABLES OF THE TIMEOUT HANDLER ---

	protected int heartbeatInterval = 30;
	protected int heartbeatTimeout = 10;

	protected final AtomicLong submittedAt = new AtomicLong();
	protected final AtomicLong receivedAt = new AtomicLong();

	protected final AtomicReference<Promise> connectPromise = new AtomicReference<>();
	protected final AtomicReference<Promise> disconnectPromise = new AtomicReference<>();
	
	// --- CONSTRUCTOR ---

	protected WebSocketConnection(AsyncHttpClient asyncHttpClient, String url, ScheduledExecutorService scheduler) {
		super(asyncHttpClient.prepareGet(url));
		this.url = url;
		this.client = asyncHttpClient;
		this.scheduler = scheduler;
		this.connectionListener = new ConnectionListener(this);
	}

	// --- ADD / REMOVE LISTENER ---

	public WebSocketConnection addWebSocketListener(WebSocketListener listener) {
		checkConnection();
		webSocketListeners.add(listener);
		return this;
	}

	public WebSocketConnection removeWebSocketListener(WebSocketListener listener) {
		checkConnection();
		webSocketListeners.remove(listener);
		return this;
	}

	public WebSocketConnection addMessageListener(Consumer<Tree> listener) {
		checkConnection();
		if (!messageListeners.containsKey(listener)) {
			messageListeners.put(listener, new WebSocketListener() {

				StringBuilder buffer = new StringBuilder();

				@Override
				public void onOpen(WebSocket websocket) {
				}

				@Override
				public void onError(Throwable t) {
				}

				@Override
				public void onClose(WebSocket websocket, int code, String reason) {
				}

				@Override
				public void onTextFrame(String payload, boolean finalFragment, int rsv) {
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
							try {
								if (c == '{' || c == '[') {
									data = new Tree(content);
								}
							} catch (Exception cause) {
								logger.warn("Unable to parse JSON!", cause);
							}
							if (data == null) {
								data = new Tree();
								data.put("data", content);
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
		Promise p = new Promise();
		Promise o = connectPromise.getAndSet(p);
		if (o != null) {
			o.complete(new InterruptedException());
		}
		openConnection();
		return p;
	}

	protected void openConnection() {
		logger.info("Connecting to " + url + "...");
		WebSocketUpgradeHandler.Builder upgradeHandlerBuilder = new WebSocketUpgradeHandler.Builder();
		for (WebSocketListener listener : webSocketListeners) {
			upgradeHandlerBuilder.addWebSocketListener(listener);
		}
		upgradeHandlerBuilder.addWebSocketListener(connectionListener);
		for (WebSocketListener listener : messageListeners.values()) {
			upgradeHandlerBuilder.addWebSocketListener(listener);
		}		
		builder.execute(upgradeHandlerBuilder.build());
	}

	// --- DICONNECT ---

	public Promise disconnect() {
		closed.set(true);
		Promise p = new Promise();
		Promise o = disconnectPromise.getAndSet(p);
		if (o != null) {
			o.complete(new InterruptedException());
		}
		closeConnection(webSocket.getAndSet(null));
		return p;
	}

	protected void closeConnection(WebSocket ws) {
		try {
			ScheduledFuture<?> future = timer.getAndSet(null);
			if (future != null) {
				future.cancel(false);
			}
		} catch (Throwable ignored) {
		}
		if (ws != null) {
			try {
				ws.sendCloseFrame();
			} catch (Throwable ignored) {
			}
		}
		Promise o = connectPromise.getAndSet(null);
		if (o != null) {
			o.complete(new InterruptedException());
		}
	}

	@Override
	protected void finalize() throws Throwable {
		disconnect();
	}

	// --- RECONNECT ---

	protected void reconnect() {
		closeConnection(webSocket.getAndSet(null));
		if (closed.get()) {
			return;
		}
		timer.getAndSet(scheduler.schedule(this::openConnection, 3, TimeUnit.SECONDS));
	}

	// --- GETTERS / SETTERS ---
	
	public int getHeartbeatInterval() {
		return heartbeatInterval;
	}

	public void setHeartbeatInterval(int heartbeatInterval) {
		this.heartbeatInterval = heartbeatInterval;
	}

	public int getHeartbeatTimeout() {
		return heartbeatTimeout;
	}

	public void setHeartbeatTimeout(int heartbeatTimeout) {
		this.heartbeatTimeout = heartbeatTimeout;
	}
	
	// --- HEARTBEAT HANDLING ---

	protected void checkTimeout() {
		long now = System.currentTimeMillis();
		if (now - submittedAt.get() > heartbeatInterval * 1000L) {
			WebSocket ws = webSocket.get();
			if (ws != null) {
				submittedAt.set(now);
				ws.sendTextFrame("!");
			}
			return;
		}
		long sa = submittedAt.get();
		long ht = heartbeatTimeout * 1000L;
		if ((sa - receivedAt.get()) >= ht && (now - sa) >= ht) {
			logger.warn("Heartbeat response message timeouted. Reconnecting...");
			reconnect();
		}
	}

	protected static class ConnectionListener implements WebSocketListener {

		protected WebSocketConnection parent;

		protected ConnectionListener(WebSocketConnection parent) {
			this.parent = parent;
		}

		@Override
		public void onOpen(WebSocket websocket) {
			if (parent.closed.get()) {
				parent.closeConnection(websocket);
				return;
			}
			WebSocket prev = parent.webSocket.getAndSet(websocket);
			if (prev != null) {
				parent.closeConnection(prev);
			}
			parent.timer.getAndSet(parent.scheduler.scheduleAtFixedRate(parent::checkTimeout,
					parent.heartbeatInterval / 3, parent.heartbeatInterval / 3, TimeUnit.SECONDS));
			Promise o = parent.connectPromise.getAndSet(null);
			if (o != null) {
				o.complete();
			}
			logger.info("WebSocket channel opened.");			
		}

		@Override
		public void onClose(WebSocket websocket, int code, String reason) {
			Promise o = parent.disconnectPromise.getAndSet(null);
			if (o != null) {
				o.complete();
			}			
			logger.info("WebSocket channel closed.");			
		}

		@Override
		public void onError(Throwable cause) {
			if (parent.closed.get()) {
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
			Promise o = parent.connectPromise.getAndSet(null);
			if (o != null) {
				o.complete(cause);
			}
			parent.reconnect();
		}

		@Override
		public void onTextFrame(String payload, boolean finalFragment, int rsv) {
			if (parent.closed.get()) {
				return;
			}
			if (finalFragment && payload != null && payload.equals("!")) {
				parent.receivedAt.set(System.currentTimeMillis());
			}
		}

	}

}