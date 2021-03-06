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

import org.asynchttpclient.ws.WebSocket;

import io.datatree.Tree;

/**
 * WebSocket message receiver. Simple usage:
 * 
 * <pre>
 * client.ws("ws://server/path", msg -> {
 * 
 *     // Message received; "msg" is a JSON structure
 *     String value = msg.get("key", "defaultValue");
 * 
 * });
 * </pre>
 * 
 * Advanced usage:
 * 
 * <pre>
 * client.ws("ws://server/path", new WebSocketHandler() {
 * 
 *     public void onMessage(Tree message) {
 *        // Message received
 *     }
 * 
 *     public void onOpen(WebSocket webSocket) {
 *        // WebSocket opened
 *     }
 * 
 *     public void onError(Throwable t) {
 *        // Error occured
 *     }
 * 
 *     public void onClose(WebSocket webSocket, int code, String reason) {
 *        // WebSocket closed
 *     }
 * 
 * });
 * </pre>
 */
@FunctionalInterface
public interface WebSocketHandler {

	void onMessage(Tree message);

	default void onOpen(WebSocket webSocket) {
		
		// Optional code
	}

	default void onError(Throwable t) {

		// Optional code
	}

	default void onClose(WebSocket webSocket, int code, String reason) {
		
		// Optional code		
	}

}