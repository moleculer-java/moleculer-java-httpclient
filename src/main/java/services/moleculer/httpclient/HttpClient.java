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

import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig.ResponseBodyPartFactory;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.Realm;
import org.asynchttpclient.SslEngineFactory;
import org.asynchttpclient.channel.ChannelPool;
import org.asynchttpclient.channel.KeepAliveStrategy;
import org.asynchttpclient.cookie.CookieStore;
import org.asynchttpclient.filter.IOExceptionFilter;
import org.asynchttpclient.filter.RequestFilter;
import org.asynchttpclient.filter.ResponseFilter;
import org.asynchttpclient.netty.channel.ConnectionSemaphoreFactory;
import org.asynchttpclient.proxy.ProxyServer;
import org.asynchttpclient.proxy.ProxyServerSelector;

import io.datatree.Promise;
import io.datatree.Tree;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.util.Timer;

/**
 * Promise based HTTP client for Moleculer-Java.
 * <pre>
 * HttpClient client = new HttpClient();
 * client.start();
 * 
 * Tree req = new Tree();
 * req.put("key1", "value1");
 * 
 * client.rest("http://host/path", req).then(rsp -> {
 * 
 *   // Success
 *   String value2 = rsp.get("key2", "defaultValue");
 * 
 * }).catchError(err -> {
 *   
 *   // Failed
 *   err.printStackTrace();
 * });
 * </pre>
 */
public class HttpClient {

	// --- PROPERTIES ---

	protected final DefaultAsyncHttpClientConfig.Builder builder = Dsl.config();
	protected AsyncHttpClient client;

	// --- INIT HTTP CLIENT ---

	public void start() throws Exception {
		client = Dsl.asyncHttpClient(builder.build());
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

		// Close connection
		if (client != null) {
			try {
				client.close();
			} catch (Exception cause) {
				cause.printStackTrace();
			}
		}
	}

	// --- SIMPLIFIED REST CALL ---

	public Promise rest(String url) {
		return get(url).execute();
	}

	public Promise rest(String url, Tree request) {
		HttpRequest req;
		if (request == null) {
			req = get(url);
		} else {
			req = post(url);
			Tree meta = request.getMeta(false);
			boolean contentTypeSet = false;
			if (meta != null) {
				Tree headers = meta.get("$headers");
				if (headers != null) {
					String name;
					for (Tree header : headers) {
						name = header.getName();
						req.setHeader(name, headers.asString());
						if ("Content-Type".equals(name)) {
							contentTypeSet = true;
						}
					}
				}
			}
			if (!contentTypeSet) {
				req.setHeader("Content-Type", "application/json; charset=utf-8");
			}
			req.setBody(request);
		}
		return req.execute();
	}

	// --- BUILDER-STYLE HTTP METHODS ---

	public HttpRequest get(String url) {
		return new HttpRequest(client, "GET", url);
	}

	public HttpRequest connect(String url) {
		return new HttpRequest(client, "CONNECT", url);
	}

	public HttpRequest options(String url) {
		return new HttpRequest(client, "OPTIONS", url);
	}

	public HttpRequest head(String url) {
		return new HttpRequest(client, "HEAD", url);
	}

	public HttpRequest post(String url) {
		return new HttpRequest(client, "POST", url);
	}

	public HttpRequest put(String url) {
		return new HttpRequest(client, "PUT", url);
	}

	public HttpRequest delete(String url) {
		return new HttpRequest(client, "DELETE", url);
	}

	public HttpRequest patch(String url) {
		return new HttpRequest(client, "PATCH", url);
	}

	public HttpRequest trace(String url) {
		return new HttpRequest(client, "TRACE", url);
	}

	// --- DELEGATED METHODS ---

	public HttpClient setFollowRedirect(boolean followRedirect) {
		builder.setFollowRedirect(followRedirect);
		return this;
	}

	public HttpClient setMaxRedirects(int maxRedirects) {
		builder.setMaxRedirects(maxRedirects);
		return this;
	}

	public HttpClient setStrict302Handling(boolean strict302Handling) {
		builder.setStrict302Handling(strict302Handling);
		return this;
	}

	public HttpClient setCompressionEnforced(boolean compressionEnforced) {
		builder.setCompressionEnforced(compressionEnforced);
		return this;
	}

	public HttpClient setUserAgent(String userAgent) {
		builder.setUserAgent(userAgent);
		return this;
	}

	public HttpClient setRealm(Realm realm) {
		builder.setRealm(realm);
		return this;
	}

	public HttpClient setRealm(org.asynchttpclient.Realm.Builder realmBuilder) {
		builder.setRealm(realmBuilder);
		return this;
	}

	public HttpClient setMaxRequestRetry(int maxRequestRetry) {
		builder.setMaxRequestRetry(maxRequestRetry);
		return this;
	}

	public HttpClient setDisableUrlEncodingForBoundRequests(boolean disableUrlEncodingForBoundRequests) {
		builder.setDisableUrlEncodingForBoundRequests(disableUrlEncodingForBoundRequests);
		return this;
	}

	public HttpClient setUseLaxCookieEncoder(boolean useLaxCookieEncoder) {
		builder.setUseLaxCookieEncoder(useLaxCookieEncoder);
		return this;
	}

	public HttpClient setDisableZeroCopy(boolean disableZeroCopy) {
		builder.setDisableZeroCopy(disableZeroCopy);
		return this;
	}

	public HttpClient setKeepEncodingHeader(boolean keepEncodingHeader) {
		builder.setKeepEncodingHeader(keepEncodingHeader);
		return this;
	}

	public HttpClient setProxyServerSelector(ProxyServerSelector proxyServerSelector) {
		builder.setProxyServerSelector(proxyServerSelector);
		return this;
	}

	public HttpClient setValidateResponseHeaders(boolean validateResponseHeaders) {
		builder.setValidateResponseHeaders(validateResponseHeaders);
		return this;
	}

	public HttpClient setProxyServer(ProxyServer proxyServer) {
		builder.setProxyServer(proxyServer);
		return this;
	}

	public HttpClient setProxyServer(org.asynchttpclient.proxy.ProxyServer.Builder proxyServerBuilder) {
		builder.setProxyServer(proxyServerBuilder);
		return this;
	}

	public HttpClient setUseProxySelector(boolean useProxySelector) {
		builder.setUseProxySelector(useProxySelector);
		return this;
	}

	public HttpClient setUseProxyProperties(boolean useProxyProperties) {
		builder.setUseProxyProperties(useProxyProperties);
		return this;
	}

	public HttpClient setAggregateWebSocketFrameFragments(boolean aggregateWebSocketFrameFragments) {
		builder.setAggregateWebSocketFrameFragments(aggregateWebSocketFrameFragments);
		return this;
	}

	public HttpClient setEnablewebSocketCompression(boolean enablewebSocketCompression) {
		builder.setEnablewebSocketCompression(enablewebSocketCompression);
		return this;
	}

	public HttpClient setWebSocketMaxBufferSize(int webSocketMaxBufferSize) {
		builder.setWebSocketMaxBufferSize(webSocketMaxBufferSize);
		return this;
	}

	public HttpClient setWebSocketMaxFrameSize(int webSocketMaxFrameSize) {
		builder.setWebSocketMaxFrameSize(webSocketMaxFrameSize);
		return this;
	}

	public HttpClient setConnectTimeout(int connectTimeout) {
		builder.setConnectTimeout(connectTimeout);
		return this;
	}

	public HttpClient setRequestTimeout(int requestTimeout) {
		builder.setRequestTimeout(requestTimeout);
		return this;
	}

	public HttpClient setReadTimeout(int readTimeout) {
		builder.setReadTimeout(readTimeout);
		return this;
	}

	public HttpClient setShutdownQuietPeriod(int shutdownQuietPeriod) {
		builder.setShutdownQuietPeriod(shutdownQuietPeriod);
		return this;
	}

	public HttpClient setShutdownTimeout(int shutdownTimeout) {
		builder.setShutdownTimeout(shutdownTimeout);
		return this;
	}

	public HttpClient setKeepAlive(boolean keepAlive) {
		builder.setKeepAlive(keepAlive);
		return this;
	}

	public HttpClient setPooledConnectionIdleTimeout(int pooledConnectionIdleTimeout) {
		builder.setPooledConnectionIdleTimeout(pooledConnectionIdleTimeout);
		return this;
	}

	public HttpClient setConnectionPoolCleanerPeriod(int connectionPoolCleanerPeriod) {
		builder.setConnectionPoolCleanerPeriod(connectionPoolCleanerPeriod);
		return this;
	}

	public HttpClient setConnectionTtl(int connectionTtl) {
		builder.setConnectionTtl(connectionTtl);
		return this;
	}

	public HttpClient setMaxConnections(int maxConnections) {
		builder.setMaxConnections(maxConnections);
		return this;
	}

	public HttpClient setMaxConnectionsPerHost(int maxConnectionsPerHost) {
		builder.setMaxConnectionsPerHost(maxConnectionsPerHost);
		return this;
	}

	public HttpClient setAcquireFreeChannelTimeout(int acquireFreeChannelTimeout) {
		builder.setAcquireFreeChannelTimeout(acquireFreeChannelTimeout);
		return this;
	}

	public HttpClient setChannelPool(ChannelPool channelPool) {
		builder.setChannelPool(channelPool);
		return this;
	}

	public HttpClient setConnectionSemaphoreFactory(ConnectionSemaphoreFactory connectionSemaphoreFactory) {
		builder.setConnectionSemaphoreFactory(connectionSemaphoreFactory);
		return this;
	}

	public HttpClient setKeepAliveStrategy(KeepAliveStrategy keepAliveStrategy) {
		builder.setKeepAliveStrategy(keepAliveStrategy);
		return this;
	}

	public HttpClient setUseOpenSsl(boolean useOpenSsl) {
		builder.setUseOpenSsl(useOpenSsl);
		return this;
	}

	public HttpClient setUseInsecureTrustManager(boolean useInsecureTrustManager) {
		builder.setUseInsecureTrustManager(useInsecureTrustManager);
		return this;
	}

	public HttpClient setDisableHttpsEndpointIdentificationAlgorithm(
			boolean disableHttpsEndpointIdentificationAlgorithm) {
		builder.setDisableHttpsEndpointIdentificationAlgorithm(disableHttpsEndpointIdentificationAlgorithm);
		return this;
	}

	public HttpClient setHandshakeTimeout(int handshakeTimeout) {
		builder.setHandshakeTimeout(handshakeTimeout);
		return this;
	}

	public HttpClient setEnabledProtocols(String[] enabledProtocols) {
		builder.setEnabledProtocols(enabledProtocols);
		return this;
	}

	public HttpClient setEnabledCipherSuites(String[] enabledCipherSuites) {
		builder.setEnabledCipherSuites(enabledCipherSuites);
		return this;
	}

	public HttpClient setFilterInsecureCipherSuites(boolean filterInsecureCipherSuites) {
		builder.setFilterInsecureCipherSuites(filterInsecureCipherSuites);
		return this;
	}

	public HttpClient setSslSessionCacheSize(Integer sslSessionCacheSize) {
		builder.setSslSessionCacheSize(sslSessionCacheSize);
		return this;
	}

	public HttpClient setSslSessionTimeout(Integer sslSessionTimeout) {
		builder.setSslSessionTimeout(sslSessionTimeout);
		return this;
	}

	public HttpClient setSslContext(SslContext sslContext) {
		builder.setSslContext(sslContext);
		return this;
	}

	public HttpClient setSslEngineFactory(SslEngineFactory sslEngineFactory) {
		builder.setSslEngineFactory(sslEngineFactory);
		return this;
	}

	public HttpClient addRequestFilter(RequestFilter requestFilter) {
		builder.addRequestFilter(requestFilter);
		return this;
	}

	public HttpClient removeRequestFilter(RequestFilter requestFilter) {
		builder.removeRequestFilter(requestFilter);
		return this;
	}

	public HttpClient addResponseFilter(ResponseFilter responseFilter) {
		builder.addResponseFilter(responseFilter);
		return this;
	}

	public HttpClient removeResponseFilter(ResponseFilter responseFilter) {
		builder.removeResponseFilter(responseFilter);
		return this;
	}

	public HttpClient addIOExceptionFilter(IOExceptionFilter ioExceptionFilter) {
		builder.addIOExceptionFilter(ioExceptionFilter);
		return this;
	}

	public HttpClient removeIOExceptionFilter(IOExceptionFilter ioExceptionFilter) {
		builder.removeIOExceptionFilter(ioExceptionFilter);
		return this;
	}

	public HttpClient setCookieStore(CookieStore cookieStore) {
		builder.setCookieStore(cookieStore);
		return this;
	}

	public HttpClient setTcpNoDelay(boolean tcpNoDelay) {
		builder.setTcpNoDelay(tcpNoDelay);
		return this;
	}

	public HttpClient setSoReuseAddress(boolean soReuseAddress) {
		builder.setSoReuseAddress(soReuseAddress);
		return this;
	}

	public HttpClient setSoLinger(int soLinger) {
		builder.setSoLinger(soLinger);
		return this;
	}

	public HttpClient setSoSndBuf(int soSndBuf) {
		builder.setSoSndBuf(soSndBuf);
		return this;
	}

	public HttpClient setSoRcvBuf(int soRcvBuf) {
		builder.setSoRcvBuf(soRcvBuf);
		return this;
	}

	public HttpClient setThreadPoolName(String threadPoolName) {
		builder.setThreadPoolName(threadPoolName);
		return this;
	}

	public HttpClient setHttpClientCodecMaxInitialLineLength(int httpClientCodecMaxInitialLineLength) {
		builder.setHttpClientCodecMaxInitialLineLength(httpClientCodecMaxInitialLineLength);
		return this;
	}

	public HttpClient setHttpClientCodecMaxHeaderSize(int httpClientCodecMaxHeaderSize) {
		builder.setHttpClientCodecMaxHeaderSize(httpClientCodecMaxHeaderSize);
		return this;
	}

	public HttpClient setHttpClientCodecMaxChunkSize(int httpClientCodecMaxChunkSize) {
		builder.setHttpClientCodecMaxChunkSize(httpClientCodecMaxChunkSize);
		return this;
	}

	public HttpClient setHttpClientCodecInitialBufferSize(int httpClientCodecInitialBufferSize) {
		builder.setHttpClientCodecInitialBufferSize(httpClientCodecInitialBufferSize);
		return this;
	}

	public HttpClient setChunkedFileChunkSize(int chunkedFileChunkSize) {
		builder.setChunkedFileChunkSize(chunkedFileChunkSize);
		return this;
	}

	public <T> HttpClient addChannelOption(ChannelOption<T> name, T value) {
		builder.addChannelOption(name, value);
		return this;
	}

	public HttpClient setEventLoopGroup(EventLoopGroup eventLoopGroup) {
		builder.setEventLoopGroup(eventLoopGroup);
		return this;
	}

	public HttpClient setUseNativeTransport(boolean useNativeTransport) {
		builder.setUseNativeTransport(useNativeTransport);
		return this;
	}

	public HttpClient setAllocator(ByteBufAllocator allocator) {
		builder.setAllocator(allocator);
		return this;
	}

	public HttpClient setNettyTimer(Timer nettyTimer) {
		builder.setNettyTimer(nettyTimer);
		return this;
	}

	public HttpClient setThreadFactory(ThreadFactory threadFactory) {
		builder.setThreadFactory(threadFactory);
		return this;
	}

	public HttpClient setHttpAdditionalChannelInitializer(Consumer<Channel> httpAdditionalChannelInitializer) {
		builder.setHttpAdditionalChannelInitializer(httpAdditionalChannelInitializer);
		return this;
	}

	public HttpClient setWsAdditionalChannelInitializer(Consumer<Channel> wsAdditionalChannelInitializer) {
		builder.setWsAdditionalChannelInitializer(wsAdditionalChannelInitializer);
		return this;
	}

	public HttpClient setResponseBodyPartFactory(ResponseBodyPartFactory responseBodyPartFactory) {
		builder.setResponseBodyPartFactory(responseBodyPartFactory);
		return this;
	}

	public HttpClient setIoThreadsCount(int ioThreadsCount) {
		builder.setIoThreadsCount(ioThreadsCount);
		return this;
	}

}
