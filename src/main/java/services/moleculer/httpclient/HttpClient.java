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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.SignatureCalculator;

import io.datatree.Promise;
import io.datatree.Tree;
import io.netty.handler.codec.http.HttpHeaders;
import services.moleculer.stream.PacketStream;

/**
 * Promise based HTTP client for Moleculer-Java.
 * 
 * <pre>
 * HttpClient client = new HttpClient();
 * client.start();
 * 
 * Tree req = new Tree();
 * req.put("key1", "value1");
 * 
 * client.post("http://host/path", req).then(rsp -> {
 * 
 * 	// Success
 * 	String value2 = rsp.get("key2", "defaultValue");
 * 
 * }).catchError(err -> {
 * 
 * 	// Failed
 * 	err.printStackTrace();
 * });
 * </pre>
 */
public class HttpClient extends DefaultAsyncHttpClientConfig.Builder {

	// --- VARIABLES ---

	protected DefaultAsyncHttpClient client;

	/**
	 * Default signature calculator to use for all requests constructed by this
	 * client instance.
	 */
	protected SignatureCalculator signatureCalculator;

	/**
	 * Task scheduler.
	 */
	protected ScheduledExecutorService scheduler;

	/**
	 * Shut down ScheduledExecutorService on stop().
	 */
	protected boolean shutDownThreadPools;

	// --- INIT HTTP CLIENT ---

	public void start() {
		client = new DefaultAsyncHttpClient(build());
	}

	// --- CLOSE RESOURCES ---

	public void stop() throws Exception {
		closeResources();
	}

	@Override
	protected void finalize() throws Throwable {
		closeResources();
	}

	protected void closeResources() {
		if (client != null) {
			try {
				client.close();
			} catch (Exception ignored) {
			}
			client = null;
		}
		if (shutDownThreadPools && scheduler != null) {
			scheduler.shutdownNow();
			scheduler = null;
		}
	}

	protected ScheduledExecutorService getScheduler() {
		if (scheduler == null) {
			scheduler = client.getEventLoopGroup();
			if (scheduler == null) {
				scheduler = Executors.newSingleThreadScheduledExecutor();
				shutDownThreadPools = true;
			}
		}
		return scheduler;
	}
	
	// --- SIMPLIFIED HTTP METHODS ---

	/**
	 * Executes an HTTP GET request.
	 *
	 * @param url
	 *            A well formed URL.
	 * 
	 * @return {@link Promise}
	 */
	public Promise get(String url) {
		return get(url, null, null);
	}

	/**
	 * Executes an HTTP GET request.
	 *
	 * @param url
	 *            A well formed URL.
	 * @param request
	 *            Request parameters in a Tree
	 * 
	 * @return {@link Promise}
	 */
	public Promise get(String url, Tree params) {
		return get(url, params, null);
	}

	/**
	 * Executes an HTTP GET request.
	 *
	 * @param url
	 *            A well formed URL.
	 * @param request
	 *            Request parameters in a Tree
	 * @param configurator
	 *            Consumer for set the parameters of the request
	 * 
	 * @return {@link Promise}
	 */
	public Promise get(String url, Tree request, Consumer<RequestParams> configurator) {
		return execute(url, "GET", new TreeConfigurator(configurator, request, false));
	}

	/**
	 * Executes an HTTP CONNECT request.
	 *
	 * @param url
	 *            A well formed URL.
	 * 
	 * @return {@link Promise}
	 */
	public Promise connect(String url) {
		return connect(url, null);
	}

	/**
	 * Executes an HTTP CONNECT request.
	 *
	 * @param url
	 *            A well formed URL.
	 * @param configurator
	 *            Consumer for set the parameters of the request
	 * 
	 * @return {@link Promise}
	 */
	public Promise connect(String url, Consumer<RequestParams> configurator) {
		return execute(url, "CONNECT", configurator);
	}

	/**
	 * Executes an HTTP OPTIONS request.
	 *
	 * @param url
	 *            A well formed URL.
	 * 
	 * @return {@link Promise}
	 */
	public Promise options(String url) {
		return options(url, null);
	}

	/**
	 * Executes an HTTP OPTIONS request.
	 *
	 * @param url
	 *            A well formed URL.
	 * @param configurator
	 *            Consumer for set the parameters of the request
	 * 
	 * @return {@link Promise}
	 */
	public Promise options(String url, Consumer<RequestParams> configurator) {
		return execute(url, "OPTIONS", configurator);
	}

	/**
	 * Executes an HTTP HEAD request.
	 *
	 * @param url
	 *            A well formed URL.
	 * 
	 * @return {@link Promise}
	 */
	public Promise head(String url) {
		return head(url, null);
	}

	/**
	 * Executes an HTTP HEAD request.
	 *
	 * @param url
	 *            A well formed URL.
	 * @param configurator
	 *            Consumer for set the parameters of the request
	 * 
	 * @return {@link Promise}
	 */
	public Promise head(String url, Consumer<RequestParams> configurator) {
		return execute(url, "HEAD", configurator);
	}

	/**
	 * Executes an HTTP POST request.
	 *
	 * @param url
	 *            A well formed URL.
	 * 
	 * @return {@link Promise}
	 */
	public Promise post(String url) {
		return post(url, (Tree) null, null);
	}

	/**
	 * Executes an HTTP POST request.
	 *
	 * @param url
	 *            A well formed URL.
	 * @param request
	 *            Request body in JSON format
	 * 
	 * @return {@link Promise}
	 */
	public Promise post(String url, Tree request) {
		return post(url, request, null);
	}

	/**
	 * Executes an HTTP POST request.
	 *
	 * @param url
	 *            A well formed URL.
	 * @param request
	 *            Request body as PacketStream
	 * 
	 * @return {@link Promise}
	 */
	public Promise post(String url, PacketStream request) {
		return post(url, request, null);
	}

	/**
	 * Executes an HTTP POST request.
	 *
	 * @param url
	 *            A well formed URL.
	 * @param configurator
	 *            Consumer for set the parameters of the request
	 * 
	 * @return {@link Promise}
	 */
	public Promise post(String url, Consumer<RequestParams> params) {
		return post(url, (Tree) null, params);
	}

	/**
	 * Executes an HTTP POST request.
	 *
	 * @param url
	 *            A well formed URL.
	 * @param request
	 *            Request body in JSON format
	 * @param configurator
	 *            Consumer for set the parameters of the request
	 * 
	 * @return {@link Promise}
	 */
	public Promise post(String url, Tree request, Consumer<RequestParams> configurator) {
		return execute(url, "POST", new TreeConfigurator(configurator, request, true));
	}

	/**
	 * Executes an HTTP POST request.
	 *
	 * @param url
	 *            A well formed URL.
	 * @param request
	 *            Request body as PacketStream
	 * @param configurator
	 *            Consumer for set the parameters of the request
	 * 
	 * @return {@link Promise}
	 */
	public Promise post(String url, PacketStream request, Consumer<RequestParams> configurator) {
		return execute(url, "POST", new PacketStreamConfigurator(configurator, request));
	}

	/**
	 * Executes an HTTP PUT request.
	 *
	 * @param url
	 *            A well formed URL.
	 * 
	 * @return {@link Promise}
	 */
	public Promise put(String url) {
		return put(url, null);
	}

	/**
	 * Executes an HTTP PUT request.
	 *
	 * @param url
	 *            A well formed URL.
	 * @param configurator
	 *            Consumer for set the parameters of the request
	 * 
	 * @return {@link Promise}
	 */
	public Promise put(String url, Consumer<RequestParams> configurator) {
		return execute(url, "PUT", configurator);
	}

	/**
	 * Executes an HTTP DELETE request.
	 *
	 * @param url
	 *            A well formed URL.
	 * 
	 * @return {@link Promise}
	 */
	public Promise delete(String url) {
		return delete(url, null);
	}

	/**
	 * Executes an HTTP DELETE request.
	 *
	 * @param url
	 *            A well formed URL.
	 * @param configurator
	 *            Consumer for set the parameters of the request
	 * 
	 * @return {@link Promise}
	 */
	public Promise delete(String url, Consumer<RequestParams> configurator) {
		return execute(url, "DELETE", configurator);
	}

	/**
	 * Executes an HTTP PATCH request.
	 *
	 * @param url
	 *            A well formed URL.
	 * 
	 * @return {@link Promise}
	 */
	public Promise patch(String url) {
		return patch(url, null);
	}

	/**
	 * Executes an HTTP PATCH request.
	 *
	 * @param url
	 *            A well formed URL.
	 * @param configurator
	 *            Consumer for set the parameters of the request
	 * 
	 * @return {@link Promise}
	 */
	public Promise patch(String url, Consumer<RequestParams> configurator) {
		return execute(url, "PATCH", configurator);
	}

	/**
	 * Executes an HTTP TRACE request.
	 *
	 * @param url
	 *            A well formed URL.
	 * 
	 * @return {@link Promise}
	 */
	public Promise trace(String url) {
		return trace(url, null);
	}

	/**
	 * Executes an HTTP TRACE request.
	 *
	 * @param url
	 *            A well formed URL.
	 * @param configurator
	 *            Consumer for set the parameters of the request
	 * 
	 * @return {@link Promise}
	 */
	public Promise trace(String url, Consumer<RequestParams> configurator) {
		return execute(url, "TRACE", configurator);
	}

	// --- WEBSOCKET LISTENER / RECEIVER ---

	public WebSocketConnection ws(String url) {
		String wsUrl = url.startsWith("http") ? "ws" + url.substring(4) : url;
		return new WebSocketConnection(this, wsUrl);
	}

	// --- COMMON HTTP-METHOD EXECUTOR ---

	protected Promise execute(String url, String method, Consumer<RequestParams> configurator) {
		RequestParams params = new RequestParams(method,
				client.getConfig().isDisableUrlEncodingForBoundRequests());
		params.setUrl(url);
		if (signatureCalculator != null) {
			params.setSignatureCalculator(signatureCalculator);
		}
		if (configurator != null) {
			configurator.accept(params);
		}
		return new Promise(res -> {
			client.executeRequest(params.build(), new AsyncHandler<Void>() {

				@Override
				public State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
					if (params.handler == null) {
						return State.CONTINUE;
					}
					return params.handler.onStatusReceived(responseStatus);
				}

				@Override
				public State onHeadersReceived(HttpHeaders headers) throws Exception {
					if (params.handler == null) {
						return State.CONTINUE;
					}
					return params.handler.onHeadersReceived(headers);
				}

				@Override
				public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
					if (params.handler == null) {
						return State.CONTINUE;
					}
					return params.handler.onBodyPartReceived(bodyPart);
				}

				@Override
				public void onThrowable(Throwable error) {
					try {
						if (params.handler != null) {
							params.handler.onThrowable(error);
						}
					} finally {
						res.reject(error);
					}
				}

				@Override
				public Void onCompleted() throws Exception {
					Object result = null;
					try {
						if (params.handler != null) {
							result = params.handler.onCompleted();
						}
					} catch (Throwable error) {
						res.reject(error);
					} finally {
						res.resolve(result);
					}
					return null;
				}

			});
		});
	}

	// --- SETTERS ---

	public void setSignatureCalculator(SignatureCalculator signatureCalculator) {
		this.signatureCalculator = signatureCalculator;
	}

}