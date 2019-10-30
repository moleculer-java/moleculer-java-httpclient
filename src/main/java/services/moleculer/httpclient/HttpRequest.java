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
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.Param;
import org.asynchttpclient.Realm;
import org.asynchttpclient.SignatureCalculator;
import org.asynchttpclient.channel.ChannelPoolPartitioning;
import org.asynchttpclient.proxy.ProxyServer;
import org.asynchttpclient.proxy.ProxyServer.Builder;
import org.asynchttpclient.request.body.generator.BodyGenerator;
import org.asynchttpclient.request.body.multipart.Part;
import org.reactivestreams.Publisher;

import io.datatree.Promise;
import io.datatree.Tree;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.resolver.NameResolver;

/**
 * An object handling an HTTP request. The HTTP request must be executed using the "execute" method.
 * <pre>
 * HttpClient client = new HttpClient();
 * client.init();
 * Promise p = client.post("http://host/port")
 *                   .addQueryParam("key", "value")
 *                   .execute();
 * Tree json = p.waitFor();
 * </pre>
 */
public class HttpRequest {

	// --- VARIABLES ---
	
	protected final AsyncHttpClient client;
	protected final BoundRequestBuilder builder;

	// --- CONSTRUCTOR ---

	protected HttpRequest(AsyncHttpClient asyncHttpClient, String method, String url) {
		this.client = asyncHttpClient;
		this.builder = asyncHttpClient.prepare(method, url);
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

	// --- EXECUTE REQUEST ---

	public Promise execute() {
		return new Promise(res -> {
			client.executeRequest(builder.build(), new AsyncHandler<Integer>() {

				private int status = 200;
				private HttpHeaders httpHeaders;
				private byte[] body;

				@Override
				public State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
					try {
						status = responseStatus.getStatusCode();
					} catch (Throwable err) {
						res.reject(err);
						return State.ABORT;
					}
					return State.CONTINUE;
				}

				@Override
				public State onHeadersReceived(HttpHeaders httpHeaders) throws Exception {
					this.httpHeaders = httpHeaders;
					return State.CONTINUE;
				}

				@Override
				public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
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
				public Integer onCompleted() throws Exception {
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
				public void onThrowable(Throwable t) {
					res.reject(t);
				}

			});
		});
	}

	// --- DELEGATED SETTERS ---

	public HttpRequest setAddress(InetAddress address) {
		builder.setAddress(address);
		return this;
	}

	public HttpRequest setLocalAddress(InetAddress address) {
		builder.setLocalAddress(address);
		return this;
	}

	public HttpRequest setVirtualHost(String virtualHost) {
		builder.setVirtualHost(virtualHost);
		return this;
	}

	public HttpRequest clearHeaders() {
		builder.clearHeaders();
		return this;
	}

	public HttpRequest setHeader(CharSequence name, String value) {
		builder.setHeader(name, value);
		return this;
	}

	public HttpRequest setHeader(CharSequence name, Object value) {
		builder.setHeader(name, value);
		return this;
	}

	public HttpRequest setHeader(CharSequence name, Iterable<?> values) {
		builder.setHeader(name, values);
		return this;
	}

	public HttpRequest addHeader(CharSequence name, String value) {
		builder.addHeader(name, value);
		return this;
	}

	public HttpRequest addHeader(CharSequence name, Object value) {
		builder.addHeader(name, value);
		return this;
	}

	public HttpRequest addHeader(CharSequence name, Iterable<?> values) {
		builder.addHeader(name, values);
		return this;
	}

	public HttpRequest setHeaders(HttpHeaders headers) {
		builder.setHeaders(headers);
		return this;
	}

	public HttpRequest setHeaders(Map<? extends CharSequence, ? extends Iterable<?>> headers) {
		builder.setHeaders(headers);
		return this;
	}

	public HttpRequest setSingleHeaders(Map<? extends CharSequence, ?> headers) {
		builder.setSingleHeaders(headers);
		return this;
	}

	public HttpRequest setCookies(Collection<Cookie> cookies) {
		builder.setCookies(cookies);
		return this;
	}

	public HttpRequest addCookie(Cookie cookie) {
		builder.addCookie(cookie);
		return this;
	}

	public HttpRequest addOrReplaceCookie(Cookie cookie) {
		builder.addOrReplaceCookie(cookie);
		return this;
	}

	public HttpRequest resetCookies() {
		builder.resetCookies();
		return this;
	}

	public HttpRequest resetQuery() {
		builder.resetQuery();
		return this;
	}

	public HttpRequest resetFormParams() {
		builder.resetFormParams();
		return this;
	}

	public HttpRequest resetNonMultipartData() {
		builder.resetNonMultipartData();
		return this;
	}

	public HttpRequest resetMultipartData() {
		builder.resetMultipartData();
		return this;
	}

	public HttpRequest setBody(File file) {
		builder.setBody(file);
		return this;
	}

	public HttpRequest setBody(byte[] data) {
		builder.setBody(data);
		return this;
	}

	public HttpRequest setBody(List<byte[]> data) {
		builder.setBody(data);
		return this;
	}

	public HttpRequest setBody(String data) {
		builder.setBody(data);
		return this;
	}

	public HttpRequest setBody(ByteBuffer data) {
		builder.setBody(data);
		return this;
	}

	public HttpRequest setBody(InputStream stream) {
		builder.setBody(stream);
		return this;
	}

	public HttpRequest setBody(Publisher<ByteBuf> publisher) {
		builder.setBody(publisher);
		return this;
	}

	public HttpRequest setBody(Publisher<ByteBuf> publisher, long contentLength) {
		builder.setBody(publisher, contentLength);
		return this;
	}

	public HttpRequest setBody(BodyGenerator bodyGenerator) {
		builder.setBody(bodyGenerator);
		return this;
	}

	public HttpRequest addQueryParam(String name, String value) {
		builder.addQueryParam(name, value);
		return this;
	}

	public HttpRequest addQueryParams(List<Param> params) {
		builder.addQueryParams(params);
		return this;
	}

	public HttpRequest setQueryParams(Map<String, List<String>> map) {
		builder.setQueryParams(map);
		return this;
	}

	public HttpRequest setQueryParams(List<Param> params) {
		builder.setQueryParams(params);
		return this;
	}

	public HttpRequest addFormParam(String name, String value) {
		builder.addFormParam(name, value);
		return this;
	}

	public HttpRequest setFormParams(Map<String, List<String>> map) {
		builder.setFormParams(map);
		return this;
	}

	public HttpRequest setFormParams(List<Param> params) {
		builder.setFormParams(params);
		return this;
	}

	public HttpRequest addBodyPart(Part bodyPart) {
		builder.addBodyPart(bodyPart);
		return this;
	}

	public HttpRequest setBodyParts(List<Part> bodyParts) {
		builder.setBodyParts(bodyParts);
		return this;
	}

	public HttpRequest setProxyServer(ProxyServer proxyServer) {
		builder.setProxyServer(proxyServer);
		return this;
	}

	public HttpRequest setProxyServer(Builder proxyServerBuilder) {
		builder.setProxyServer(proxyServerBuilder);
		return this;
	}

	public HttpRequest setRealm(org.asynchttpclient.Realm.Builder realm) {
		builder.setRealm(realm);
		return this;
	}

	public HttpRequest setRealm(Realm realm) {
		builder.setRealm(realm);
		return this;
	}

	public HttpRequest setFollowRedirect(boolean followRedirect) {
		builder.setFollowRedirect(followRedirect);
		return this;
	}

	public HttpRequest setRequestTimeout(int requestTimeout) {
		builder.setRequestTimeout(requestTimeout);
		return this;
	}

	public HttpRequest setReadTimeout(int readTimeout) {
		builder.setReadTimeout(readTimeout);
		return this;
	}

	public HttpRequest setRangeOffset(long rangeOffset) {
		builder.setRangeOffset(rangeOffset);
		return this;
	}

	public HttpRequest setCharset(Charset charset) {
		builder.setCharset(charset);
		return this;
	}

	public HttpRequest setChannelPoolPartitioning(ChannelPoolPartitioning channelPoolPartitioning) {
		builder.setChannelPoolPartitioning(channelPoolPartitioning);
		return this;
	}

	public HttpRequest setNameResolver(NameResolver<InetAddress> nameResolver) {
		builder.setNameResolver(nameResolver);
		return this;
	}

	public HttpRequest setSignatureCalculator(SignatureCalculator signatureCalculator) {
		builder.setSignatureCalculator(signatureCalculator);
		return this;
	}

}
