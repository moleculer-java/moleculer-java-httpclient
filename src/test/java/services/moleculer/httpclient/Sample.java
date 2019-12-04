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

import io.datatree.Tree;

public class Sample {

	public static void main(String[] args) {
		System.out.println("START");
		try {

			// Init client
			HttpClient client = new HttpClient();
			client.start();
						
			// Create JSON request (=POST body)
			Tree req = new Tree().put("key", "value");

			client.post("", params -> {
				params.addCookie(null);
			}).then(rsp -> {
				
			});
			
			// Invoke REST service
			client.post("http://localhost:4151/", req).then(rsp -> {
				
				// Success (rsp = JSON response)
				System.out.println(rsp);
				
			}).catchError(err -> {
				
				// Failed (err = Throwable)
				err.printStackTrace();
				
			});
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("STOP");
	}

}
