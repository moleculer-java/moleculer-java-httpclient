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

import java.util.LinkedList;
import java.util.function.Consumer;

import org.asynchttpclient.Param;

import io.datatree.Tree;

/**
 * Configurator for Tree/JSON-based requests.
 */
public class TreeConfigurator implements Consumer<RequestParams> {

	// --- VARIABLES ---
	
	protected final Consumer<RequestParams> configurator;
	protected final Tree request;
	protected final boolean post;
	
	// --- CONSTRUCTOR ---
	
	protected TreeConfigurator(Consumer<RequestParams> configurator, Tree request, boolean post) {
		this.configurator = configurator;
		this.request = request;
		this.post = post;
	}
	
	@Override
	public void accept(RequestParams params) {
		if (request != null && !request.isEmpty()) {
			if (post) {
				params.setBody(request.toBinary());
			} else {
				LinkedList<Param> list = new LinkedList<>();
				for (Tree item : request) {
					list.addLast(new Param(item.getName(), item.asString()));
				}
				params.setQueryParams(list);
			}
		}
		if (configurator != null) {
			configurator.accept(params);
		}		
	}

}