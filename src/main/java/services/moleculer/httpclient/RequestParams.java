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
import org.asynchttpclient.RequestBuilderBase;

import io.datatree.Tree;
import services.moleculer.stream.PacketStream;

/**
 * Builder for a HTTTP request. Usage:
 * 
 * <pre>
 * client.post("http://server/path", params -> {
 * 
 *    // Set request parameters:
 *    params.returnAsByteArray();
 *    params.setHeader("name", "value");
 *    // ...
 * 
 * }).then(rsp -> {
 * 
 *    // Success; response in the "rsp" object
 * 
 * }).catchError(err -> {
 *
 *    // Request failed; "err" is a Throwable
 * 
 * });
 * </pre>
 */
public class RequestParams extends RequestBuilderBase<RequestParams> {

	// --- VARIABLES ---

	/**
	 * Copy HTTP response status into the Meta structure of the response Tree.
	 */
	protected boolean returnStatusCode;

	/**
	 * Copy HTTP response headers into the Meta structure of the response Tree.
	 */
	protected boolean returnHttpHeaders;

	/**
	 * Output handler / parser.
	 */
	protected AsyncHandler<?> handler;

	/**
	 * Do not parse response as JSON, just return the response body in a
	 * byte-array.
	 */
	protected boolean returnBytes;

	// --- CONSTRUCTOR ---

	protected RequestParams(String method, boolean isDisableUrlEncoding) {
		super(method, isDisableUrlEncoding);
	}

	// --- SET JSON BODY AS TREE ---

	/**
	 * Set the request body by the specified Tree (~= JSON object).
	 * 
	 * @param data
	 *            input JSON structure
	 * 
	 * @return this builder (for method chaining)
	 */
	public RequestParams setBody(Tree data) {
		if (data != null) {
			setBody(data.toBinary());
		}
		return this;
	}

	// --- SET BINARY BODY AS MOLECULER STREAM ---

	/**
	 * Read request body from the specified PacketStream. The Content-Length is
	 * unknown. Executes a chunked HTTP-request, without "Content-Length"
	 * header.
	 * 
	 * @param stream
	 *            source PacketStream
	 * 
	 * @return this builder (for method chaining)
	 */
	public RequestParams setBody(PacketStream stream) {
		return setBody(stream, -1L);
	}

	/**
	 * Read request body from the specified PacketStream. Executes a normal
	 * HTTP-request, with "Content-Length" header.
	 * 
	 * @param stream
	 *            source PacketStream
	 * @param contentLength
	 *            length of the content (in bytes)
	 * 
	 * @return this builder (for method chaining)
	 */
	public RequestParams setBody(PacketStream stream, long contentLength) {
		if (stream != null) {
			setBody(new PacketStreamBodyGenerator(stream, contentLength));
		}
		return this;
	}

	// --- SET OUTPUT TARGETS ---

	/**
	 * Redirect response to the specified AsyncHandler.
	 * 
	 * @param target
	 *            target AsyncHandler
	 * 
	 * @return this builder (for method chaining)
	 */
	public RequestParams transferTo(AsyncHandler<?> target) {
		handler = target;
		return this;
	}

	/**
	 * Redirect response into the specified PacketStream.
	 * 
	 * @param target
	 *            target PacketStream
	 * 
	 * @return this builder (for method chaining)
	 */
	public RequestParams transferTo(PacketStream target) {
		this.handler = new ResponseToPacketStream(this, target);
		return this;
	}

	/**
	 * Redirect response into the specified OutputStream.
	 * 
	 * @param target
	 *            target OutputStream
	 * 
	 * @return this builder (for method chaining)
	 */
	public RequestParams transferTo(OutputStream target) {
		this.handler = new ResponseToOutputStream(this, target);
		return this;
	}

	/**
	 * Redirect response into the specified WritableByteChannel.
	 * 
	 * @param target
	 *            target WritableByteChannel
	 * 
	 * @return this builder (for method chaining)
	 */
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

	/**
	 * Copy HTTP response headers into the Meta structure of the response Tree.
	 * Usage:
	 * 
	 * <pre>
	 * client.post("http://server/path", params -> {
	 * 	params.returnHttpHeaders();
	 * }).then(rsp -> {
	 * 
	 * 	// Get response status
	 * 	Tree headers = rsp.getMeta().get("$headers");
	 * 	for (Tree header : headers) {
	 * 		String name = header.getName();
	 * 		String value = header.asString();
	 * 	}
	 * 
	 * })
	 * </pre>
	 * 
	 * @return this builder (for method chaining)
	 */
	public RequestParams returnHttpHeaders() {
		this.returnHttpHeaders = true;
		return this;
	}

	/**
	 * Copy HTTP response status into the Meta structure of the response Tree.
	 * Usage:
	 * 
	 * <pre>
	 * client.post("http://server/path", params -> {
	 * 	params.returnStatusCode();
	 * }).then(rsp -> {
	 * 
	 * 	// Get response status
	 * 	int status = rsp.getMeta().get("$status", 0);
	 * 
	 * })
	 * </pre>
	 * 
	 * @return this builder (for method chaining)
	 */
	public RequestParams returnStatusCode() {
		this.returnStatusCode = true;
		return this;
	}

	/**
	 * Do not parse response, just return with a byte array. Usage:
	 * 
	 * <pre>
	 * client.post("http://server/path", params -> {
	 * 
	 * 	// Do not parse the response as JSON
	 * 	params.returnAsByteArray();
	 * 
	 * }).then(rsp -> {
	 * 
	 * 	// Success
	 * 	byte[] bytes = rsp.asBytes();
	 * 
	 * }).catchError(err -> {
	 *
	 * 	// Failed
	 * 	err.printStackTrace();
	 * 
	 * });
	 * </pre>
	 * 
	 * @return this builder (for method chaining)
	 */
	public RequestParams returnAsByteArray() {
		this.returnBytes = true;
		return this;
	}

}