package services.moleculer.httpclient;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;

import io.datatree.Tree;

public class WebSocketHandler extends RequestSetter {

	// --- VARIABLES ---

	protected final AsyncHttpClient client;
	protected final ScheduledExecutorService scheduler;
	protected final Consumer<Tree> listener;
	protected final int reconnectDelay = 5;

	protected final AtomicReference<WebSocket> webSocket = new AtomicReference<>();
	protected final AtomicReference<ScheduledFuture<?>> timer = new AtomicReference<>();

	protected final AtomicBoolean closed = new AtomicBoolean();

	// --- CONSTRUCTOR ---

	protected WebSocketHandler(AsyncHttpClient asyncHttpClient, String url, ScheduledExecutorService scheduler,
			Consumer<Tree> listener) {
		super(asyncHttpClient.prepareGet(url));
		this.client = asyncHttpClient;
		this.scheduler = scheduler;
		this.listener = listener;
	}

	// --- CONNECT ---

	public void connect() {
		closeConnection(webSocket.getAndSet(null));
		closed.set(false);
		openConnection();
	}

	protected void openConnection() {
		WebSocketUpgradeHandler.Builder upgradeHandlerBuilder = new WebSocketUpgradeHandler.Builder();
		upgradeHandlerBuilder.addWebSocketListener(new WebSocketListener() {

			StringBuilder buffer = new StringBuilder();

			@Override
			public void onOpen(WebSocket websocket) {
				WebSocket prev = webSocket.getAndSet(websocket);
				if (prev != null) {
					closeConnection(prev);
				}
			}

			@Override
			public void onError(Throwable t) {
				if (!closed.get()) {
					reconnect();
				}
			}

			@Override
			public void onClose(WebSocket websocket, int code, String reason) {
			}

			@Override
			public void onTextFrame(String payload, boolean finalFragment, int rsv) {
				if (payload != null) {
					buffer.append(payload);
				}
				if (finalFragment) {
					String content = buffer.toString();
					buffer.setLength(0);
					Tree data = null;
					if (content.length() > 0) {
						char c = content.charAt(0);
						try {
							if (c == '{' || c == '[') {
								data = new Tree(content);
							}
						} catch (Exception cause) {
							// TODO Log "Unable to parse"...
						}
						if (data == null) {
							data = new Tree();
							data.put("data", content);
						}
					}
				}
			}

		});
		WebSocketUpgradeHandler upgradeHandler = upgradeHandlerBuilder.build();
		builder.execute(upgradeHandler);
	}

	// --- DICONNECT ---

	public void disconnect() {
		closed.set(true);
		closeConnection(webSocket.getAndSet(null));
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
	}

	// --- RECONNECT ---

	protected void reconnect() {
		closeConnection(webSocket.getAndSet(null));
		if (!closed.get()) {
			timer.getAndSet(scheduler.schedule(this::openConnection, reconnectDelay, TimeUnit.SECONDS));
		}
	}

}