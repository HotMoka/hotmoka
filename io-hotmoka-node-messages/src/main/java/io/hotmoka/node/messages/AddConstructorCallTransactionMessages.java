/*
Copyright 2024 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.messages;

import io.hotmoka.beans.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.messages.api.AddConstructorCallTransactionMessage;
import io.hotmoka.node.messages.internal.AddConstructorCallTransactionMessageImpl;
import io.hotmoka.node.messages.internal.gson.AddConstructorCallTransactionMessageDecoder;
import io.hotmoka.node.messages.internal.gson.AddConstructorCallTransactionMessageEncoder;
import io.hotmoka.node.messages.internal.gson.AddConstructorCallTransactionMessageJson;

/**
 * A provider of {@link AddConstructorCallTransactionMessage}.
 */
public final class AddConstructorCallTransactionMessages {

	private AddConstructorCallTransactionMessages() {}

	/**
	 * Yields a {@link AddConstructorCallTransactionMessage}.
	 * 
	 * @param id the identifier of the message
	 * @param request the request of the transaction required to add
	 * @return the message
	 */
	public static AddConstructorCallTransactionMessage of(ConstructorCallTransactionRequest request, String id) {
		return new AddConstructorCallTransactionMessageImpl(request, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends AddConstructorCallTransactionMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends AddConstructorCallTransactionMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends AddConstructorCallTransactionMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(AddConstructorCallTransactionMessage message) {
    		super(message);
    	}
    }
}