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

public class RequestSetter {

	protected final BoundRequestBuilder builder;
	
	// --- CONSTRUCTOR ---
	
	protected RequestSetter(BoundRequestBuilder builder) {
		this.builder = builder;
	}
	
	// --- DELEGATED SETTERS ---

	public RequestSetter setAddress(InetAddress address) {
		builder.setAddress(address);
		return this;
	}

	public RequestSetter setLocalAddress(InetAddress address) {
		builder.setLocalAddress(address);
		return this;
	}

	public RequestSetter setVirtualHost(String virtualHost) {
		builder.setVirtualHost(virtualHost);
		return this;
	}

	public RequestSetter clearHeaders() {
		builder.clearHeaders();
		return this;
	}

	public RequestSetter setHeader(CharSequence name, String value) {
		builder.setHeader(name, value);
		return this;
	}

	public RequestSetter setHeader(CharSequence name, Object value) {
		builder.setHeader(name, value);
		return this;
	}

	public RequestSetter setHeader(CharSequence name, Iterable<?> values) {
		builder.setHeader(name, values);
		return this;
	}

	public RequestSetter addHeader(CharSequence name, String value) {
		builder.addHeader(name, value);
		return this;
	}

	public RequestSetter addHeader(CharSequence name, Object value) {
		builder.addHeader(name, value);
		return this;
	}

	public RequestSetter addHeader(CharSequence name, Iterable<?> values) {
		builder.addHeader(name, values);
		return this;
	}

	public RequestSetter setHeaders(HttpHeaders headers) {
		builder.setHeaders(headers);
		return this;
	}

	public RequestSetter setHeaders(Map<? extends CharSequence, ? extends Iterable<?>> headers) {
		builder.setHeaders(headers);
		return this;
	}

	public RequestSetter setSingleHeaders(Map<? extends CharSequence, ?> headers) {
		builder.setSingleHeaders(headers);
		return this;
	}

	public RequestSetter setCookies(Collection<Cookie> cookies) {
		builder.setCookies(cookies);
		return this;
	}

	public RequestSetter addCookie(Cookie cookie) {
		builder.addCookie(cookie);
		return this;
	}

	public RequestSetter addOrReplaceCookie(Cookie cookie) {
		builder.addOrReplaceCookie(cookie);
		return this;
	}

	public RequestSetter resetCookies() {
		builder.resetCookies();
		return this;
	}

	public RequestSetter resetQuery() {
		builder.resetQuery();
		return this;
	}

	public RequestSetter resetFormParams() {
		builder.resetFormParams();
		return this;
	}

	public RequestSetter resetNonMultipartData() {
		builder.resetNonMultipartData();
		return this;
	}

	public RequestSetter resetMultipartData() {
		builder.resetMultipartData();
		return this;
	}

	public RequestSetter setBody(File file) {
		builder.setBody(file);
		return this;
	}

	public RequestSetter setBody(byte[] data) {
		builder.setBody(data);
		return this;
	}

	public RequestSetter setBody(List<byte[]> data) {
		builder.setBody(data);
		return this;
	}

	public RequestSetter setBody(String data) {
		builder.setBody(data);
		return this;
	}

	public RequestSetter setBody(ByteBuffer data) {
		builder.setBody(data);
		return this;
	}

	public RequestSetter setBody(InputStream stream) {
		builder.setBody(stream);
		return this;
	}

	public RequestSetter setBody(Publisher<ByteBuf> publisher) {
		builder.setBody(publisher);
		return this;
	}

	public RequestSetter setBody(Publisher<ByteBuf> publisher, long contentLength) {
		builder.setBody(publisher, contentLength);
		return this;
	}

	public RequestSetter setBody(BodyGenerator bodyGenerator) {
		builder.setBody(bodyGenerator);
		return this;
	}

	public RequestSetter addQueryParam(String name, String value) {
		builder.addQueryParam(name, value);
		return this;
	}

	public RequestSetter addQueryParams(List<Param> params) {
		builder.addQueryParams(params);
		return this;
	}

	public RequestSetter setQueryParams(Map<String, List<String>> map) {
		builder.setQueryParams(map);
		return this;
	}

	public RequestSetter setQueryParams(List<Param> params) {
		builder.setQueryParams(params);
		return this;
	}

	public RequestSetter addFormParam(String name, String value) {
		builder.addFormParam(name, value);
		return this;
	}

	public RequestSetter setFormParams(Map<String, List<String>> map) {
		builder.setFormParams(map);
		return this;
	}

	public RequestSetter setFormParams(List<Param> params) {
		builder.setFormParams(params);
		return this;
	}

	public RequestSetter addBodyPart(Part bodyPart) {
		builder.addBodyPart(bodyPart);
		return this;
	}

	public RequestSetter setBodyParts(List<Part> bodyParts) {
		builder.setBodyParts(bodyParts);
		return this;
	}

	public RequestSetter setProxyServer(ProxyServer proxyServer) {
		builder.setProxyServer(proxyServer);
		return this;
	}

	public RequestSetter setProxyServer(Builder proxyServerBuilder) {
		builder.setProxyServer(proxyServerBuilder);
		return this;
	}

	public RequestSetter setRealm(org.asynchttpclient.Realm.Builder realm) {
		builder.setRealm(realm);
		return this;
	}

	public RequestSetter setRealm(Realm realm) {
		builder.setRealm(realm);
		return this;
	}

	public RequestSetter setFollowRedirect(boolean followRedirect) {
		builder.setFollowRedirect(followRedirect);
		return this;
	}

	public RequestSetter setRequestTimeout(int requestTimeout) {
		builder.setRequestTimeout(requestTimeout);
		return this;
	}

	public RequestSetter setReadTimeout(int readTimeout) {
		builder.setReadTimeout(readTimeout);
		return this;
	}

	public RequestSetter setRangeOffset(long rangeOffset) {
		builder.setRangeOffset(rangeOffset);
		return this;
	}

	public RequestSetter setCharset(Charset charset) {
		builder.setCharset(charset);
		return this;
	}

	public RequestSetter setChannelPoolPartitioning(ChannelPoolPartitioning channelPoolPartitioning) {
		builder.setChannelPoolPartitioning(channelPoolPartitioning);
		return this;
	}

	public RequestSetter setNameResolver(NameResolver<InetAddress> nameResolver) {
		builder.setNameResolver(nameResolver);
		return this;
	}

	public RequestSetter setSignatureCalculator(SignatureCalculator signatureCalculator) {
		builder.setSignatureCalculator(signatureCalculator);
		return this;
	}
	
}