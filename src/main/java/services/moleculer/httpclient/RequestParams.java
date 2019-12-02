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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.RequestBuilderBase;

import io.datatree.Tree;
import services.moleculer.stream.PacketStream;

public class RequestParams extends RequestBuilderBase<BoundRequestBuilder> {

	// --- VARIABLES ---
	
	/**
	 * Copy HTTP response status into the Meta structure of the response Tree.
	 */
	protected boolean returnStatusCode;
	
	/**
	 * Copy HTTP response headers into the Meta structure of the response Tree.
	 */
	protected boolean returnHttpHeaders;
	
	protected AsyncHandler<?> handler;
		
	/**
	 * Do not parse response as JSON, just return the response body in a
	 * byte-array.
	 */
	protected boolean returnBytes;
	
	// --- CONSTRUCTOR ---
	
	protected RequestParams(String method, boolean isDisableUrlEncoding) {
		super(method, isDisableUrlEncoding);
		handler = new ResponseToJson(this);
	}

	// --- SET JSON BODY AS TREE ---

	public RequestParams setBody(Tree data) {
		if (data != null) {
			setBody(data.toBinary());
		}
		return this;
	}

	// --- SET BINARY BODY AS MOLECULER STREAM ---

	public RequestParams setBody(PacketStream stream) {
		return setBody(stream, -1L);
	}

	public RequestParams setBody(PacketStream stream, long contentLength) {
		if (stream != null) {
			setBody(new PacketStreamBodyGenerator(stream, contentLength));
		}
		return this;
	}
	
	// --- SET OUTPUT TARGETS ---

	public RequestParams transferTo(AsyncHandler<?> target) {
		handler = target;
		return this;
	}
	
	public RequestParams transferTo(PacketStream target) {
		this.handler = new ResponseToPacketStream(this, target);
		return this;
	}
	
	public RequestParams transferTo(OutputStream target) {
		this.handler = new ResponseToOutputStream(this, target);
		return this;
	}

	public RequestParams transferTo(WritableByteChannel target) {
		OutputStream out = new OutputStream() {

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

		};
		this.handler = new ResponseToOutputStream(this, out);
		return this;
	}
	
	// --- BUILDER-STYLE PROPERTY SETTERS ---

	public RequestParams setReturnHttpHeaders(boolean returnHttpHeaders) {
		this.returnHttpHeaders = returnHttpHeaders;
		return this;
	}
	
	public RequestParams setReturnStatusCode(boolean returnStatusCode) {
		this.returnStatusCode = returnStatusCode;
		return this;
	}
	
	public RequestParams setReturnBytes(boolean returnBytes) {
		this.returnBytes = returnBytes;
		return this;
	}
	
}