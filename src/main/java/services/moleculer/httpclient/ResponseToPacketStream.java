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

import java.nio.ByteBuffer;

import org.asynchttpclient.HttpResponseBodyPart;

import io.datatree.Tree;
import services.moleculer.stream.PacketStream;

public class ResponseToPacketStream extends ResponseHandler {

	// --- VARIABLES ---

	protected final PacketStream target;

	// --- CONSTRUCTOR ---

	protected ResponseToPacketStream(RequestParams params, PacketStream target) {
		super(params);
		this.target = target;
	}

	// --- REQUEST PROCESSORS ---

	@Override
	public void onThrowable(Throwable t) {
		closeStream();
	}

	@Override
	public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
		ByteBuffer buffer = bodyPart.getBodyByteBuffer();
		int len = buffer.capacity();
		byte[] chunk = new byte[len];
		buffer.get(chunk, 0, len);
		target.sendData(chunk);
		return State.CONTINUE;
	}

	@Override
	public Tree onCompleted() throws Exception {
		closeStream();
		Tree rsp = new Tree();
		rsp.put("transfered", target.getTransferedBytes());
		addStatusAndHeaders(rsp);
		return rsp;
	}

	protected void closeStream() {
		if (target != null) {
			target.sendClose();
		}
	}

}