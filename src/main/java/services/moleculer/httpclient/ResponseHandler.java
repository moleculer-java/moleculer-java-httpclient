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

import java.util.Iterator;
import java.util.Map.Entry;

import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;

import io.datatree.Tree;
import io.netty.handler.codec.http.HttpHeaders;

/**
 * Base class for response parsers (JSON, byte-array, etc).
 */
public abstract class ResponseHandler implements AsyncHandler<Tree> {

	// --- VARIABLES ---

	protected final RequestParams params;
		
	protected volatile int status = 200;
	protected volatile HttpHeaders httpHeaders;
		
	// --- CONSTRUCTOR ---
	
	protected ResponseHandler(RequestParams params) {
		this.params = params;
	}

	// --- REQUEST PROCESSORS ---
	
	@Override
	public State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
		if (params.returnStatusCode) {
			status = responseStatus.getStatusCode();
		}
		return State.CONTINUE;
	}

	@Override
	public State onHeadersReceived(HttpHeaders httpHeaders) throws Exception {
		if (params.returnHttpHeaders) {
			this.httpHeaders = httpHeaders;
		}
		return State.CONTINUE;
	}

	@Override
	public void onThrowable(Throwable t) {
	}
	
	protected void addStatusAndHeaders(Tree rsp) {
		if (params.returnStatusCode || params.returnHttpHeaders) {
			Tree meta = rsp.getMeta();
			if (params.returnStatusCode) {
				meta.put("$status", status);
			}
			if (params.returnHttpHeaders) {
				Tree headers = meta.putMap("$headers");
				if (httpHeaders != null) {
					Iterator<Entry<CharSequence, CharSequence>> i = httpHeaders.iteratorCharSequence();
					Entry<CharSequence, CharSequence> e;
					while (i.hasNext()) {
						e = i.next();
						headers.put(e.getKey().toString(), e.getValue().toString());
					}
				}
			}
		}
	}
	
	@Override
	public abstract State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception;

	@Override
	public abstract Tree onCompleted() throws Exception;
	
}