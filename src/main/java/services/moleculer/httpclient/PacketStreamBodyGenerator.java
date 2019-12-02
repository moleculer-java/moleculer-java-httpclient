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

import org.asynchttpclient.request.body.Body;
import org.asynchttpclient.request.body.generator.FeedListener;
import org.asynchttpclient.request.body.generator.FeedableBodyGenerator;

import io.netty.buffer.ByteBuf;
import services.moleculer.stream.PacketStream;

public class PacketStreamBodyGenerator implements FeedableBodyGenerator {

	// --- VARIABLES ---
	
	protected final PacketStreamBody body;

	// --- CONSTRUCTOR ---
	
	public PacketStreamBodyGenerator(PacketStream stream, long contentLength) {
		body = new PacketStreamBody(stream, contentLength);
	}

	@Override
	public Body createBody() {
		return body;
	}

	@Override
	public boolean feed(ByteBuf buffer, boolean isLast) throws Exception {
		return false;
	}

	@Override
	public void setListener(FeedListener listener) {
		body.setListener(listener);
	}

}