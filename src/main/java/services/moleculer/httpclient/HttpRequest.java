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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.Param;
import org.asynchttpclient.Request;

import io.datatree.Promise;
import io.datatree.Tree;
import io.netty.handler.codec.http.HttpHeaders;
import services.moleculer.stream.PacketStream;

/**
 * An object handling an HTTP request. The HTTP request must be executed using
 * the "execute" method.
 * 
 * <pre>
 * // Init client: 
 * HttpClient client = new HttpClient();
 * client.start();
 * 
 * // Start HTTP request:
 * Promise p = client.post("http://host/port").addQueryParam("key", "value").execute();
 * 
 * // Blocking syntax:
 * Tree json = p.waitFor();
 * 
 * // Async syntax:
 * p.then(json -&gt; {
 *   int value1 = json.get("key1");
 * });
 * </pre>
 */
public class HttpRequest extends DelegatedRequestBuilder<HttpRequest> {

	// --- INTERNAL CLIENT ---

	protected final AsyncHttpClient client;

	// --- PROPERTIES ---

	/**
	 * Copy HTTP response status into the Meta structure of the response Tree.
	 */
	protected boolean returnStatus;

	/**
	 * Copy HTTP response headers into the Meta structure of the response Tree.
	 */
	protected boolean returnHeaders;

	// --- CONSTRUCTOR ---

	protected HttpRequest(HttpClient httpClient, String method, String url) {
		super(httpClient.client.prepare(method, url));
		this.client = httpClient.client;
		this.returnStatus = httpClient.returnStatus;
		this.returnHeaders = httpClient.returnHeaders;
	}

	// --- BUILDER-STYLE PROPERTY SETTERS ---

	public HttpRequest setReturnStatus(boolean returnStatus) {
		this.returnStatus = returnStatus;
		return this;
	}

	public HttpRequest setReturnHeaders(boolean returnHeaders) {
		this.returnHeaders = returnHeaders;
		return this;
	}

	// --- SET JSON BODY AS TREE ---

	public HttpRequest setBody(Tree data) {
		if (data == null) {
			setBody(new byte[0]);
		} else {
			setBody(data.toBinary());
		}
		return this;
	}

	// --- SET BINARY BODY AS MOLECULER STREAM ---

	public HttpRequest setBody(PacketStream stream) {
		return setBody(stream, -1L);
	}

	public HttpRequest setBody(PacketStream stream, long contentLength) {
		return setBody(new PacketStreamBodyGenerator(stream, contentLength));
	}

	// --- SET QUERY PARAMS BY TREE ---

	public HttpRequest setQueryParams(Tree params) {
		if (params != null) {
			int size = params.size();
			if (size > 0) {
				ArrayList<Param> list = new ArrayList<>(size);
				for (Tree param : params) {
					list.add(new Param(param.getName(), param.asString()));
				}
				setQueryParams(list);
			}
		}
		return this;
	}

	// --- TRANSFER RESPONSE TO FILE ---

	public Promise transferTo(File target) {
		return transferTo(builder.build(), target);	
	}
	
	public Promise transferTo(Request request, File target) {
		try {
			return transferTo(request, new FileOutputStream(target));
		} catch (Throwable err) {
			return Promise.reject(err);
		}
	}

	// --- TRANSFER RESPONSE TO CHANNEL ---

	public Promise transferTo(WritableByteChannel target) {
		return transferTo(builder.build(), target);	
	}
	
	public Promise transferTo(Request request, WritableByteChannel target) {
		return transferTo(request, new OutputStream() {

			@Override
			public final void write(int b) throws IOException {
				target.write(ByteBuffer.wrap(new byte[] { (byte) b }));
			}

			@Override
			public final void write(byte[] b) throws IOException {
				target.write(ByteBuffer.wrap(b));
			}

			@Override
			public final void write(byte[] b, int off, int len) throws IOException {
				target.write(ByteBuffer.wrap(b, off, len));
			}

			@Override
			public final void close() throws IOException {
				target.close();
			}

		});
	}

	// --- TRANSFER RESPONSE TO OUTPUT STREAM ---

	public Promise transferTo(OutputStream target) {
		return transferTo(builder.build(), target);
	}
	
	public Promise transferTo(Request request, OutputStream target) {
		return execute(request, new AsyncTreeHandler() {

			private final AtomicLong transfered = new AtomicLong();
			
			@Override
			public final State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
				ByteBuffer buffer = bodyPart.getBodyByteBuffer();
				int len = buffer.capacity();
				byte[] chunk = new byte[len];
				buffer.get(chunk, 0, len);
				target.write(chunk, 0, len);
				transfered.addAndGet(len);
				return State.CONTINUE;
			}

			@Override
			public final Tree onCompleted() throws Exception {
				closeStream();
				return new Tree().put("transfered", transfered.get());
			}

			@Override
			public final void onThrowable(Throwable t) {
				closeStream();
			}

			private final void closeStream() {
				if (target != null) {
					try {
						target.close();
					} catch (Exception ignored) {
					}
				}
			}

		});
	}

	// --- TRANSFER RESPONSE TO MOLECULER STREAM ---

	public Promise transferTo(PacketStream target) {
		return transferTo(builder.build(), target);
	}

	public Promise transferTo(Request request, PacketStream target) {
		return execute(request, new AsyncTreeHandler() {

			private final AtomicLong transfered = new AtomicLong();
			
			@Override
			public final State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
				ByteBuffer buffer = bodyPart.getBodyByteBuffer();
				int len = buffer.capacity();
				byte[] chunk = new byte[len];
				buffer.get(chunk, 0, len);
				target.sendData(chunk);
				transfered.addAndGet(len);
				return State.CONTINUE;
			}

			@Override
			public final Tree onCompleted() throws Exception {
				closeStream();
				return new Tree().put("transfered", transfered.get());
			}

			@Override
			public final void onThrowable(Throwable t) {
				closeStream();
			}

			private final void closeStream() {
				if (target != null) {
					try {
						target.sendClose();
					} catch (Exception ignored) {
					}
				}
			}

		});
	}

	// --- EXECUTE REQUEST (RECEIVE JSON RESPONSE) ---

	public Promise execute() {
		return execute(builder.build());
	}

	public Promise execute(Request request) {
		return execute(request, new AsyncTreeHandler() {

			private volatile int status = 200;
			private volatile HttpHeaders httpHeaders;
			private byte[] body;

			@Override
			public final State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
				if (returnStatus) {
					status = responseStatus.getStatusCode();
				}
				return State.CONTINUE;
			}

			@Override
			public final State onHeadersReceived(HttpHeaders httpHeaders) throws Exception {
				if (returnHeaders) {
					this.httpHeaders = httpHeaders;
				}
				return State.CONTINUE;
			}

			@Override
			public final State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
				ByteBuffer buffer = bodyPart.getBodyByteBuffer();
				int len = buffer.capacity();
				if (body == null) {
					body = new byte[len];
					buffer.get(body, 0, len);
				} else {
					byte[] expanded = new byte[body.length + len];
					System.arraycopy(body, 0, expanded, 0, body.length);
					buffer.get(expanded, body.length, len);
				}
				return State.CONTINUE;
			}

			@Override
			public final Tree onCompleted() throws Exception {
				Tree rsp;
				if (body == null || body.length == 0) {
					rsp = new Tree();
				} else if (body[0] == '{' || body[0] == '[') {
					rsp = new Tree(body);
				} else {
					rsp = new Tree().put("data", body);
				}
				if (returnStatus || returnHeaders) {
					Tree meta = rsp.getMeta();
					if (returnStatus) {
						meta.put("$status", status);
					}
					if (returnHeaders) {
						Tree headers = meta.putMap("$headers");
						if (httpHeaders != null) {
							Iterator<Entry<CharSequence, CharSequence>> i = httpHeaders.iteratorCharSequence();
							Entry<CharSequence, CharSequence> e;
							while (i.hasNext()) {
								e = i.next();
								headers.put(e.getKey().toString(), e.getValue().toString());
							}
						}
					}
				}
				return rsp;
			}

		});
	}

	// --- EXECUTE REQUEST (WITH A CUSTOM HANDLER) ---

	public Promise execute(AsyncHandler<?> handler) {
		return execute(builder.build(), handler);
	}

	public Promise execute(Request request, AsyncHandler<?> handler) {
		return new Promise(res -> {
			client.executeRequest(request, new AsyncHandler<Object>() {

				@Override
				public final State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
					try {
						return handler.onStatusReceived(responseStatus);
					} catch (Throwable t) {
						res.reject(t);
						throw t;
					}
				}

				@Override
				public final State onHeadersReceived(HttpHeaders headers) throws Exception {
					try {
						return handler.onHeadersReceived(headers);
					} catch (Throwable t) {
						res.reject(t);
						throw t;
					}
				}

				@Override
				public final State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
					try {
						return handler.onBodyPartReceived(bodyPart);
					} catch (Throwable t) {
						res.reject(t);
						throw t;
					}
				}

				@Override
				public final void onThrowable(Throwable t) {
					res.reject(t);
					handler.onThrowable(t);
				}

				@Override
				public final Object onCompleted() throws Exception {
					try {
						Object result = handler.onCompleted();
						res.resolve(result);
						return result;
					} catch (Throwable t) {
						res.reject(t);
						throw t;
					}
				}

			});
		});
	}

}