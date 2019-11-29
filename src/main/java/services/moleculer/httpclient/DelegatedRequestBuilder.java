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
import org.asynchttpclient.uri.Uri;
import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.resolver.NameResolver;

public abstract class DelegatedRequestBuilder<T extends DelegatedRequestBuilder<T>> {

	// --- INTERNAL REQUEST BUILDER ---

	protected final BoundRequestBuilder builder;

	// --- CONSTRUCTOR ---

	protected DelegatedRequestBuilder(BoundRequestBuilder builder) {
		this.builder = builder;
	}

	// --- RETURN DERIVED TYPE ---

	@SuppressWarnings("unchecked")
	protected T asDerivedType() {
		return (T) this;
	}

	// --- DELEGATED BUILDER ---

	public BoundRequestBuilder getBuilder() {
		return builder;
	}

	// --- DELEGATED SETTERS ---

	public T setMethod(String method) {
		builder.setMethod(method);
		return asDerivedType();
	}

	public T setUri(Uri uri) {
		builder.setUri(uri);
		return asDerivedType();
	}

	public T setUrl(String url) {
		builder.setUrl(url);
		return asDerivedType();
	}

	public T setAddress(InetAddress address) {
		builder.setAddress(address);
		return asDerivedType();
	}

	public T setLocalAddress(InetAddress address) {
		builder.setLocalAddress(address);
		return asDerivedType();
	}

	public T setVirtualHost(String virtualHost) {
		builder.setVirtualHost(virtualHost);
		return asDerivedType();
	}

	public T clearHeaders() {
		builder.clearHeaders();
		return asDerivedType();
	}

	public T setHeader(CharSequence name, String value) {
		builder.setHeader(name, value);
		return asDerivedType();
	}

	public T setHeader(CharSequence name, Object value) {
		builder.setHeader(name, value);
		return asDerivedType();
	}

	public T setHeader(CharSequence name, Iterable<?> values) {
		builder.setHeader(name, values);
		return asDerivedType();
	}

	public T addHeader(CharSequence name, String value) {
		builder.addHeader(name, value);
		return asDerivedType();
	}

	public T addHeader(CharSequence name, Object value) {
		builder.addHeader(name, value);
		return asDerivedType();
	}

	public T addHeader(CharSequence name, Iterable<?> values) {
		builder.addHeader(name, values);
		return asDerivedType();
	}

	public T setHeaders(HttpHeaders headers) {
		builder.setHeaders(headers);
		return asDerivedType();
	}

	public T setHeaders(Map<? extends CharSequence, ? extends Iterable<?>> headers) {
		builder.setHeaders(headers);
		return asDerivedType();
	}

	public T setSingleHeaders(Map<? extends CharSequence, ?> headers) {
		builder.setSingleHeaders(headers);
		return asDerivedType();
	}

	public T setCookies(Collection<Cookie> cookies) {
		builder.setCookies(cookies);
		return asDerivedType();
	}

	public T addCookie(Cookie cookie) {
		builder.addCookie(cookie);
		return asDerivedType();
	}

	public T addOrReplaceCookie(Cookie cookie) {
		builder.addOrReplaceCookie(cookie);
		return asDerivedType();
	}

	public T resetCookies() {
		builder.resetCookies();
		return asDerivedType();
	}

	public T resetQuery() {
		builder.resetQuery();
		return asDerivedType();
	}

	public T resetFormParams() {
		builder.resetFormParams();
		return asDerivedType();
	}

	public T resetNonMultipartData() {
		builder.resetNonMultipartData();
		return asDerivedType();
	}

	public T resetMultipartData() {
		builder.resetMultipartData();
		return asDerivedType();
	}

	public T setBody(File file) {
		builder.setBody(file);
		return asDerivedType();
	}

	public T setBody(byte[] data) {
		builder.setBody(data);
		return asDerivedType();
	}

	public T setBody(List<byte[]> data) {
		builder.setBody(data);
		return asDerivedType();
	}

	public T setBody(String data) {
		builder.setBody(data);
		return asDerivedType();
	}

	public T setBody(ByteBuffer data) {
		builder.setBody(data);
		return asDerivedType();
	}

	public T setBody(InputStream stream) {
		builder.setBody(stream);
		return asDerivedType();
	}

	public T setBody(Publisher<ByteBuf> publisher) {
		builder.setBody(publisher);
		return asDerivedType();
	}

	public T setBody(Publisher<ByteBuf> publisher, long contentLength) {
		builder.setBody(publisher, contentLength);
		return asDerivedType();
	}

	public T setBody(BodyGenerator bodyGenerator) {
		builder.setBody(bodyGenerator);
		return asDerivedType();
	}

	public T addQueryParam(String name, String value) {
		builder.addQueryParam(name, value);
		return asDerivedType();
	}

	public T addQueryParams(List<Param> params) {
		builder.addQueryParams(params);
		return asDerivedType();
	}

	public T setQueryParams(Map<String, List<String>> map) {
		builder.setQueryParams(map);
		return asDerivedType();
	}

	public T setQueryParams(List<Param> params) {
		builder.setQueryParams(params);
		return asDerivedType();
	}

	public T addFormParam(String name, String value) {
		builder.addFormParam(name, value);
		return asDerivedType();
	}

	public T setFormParams(Map<String, List<String>> map) {
		builder.setFormParams(map);
		return asDerivedType();
	}

	public T setFormParams(List<Param> params) {
		builder.setFormParams(params);
		return asDerivedType();
	}

	public T addBodyPart(Part bodyPart) {
		builder.addBodyPart(bodyPart);
		return asDerivedType();
	}

	public T setBodyParts(List<Part> bodyParts) {
		builder.setBodyParts(bodyParts);
		return asDerivedType();
	}

	public T setProxyServer(ProxyServer proxyServer) {
		builder.setProxyServer(proxyServer);
		return asDerivedType();
	}

	public T setProxyServer(Builder proxyServerBuilder) {
		builder.setProxyServer(proxyServerBuilder);
		return asDerivedType();
	}

	public T setRealm(Realm.Builder realm) {
		builder.setRealm(realm);
		return asDerivedType();
	}

	public T setRealm(Realm realm) {
		builder.setRealm(realm);
		return asDerivedType();
	}

	public T setFollowRedirect(boolean followRedirect) {
		builder.setFollowRedirect(followRedirect);
		return asDerivedType();
	}

	public T setRequestTimeout(int requestTimeout) {
		builder.setRequestTimeout(requestTimeout);
		return asDerivedType();
	}

	public T setReadTimeout(int readTimeout) {
		builder.setReadTimeout(readTimeout);
		return asDerivedType();
	}

	public T setRangeOffset(long rangeOffset) {
		builder.setRangeOffset(rangeOffset);
		return asDerivedType();
	}

	public T setCharset(Charset charset) {
		builder.setCharset(charset);
		return asDerivedType();
	}

	public T setChannelPoolPartitioning(ChannelPoolPartitioning channelPoolPartitioning) {
		builder.setChannelPoolPartitioning(channelPoolPartitioning);
		return asDerivedType();
	}

	public T setNameResolver(NameResolver<InetAddress> nameResolver) {
		builder.setNameResolver(nameResolver);
		return asDerivedType();
	}

	public T setSignatureCalculator(SignatureCalculator signatureCalculator) {
		builder.setSignatureCalculator(signatureCalculator);
		return asDerivedType();
	}

}