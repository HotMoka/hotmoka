/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.beans.marshalling.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.marshalling.AbstractObjectUnmarshaller;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * An unmarshaller for transaction references.
 */
public class TransactionReferenceUnmarshaller extends AbstractObjectUnmarshaller<TransactionReference> {

	private final Map<Integer, TransactionReference> memory = new HashMap<>();

	public TransactionReferenceUnmarshaller() {
		super(TransactionReference.class);
	}

	@Override
	public TransactionReference read(UnmarshallingContext context) throws IOException {
		int selector = context.readByte();
		if (selector < 0)
			selector = 256 + selector;

		if (selector == 255) {
			byte[] bytes = context.readBytes(TransactionRequest.REQUEST_HASH_LENGTH, "Cannot read a transaction request");
			var reference = TransactionReferences.of(bytes);
			memory.put(memory.size(), reference);
			return reference;
		}
		else if (selector == 254)
			return memory.get(context.readInt());
		else
			return memory.get(selector);
	}
}