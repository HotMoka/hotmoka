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

package io.hotmoka.node.messages.internal;

import java.util.Objects;

import io.hotmoka.beans.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.messages.api.PostConstructorCallTransactionResultMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessage;

/**
 * Implementation of the network message corresponding to the result of the {@link Node#postConstructorCallTransaction(ConstructorCallTransactionRequest)} method.
 */
public class PostConstructorCallTransactionResultMessageImpl extends AbstractRpcMessage implements PostConstructorCallTransactionResultMessage {

	/**
	 * The result of the call; this is the reference to the transaction that has been posted.
	 */
	private final TransactionReference result;

	/**
	 * Creates the message.
	 * 
	 * @param result the result of the call; this is the reference to the transaction that has been posted
	 * @param id the identifier of the message
	 */
	public PostConstructorCallTransactionResultMessageImpl(TransactionReference result, String id) {
		super(id);

		this.result = Objects.requireNonNull(result, "result cannot be null");
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof PostConstructorCallTransactionResultMessage pcctrm && super.equals(other) && result.equals(pcctrm.get());
	}

	@Override
	protected String getExpectedType() {
		return PostConstructorCallTransactionResultMessage.class.getName();
	}

	@Override
	public TransactionReference get() {
		return result;
	}
}