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

import org.asynchttpclient.RequestBuilderBase;

/**
 * WebSocket message receiver with built-in heartbeat function. Usage:
 * 
 * <pre>
 * client.ws("ws://server/path", msg -> {
 * 
 *     // Message received; "msg" is a JSON structure
 *     String value = msg.get("key", "defaultValue");
 * 
 * }, params -> {
 * 
 *     // Configure connection
 *     params.setHeartbeatInterval(60);
 *     params.setHeader("key", "value");
 * 
 * });
 * </pre>
 */
public class WebSocketParams extends RequestBuilderBase<WebSocketParams> {

	// --- TIMEOUTS (IN SECONDS) ---
	
	protected int heartbeatInterval = 60;
	protected int heartbeatTimeout = 10;
	protected int reconnectDelay = 3;
	
	// --- CONSTRUCTOR ---

	protected WebSocketParams(boolean isDisableUrlEncoding) {
		super("GET", isDisableUrlEncoding);
	}
	
	// --- SETTERS ---

	public WebSocketParams setHeartbeatInterval(int heartbeatInterval) {
		this.heartbeatInterval = heartbeatInterval;
		return this;
	}

	public WebSocketParams setHeartbeatTimeout(int heartbeatTimeout) {
		this.heartbeatTimeout = heartbeatTimeout;
		return this;
	}

	public WebSocketParams setReconnectDelay(int reconnectDelay) {
		this.reconnectDelay = reconnectDelay;
		return this;
	}
	
}