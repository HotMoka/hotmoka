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

package io.hotmoka.node.local.api;

import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;

/**
 * The store of a node. It keeps information about the state of the objects created
 * by the requests executed by the node. A store is external to the node and, typically, only
 * its hash is held in the node, if consensus is needed. Stores must be thread-safe, since they can
 * be used concurrently for executing more requests.
 */
@Immutable
public interface Store<S extends Store<S, T>, T extends StoreTransaction<S, T>> extends AutoCloseable {

	/**
	 * Yields the request that generated the transaction with the given reference.
	 * If this node has some form of commit, then this method is called only when
	 * the transaction has been already committed.
	 * 
	 * @param reference the reference of the transaction
	 * @return the request
	 */
	TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException, StoreException;

	/**
	 * Yields the response of the transaction having the given reference.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response
	 */
	TransactionResponse getResponse(TransactionReference reference) throws UnknownReferenceException, StoreException;

	/**
	 * Yields the history of the given object, that is, the references to the transactions
	 * that can be used to reconstruct the current values of its fields.
	 * 
	 * @param object the reference of the object
	 * @return the history
	 * @throws StoreException if the store is not able to perform the operation
	 */
	Stream<TransactionReference> getHistory(StorageReference object) throws UnknownReferenceException, StoreException;

	/**
	 * Yields the manifest installed when the node is initialized.
	 * 
	 * @return the manifest
	 * @throws StoreException if the store is not able to complete the operation correctly
	 */
	Optional<StorageReference> getManifest() throws StoreException;

	ConsensusConfig<?,?> getConfig();

	/**
	 * Checks that the given transaction request is valid.
	 * 
	 * @param request the request
	 * @throws TransactionRejectedException if the request is not valid
	 * @throws NodeException 
	 */
	void checkTransaction(TransactionRequest<?> request) throws TransactionRejectedException, StoreException;

	/**
	 * Starts a transaction. Instance updates during the transaction are saved
	 * in the supporting database if the transaction will later be committed.
	 */
	T beginTransaction(long now) throws StoreException;

	/**
	 * Starts a view transaction. It assumes the time of the transaction to be the
	 * current time UTC and the maximal gas allowed for the transaction to be
	 * that given in the local configuration of the node:
	 * {@link LocalNodeConfig#getMaxGasPerViewTransaction()}.
	 */
	T beginViewTransaction() throws StoreException;

	@Override
	void close() throws StoreException, InterruptedException;
}