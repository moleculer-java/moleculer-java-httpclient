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

import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Param;

import io.datatree.Promise;
import io.datatree.Tree;
import io.netty.handler.codec.http.HttpHeaders;
import services.moleculer.stream.PacketStream;

/**
 * An object handling an HTTP request. The HTTP request must be executed using
 * the "execute" method.
 * 
 * <pre>
 * HttpClient client = new HttpClient();
 * client.init();
 * Promise p = client.post("http://host/port").addQueryParam("key", "value").execute();
 * Tree json = p.waitFor();
 * </pre>
 */
public class HttpRequest extends RequestSetter {

	// --- VARIABLES ---

	protected final AsyncHttpClient client;

	// --- CONSTRUCTOR ---

	protected HttpRequest(AsyncHttpClient asyncHttpClient, String method, String url) {
		super(asyncHttpClient.prepare(method, url));
		this.client = asyncHttpClient;
	}

	// --- SET JSON BODY AS TREE ---

	public HttpRequest setBody(Tree data) {
		if (data == null) {
			builder.setBody(new byte[0]);
		} else {
			builder.setBody(data.toBinary());
		}
		return this;
	}

	// --- SET FORM PARAMS AS TREE ---

	public HttpRequest setFormParams(Tree data) {
		if (data != null) {
			int size = data.size();
			if (size > 0) {
				ArrayList<Param> list = new ArrayList<>(size);
				for (Tree param : data) {
					list.add(new Param(param.getName(), param.asString()));
				}
				builder.setFormParams(list);
			}
		}
		return this;
	}

	// --- "TRANSFER TO" FUNCTIONS ---

	public Promise transferTo(File target) {
		try {
			return transferTo(new FileOutputStream(target));
		} catch (Throwable err) {
			return Promise.reject(err);
		}
	}

	public Promise transferTo(WritableByteChannel target) {
		return transferTo(new OutputStream() {

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

	public Promise transferTo(OutputStream target) {
		return new Promise(res -> {
			client.executeRequest(builder.build(), new AsyncHandler<Integer>() {

				private int status = 200;

				@Override
				public final State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
					try {
						status = responseStatus.getStatusCode();
					} catch (Throwable err) {
						onThrowable(err);
						return State.ABORT;
					}
					return State.CONTINUE;
				}

				@Override
				public final State onHeadersReceived(HttpHeaders httpHeaders) throws Exception {
					return State.CONTINUE;
				}

				@Override
				public final State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
					try {
						ByteBuffer buffer = bodyPart.getBodyByteBuffer();
						int len = buffer.capacity();
						byte[] chunk = new byte[len];
						buffer.get(chunk, 0, len);
						target.write(chunk, 0, len);
					} catch (Throwable err) {
						onThrowable(err);
						return State.ABORT;
					}
					return State.CONTINUE;
				}

				@Override
				public final Integer onCompleted() throws Exception {
					closeStream();
					res.resolve();
					return status;
				}

				@Override
				public final void onThrowable(Throwable t) {
					closeStream();
					res.reject(t);
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
		});
	}

	public Promise transferTo(PacketStream target) {
		return new Promise(res -> {
			client.executeRequest(builder.build(), new AsyncHandler<Integer>() {

				private int status = 200;

				@Override
				public final State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
					try {
						status = responseStatus.getStatusCode();
					} catch (Throwable err) {
						onThrowable(err);
						return State.ABORT;
					}
					return State.CONTINUE;
				}

				@Override
				public final State onHeadersReceived(HttpHeaders httpHeaders) throws Exception {
					return State.CONTINUE;
				}

				@Override
				public final State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
					try {
						ByteBuffer buffer = bodyPart.getBodyByteBuffer();
						int len = buffer.capacity();
						byte[] chunk = new byte[len];
						buffer.get(chunk, 0, len);
						target.sendData(chunk);
					} catch (Throwable err) {
						onThrowable(err);
						return State.ABORT;
					}
					return State.CONTINUE;
				}

				@Override
				public final Integer onCompleted() throws Exception {
					closeStream();
					res.resolve();
					return status;
				}

				@Override
				public final void onThrowable(Throwable t) {
					closeStream();
					res.reject(t);
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
		});
	}

	// --- EXECUTE REQUEST (JSON RESPONSE) ---

	public Promise execute() {
		return new Promise(res -> {
			client.executeRequest(builder.build(), new AsyncHandler<Integer>() {

				private int status = 200;
				private HttpHeaders httpHeaders;
				private byte[] body;

				@Override
				public final State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
					try {
						status = responseStatus.getStatusCode();
					} catch (Throwable err) {
						res.reject(err);
						return State.ABORT;
					}
					return State.CONTINUE;
				}

				@Override
				public final State onHeadersReceived(HttpHeaders httpHeaders) throws Exception {
					this.httpHeaders = httpHeaders;
					return State.CONTINUE;
				}

				@Override
				public final State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
					try {
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
					} catch (Throwable err) {
						res.reject(err);
						return State.ABORT;
					}
					return State.CONTINUE;
				}

				@Override
				public final Integer onCompleted() throws Exception {
					try {
						Tree rsp;
						if (body == null || body.length == 0) {
							rsp = new Tree();
						} else if (body[0] == '{' || body[0] == '[') {
							rsp = new Tree(body);
						} else {
							rsp = new Tree().put("data", body);
						}
						Tree meta = rsp.getMeta();
						meta.put("$status", status);
						Tree headers = meta.putMap("$headers");
						if (httpHeaders != null) {
							Iterator<Entry<CharSequence, CharSequence>> i = httpHeaders.iteratorCharSequence();
							Entry<CharSequence, CharSequence> e;
							while (i.hasNext()) {
								e = i.next();
								headers.put(e.getKey().toString(), e.getValue().toString());
							}
						}
						res.resolve(rsp);
					} catch (Throwable err) {
						res.reject(err);
					}
					return status;
				}

				@Override
				public final void onThrowable(Throwable t) {
					res.reject(t);
				}

			});
		});
	}

	// --- DELEGATED EXECUTE REQUEST ---
	
	public <T> ListenableFuture<T> executeRequest(AsyncHandler<T> handler) {
		return client.executeRequest(builder.build(), handler); 
	}
	
}