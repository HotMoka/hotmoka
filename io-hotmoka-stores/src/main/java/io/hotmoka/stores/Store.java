/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.stores;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;

/**
 * The store of a node. It keeps information about the state of the objects created
 * by the requests executed by the node. A store is external to the node and, typically, only
 * its hash is held in the node, if consensus is needed. Stores must be thread-safe, since they can
 * be used concurrently for executing more requests.
 */
@ThreadSafe
public interface Store extends AutoCloseable {

	/**
	 * Yields the response of the transaction having the given reference.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response, if any
	 */
	Optional<TransactionResponse> getResponse(TransactionReference reference);

	/**
	 * Yields the response of the transaction having the given reference.
	 * The response if returned also when it is not committed yet.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response, if any
	 */
	Optional<TransactionResponse> getResponseUncommitted(TransactionReference reference);

	/**
	 * Yields the error generated by the transaction having the given reference.
	 * 
	 * @param reference the reference of the transaction
	 * @return the error, if any
	 */
	Optional<String> getError(TransactionReference reference);

	/**
	 * Yields the history of the given object, that is, the references to the transactions
	 * that provide information about the current values of its fields.
	 * 
	 * @param object the reference of the object
	 * @return the history. Yields an empty stream if there is no history for {@code object}
	 */
	Stream<TransactionReference> getHistory(StorageReference object);

	/**
	 * Yields the history of the given object, that is, the references of the transactions
	 * that provide information about the current values of its fields. This considers also
	 * updates of the transaction not committed yet.
	 * 
	 * @param object the reference of the object
	 * @return the history. Yields an empty stream if there is no history for {@code object}
	 */
	Stream<TransactionReference> getHistoryUncommitted(StorageReference object);

	/**
	 * Yields the manifest installed when the node is initialized.
	 * 
	 * @return the manifest
	 */
	Optional<StorageReference> getManifest();

	/**
	 * Yields the manifest installed when the node is initialized, also when the
	 * transaction that installed it is not committed yet.
	 * 
	 * @return the manifest
	 */
	Optional<StorageReference> getManifestUncommitted();

	/**
	 * Yields the request that generated the transaction with the given reference.
	 * If this node has some form of commit, then this method is called only when
	 * the transaction has been already committed.
	 * 
	 * @param reference the reference of the transaction
	 * @return the request, if any
	 */
	Optional<TransactionRequest<?>> getRequest(TransactionReference reference);

	/**
	 * Pushes into the store the result of executing a successful Hotmoka request.
	 * This method assumes that the given request was not already present in the store.
	 * 
	 * @param reference the reference of the request
	 * @param request the request of the transaction
	 * @param response the response of the transaction
	 * @throws IOException if an I/O occurred
	 */
	void push(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) throws IOException;

	/**
	 * Pushes into the store the result of executing a successful Hotmoka request.
	 * This method assumes that the given request was already present in the store.
	 * 
	 * @param reference the reference of the request
	 * @param request the request of the transaction
	 * @param response the response of the transaction
	 */
	void replace(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response);

	/**
	 * Pushes into the store the error message resulting from the unsuccessful execution of a Hotmoka request.
	 * 
	 * @param reference the reference of the request
	 * @param request the request of the transaction
	 * @param errorMessage the error message
	 */
	void push(TransactionReference reference, TransactionRequest<?> request, String errorMessage);
}