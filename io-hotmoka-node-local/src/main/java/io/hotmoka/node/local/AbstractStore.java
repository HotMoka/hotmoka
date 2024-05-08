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

package io.hotmoka.node.local;

import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalLong;

import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.Store;

public abstract class AbstractStore<T extends AbstractStore<T, N>, N extends AbstractLocalNode<N, ?, T>> implements Store<T, N> {

	/**
	 * Cached recent requests that have had their signature checked.
	 * This can be shared across distinct stores since valid signatures
	 * remain valid over time.
	 */
	final LRUCache<SignedTransactionRequest<?>, Boolean> checkedSignatures; // TODO: possibly store by reference

	/**
	 * The cache for the class loaders. This can be shared across distinct stores since
	 * jars installed in store remain valid over time.
	 */
	final LRUCache<TransactionReference, EngineClassLoader> classLoaders;

	/**
	 * The node for which this store has been created.
	 */
	private final N node;

	/**
	 * The current consensus configuration in this store. This information could be recovered from the store
	 * itself, but this field is used for caching. The consensus configuration might be missing if the
	 * store has been checked out to a specific root and consequently this cache has not been recomputed yet.
	 */
	final ConsensusConfig<?,?> consensus;

	/**
	 * The current gas price in this store. This information could be recovered from the store
	 * itself, but this field is used for caching. The gas price might be missing if the
	 * node is not initialized yet.
	 */
	final Optional<BigInteger> gasPrice;

	/**
	 * The current inflation in this store. This information could be recovered from the store
	 * itself, but this field is used for caching. The inflation might be missing if the
	 * node is not initialized yet.
	 */
	final OptionalLong inflation;

	final Hasher<TransactionRequest<?>> hasher;

	protected AbstractStore(N node, ConsensusConfig<?,?> consensus, Hasher<TransactionRequest<?>> hasher) {
		this.node = node;
		this.hasher = hasher;
		this.checkedSignatures = new LRUCache<>(100, 1000);
		this.classLoaders = new LRUCache<>(100, 1000);
		this.consensus = consensus;
		this.gasPrice = Optional.empty();
		this.inflation = OptionalLong.empty();
	}

	protected AbstractStore(AbstractStore<T, N> toClone) {
		this.node = toClone.node;
		this.hasher = toClone.hasher;
		this.checkedSignatures = toClone.checkedSignatures;
		this.classLoaders = toClone.classLoaders;
		this.consensus = toClone.consensus;
		this.gasPrice = toClone.gasPrice;
		this.inflation = toClone.inflation;
	}

	protected AbstractStore(AbstractStore<T, N> toClone, ConsensusConfig<?,?> consensus, Optional<BigInteger> gasPrice, OptionalLong inflation) {
		this.node = toClone.node;
		this.hasher = toClone.hasher;
		this.checkedSignatures = toClone.checkedSignatures;
		this.classLoaders = toClone.classLoaders;
		this.consensus = consensus;
		this.gasPrice = gasPrice;
		this.inflation = inflation;
	}

	@Override
	public final N getNode() {
		return node;
	}

	public ConsensusConfig<?,?> getConfig() {
		return consensus;
	}
}