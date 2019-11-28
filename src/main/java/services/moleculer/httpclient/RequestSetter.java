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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.Param;
import org.asynchttpclient.Realm;
import org.asynchttpclient.SignatureCalculator;
import org.asynchttpclient.channel.ChannelPoolPartitioning;
import org.asynchttpclient.proxy.ProxyServer;
import org.asynchttpclient.proxy.ProxyServer.Builder;
import org.asynchttpclient.request.body.generator.BodyGenerator;
import org.asynchttpclient.request.body.multipart.Part;
import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.resolver.NameResolver;

public abstract class RequestSetter<SELF extends RequestSetter<SELF>> {

	protected final BoundRequestBuilder builder;
	
	// --- CONSTRUCTOR ---
	
	protected RequestSetter(BoundRequestBuilder builder) {
		this.builder = builder;
	}
	
	// --- DELEGATED SETTERS ---

	public SELF setAddress(InetAddress address) {
		builder.setAddress(address);
		return self();
	}

	public SELF setLocalAddress(InetAddress address) {
		builder.setLocalAddress(address);
		return self();
	}

	public SELF setVirtualHost(String virtualHost) {
		builder.setVirtualHost(virtualHost);
		return self();
	}

	public SELF clearHeaders() {
		builder.clearHeaders();
		return self();
	}

	public SELF setHeader(CharSequence name, String value) {
		builder.setHeader(name, value);
		return self();
	}

	public SELF setHeader(CharSequence name, Object value) {
		builder.setHeader(name, value);
		return self();
	}

	public SELF setHeader(CharSequence name, Iterable<?> values) {
		builder.setHeader(name, values);
		return self();
	}

	public SELF addHeader(CharSequence name, String value) {
		builder.addHeader(name, value);
		return self();
	}

	public SELF addHeader(CharSequence name, Object value) {
		builder.addHeader(name, value);
		return self();
	}

	public SELF addHeader(CharSequence name, Iterable<?> values) {
		builder.addHeader(name, values);
		return self();
	}

	public SELF setHeaders(HttpHeaders headers) {
		builder.setHeaders(headers);
		return self();
	}

	public SELF setHeaders(Map<? extends CharSequence, ? extends Iterable<?>> headers) {
		builder.setHeaders(headers);
		return self();
	}

	public SELF setSingleHeaders(Map<? extends CharSequence, ?> headers) {
		builder.setSingleHeaders(headers);
		return self();
	}

	public SELF setCookies(Collection<Cookie> cookies) {
		builder.setCookies(cookies);
		return self();
	}

	public SELF addCookie(Cookie cookie) {
		builder.addCookie(cookie);
		return self();
	}

	public SELF addOrReplaceCookie(Cookie cookie) {
		builder.addOrReplaceCookie(cookie);
		return self();
	}

	public SELF resetCookies() {
		builder.resetCookies();
		return self();
	}

	public SELF resetQuery() {
		builder.resetQuery();
		return self();
	}

	public SELF resetFormParams() {
		builder.resetFormParams();
		return self();
	}

	public SELF resetNonMultipartData() {
		builder.resetNonMultipartData();
		return self();
	}

	public SELF resetMultipartData() {
		builder.resetMultipartData();
		return self();
	}

	public SELF setBody(File file) {
		builder.setBody(file);
		return self();
	}

	public SELF setBody(byte[] data) {
		builder.setBody(data);
		return self();
	}

	public SELF setBody(List<byte[]> data) {
		builder.setBody(data);
		return self();
	}

	public SELF setBody(String data) {
		builder.setBody(data);
		return self();
	}

	public SELF setBody(ByteBuffer data) {
		builder.setBody(data);
		return self();
	}

	public SELF setBody(InputStream stream) {
		builder.setBody(stream);
		return self();
	}

	public SELF setBody(Publisher<ByteBuf> publisher) {
		builder.setBody(publisher);
		return self();
	}

	public SELF setBody(Publisher<ByteBuf> publisher, long contentLength) {
		builder.setBody(publisher, contentLength);
		return self();
	}

	public SELF setBody(BodyGenerator bodyGenerator) {
		builder.setBody(bodyGenerator);
		return self();
	}

	public SELF addQueryParam(String name, String value) {
		builder.addQueryParam(name, value);
		return self();
	}

	public SELF addQueryParams(List<Param> params) {
		builder.addQueryParams(params);
		return self();
	}

	public SELF setQueryParams(Map<String, List<String>> map) {
		builder.setQueryParams(map);
		return self();
	}

	public SELF setQueryParams(List<Param> params) {
		builder.setQueryParams(params);
		return self();
	}

	public SELF addFormParam(String name, String value) {
		builder.addFormParam(name, value);
		return self();
	}

	public SELF setFormParams(Map<String, List<String>> map) {
		builder.setFormParams(map);
		return self();
	}

	public SELF setFormParams(List<Param> params) {
		builder.setFormParams(params);
		return self();
	}

	public SELF addBodyPart(Part bodyPart) {
		builder.addBodyPart(bodyPart);
		return self();
	}

	public SELF setBodyParts(List<Part> bodyParts) {
		builder.setBodyParts(bodyParts);
		return self();
	}

	public SELF setProxyServer(ProxyServer proxyServer) {
		builder.setProxyServer(proxyServer);
		return self();
	}

	public SELF setProxyServer(Builder proxyServerBuilder) {
		builder.setProxyServer(proxyServerBuilder);
		return self();
	}

	public SELF setRealm(Realm.Builder realm) {
		builder.setRealm(realm);
		return self();
	}

	public SELF setRealm(Realm realm) {
		builder.setRealm(realm);
		return self();
	}

	public SELF setFollowRedirect(boolean followRedirect) {
		builder.setFollowRedirect(followRedirect);
		return self();
	}

	public SELF setRequestTimeout(int requestTimeout) {
		builder.setRequestTimeout(requestTimeout);
		return self();
	}

	public SELF setReadTimeout(int readTimeout) {
		builder.setReadTimeout(readTimeout);
		return self();
	}

	public SELF setRangeOffset(long rangeOffset) {
		builder.setRangeOffset(rangeOffset);
		return self();
	}

	public SELF setCharset(Charset charset) {
		builder.setCharset(charset);
		return self();
	}

	public SELF setChannelPoolPartitioning(ChannelPoolPartitioning channelPoolPartitioning) {
		builder.setChannelPoolPartitioning(channelPoolPartitioning);
		return self();
	}

	public SELF setNameResolver(NameResolver<InetAddress> nameResolver) {
		builder.setNameResolver(nameResolver);
		return self();
	}

	public SELF setSignatureCalculator(SignatureCalculator signatureCalculator) {
		builder.setSignatureCalculator(signatureCalculator);
		return self();
	}
	
	// --- SELF TYPE ---
	
	protected abstract SELF self();
	
}