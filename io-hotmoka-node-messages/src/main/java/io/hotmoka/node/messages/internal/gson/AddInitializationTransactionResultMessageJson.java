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

package io.hotmoka.node.messages.internal.gson;

import io.hotmoka.node.messages.AddInitializationTransactionResultMessages;
import io.hotmoka.node.messages.api.AddInitializationTransactionResultMessage;
import io.hotmoka.websockets.beans.AbstractRpcMessageJsonRepresentation;

/**
 * The JSON representation of a {@link AddInitializationTransactionResultMessage}.
 */
public abstract class AddInitializationTransactionResultMessageJson extends AbstractRpcMessageJsonRepresentation<AddInitializationTransactionResultMessage> {

	protected AddInitializationTransactionResultMessageJson(AddInitializationTransactionResultMessage message) {
		super(message);
	}

	@Override
	public AddInitializationTransactionResultMessage unmap() {
		return AddInitializationTransactionResultMessages.of(getId());
	}

	@Override
	protected String getExpectedType() {
		return AddInitializationTransactionResultMessage.class.getName();
	}
}