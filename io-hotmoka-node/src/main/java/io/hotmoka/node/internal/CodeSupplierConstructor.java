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

package io.hotmoka.node.internal;

import java.util.concurrent.TimeoutException;

import io.hotmoka.beans.api.responses.ConstructorCallTransactionExceptionResponse;
import io.hotmoka.beans.api.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.beans.api.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.api.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.CodeSupplier;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

/**
 * Implementation of the future of a transaction that executes a constructor in a node.
 * It caches the result for repeated use.
 */
public class CodeSupplierConstructor implements CodeSupplier<StorageReference> {
	private final TransactionReference reference;
	private final Node node;

	/**
	 * The cached result.
	 */
	private volatile StorageReference cachedGet;

	/**
	 * Creates a future for the execution a constructor in a node, with a transaction request
	 * that has the given reference.
	 * 
	 * @param reference the reference to the request of the transaction
	 * @param node the node where the constructor is executed
	 */
	public CodeSupplierConstructor(TransactionReference reference, Node node) {
		this.reference = reference;
		this.node = node;
	}

	@Override
	public TransactionReference getReferenceOfRequest() {
		return reference;
	}

	@Override
	public StorageReference get() throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		try {
			return cachedGet != null ? cachedGet : (cachedGet = getOutcome((ConstructorCallTransactionResponse) node.getPolledResponse(reference)));
		}
		catch (TransactionRejectedException | CodeExecutionException | TransactionException | NodeException | TimeoutException | InterruptedException e) {
			throw e;
		}
		catch (Throwable t) {
			throw new TransactionRejectedException(t);
		}
	}

	private StorageReference getOutcome(ConstructorCallTransactionResponse response) throws CodeExecutionException, TransactionException {
		if (response instanceof ConstructorCallTransactionExceptionResponse ccter)
			throw new CodeExecutionException(ccter.getClassNameOfCause(), ccter.getMessageOfCause(), ccter.getWhere());
		else if (response instanceof ConstructorCallTransactionFailedResponse cctfr)
			throw new TransactionException(cctfr.getClassNameOfCause(), cctfr.getMessageOfCause(), cctfr.getWhere());
		else
			return ((ConstructorCallTransactionSuccessfulResponse) response).getNewObject();
	}
}