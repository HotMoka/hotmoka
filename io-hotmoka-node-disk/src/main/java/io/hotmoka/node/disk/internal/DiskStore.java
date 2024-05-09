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

package io.hotmoka.node.disk.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.node.NodeMarshallingContexts;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.AbstractStore;
import io.hotmoka.node.local.LRUCache;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.StoreException;

/**
 * The store of the memory blockchain. It is not transactional and just writes
 * everything immediately into files. It keeps responses into persistent memory,
 * while the histories are kept in RAM.
 */
@Immutable
class DiskStore extends AbstractStore<DiskStore, DiskNodeImpl> {

	/**
	 * The path where the database of the store gets created.
	 */
	private final Path dir;
	private final ConcurrentMap<TransactionReference, TransactionRequest<?>> requests;
	private final ConcurrentMap<TransactionReference, TransactionResponse> responses;

	/**
	 * The histories of the objects created in blockchain. In a real implementation, this must
	 * be stored in a persistent state.
	 */
	private final ConcurrentMap<StorageReference, TransactionReference[]> histories;

	/**
	 * The errors generated by each transaction (if any). In a real implementation, this must
	 * be stored in a persistent memory such as a blockchain.
	 */
	private final ConcurrentMap<TransactionReference, String> errors;

	/**
	 * The storage reference of the manifest stored inside the node, if any.
	 */
	private final AtomicReference<StorageReference> manifest;

	private final AtomicInteger blockNumber;

	/**
     * Creates a state for a node.
     */
    DiskStore(ExecutorService executors, ConsensusConfig<?,?> consensus, LocalNodeConfig<?,?> config, Hasher<TransactionRequest<?>> hasher) {
    	super(executors, consensus, config, hasher);

    	this.dir = config.getDir();
    	this.requests = new ConcurrentHashMap<>();
    	this.responses = new ConcurrentHashMap<>();
    	this.histories = new ConcurrentHashMap<>();
    	this.errors = new ConcurrentHashMap<>();
    	this.manifest = new AtomicReference<>();
    	this.blockNumber = new AtomicInteger(0);
    }

    DiskStore(DiskStore toClone, LRUCache<TransactionReference, Boolean> checkedSignatures, LRUCache<TransactionReference, EngineClassLoader> classLoaders,
    		ConsensusConfig<?,?> consensus, Optional<BigInteger> gasPrice, OptionalLong inflation, Map<TransactionReference, TransactionRequest<?>> addedRequests,
    		Map<TransactionReference, TransactionResponse> addedResponses,
    		Map<StorageReference, TransactionReference[]> addedHistories,
    		Map<TransactionReference, String> addedErrors,
    		Optional<StorageReference> addedManifest) throws StoreException {

    	super(toClone, checkedSignatures, classLoaders, consensus, gasPrice, inflation);

    	this.dir = toClone.dir;
    	this.requests = new ConcurrentHashMap<>(toClone.requests);
    	this.responses = new ConcurrentHashMap<>(toClone.responses);
    	this.histories = new ConcurrentHashMap<>(toClone.histories);
    	this.errors = new ConcurrentHashMap<>(toClone.errors);
    	this.manifest = new AtomicReference<>(toClone.manifest.get());
    	this.blockNumber = new AtomicInteger(addedRequests.isEmpty() ? toClone.blockNumber.get() : toClone.blockNumber.get() + 1);

    	addedManifest.ifPresent(manifest::set);

		for (var entry: addedErrors.entrySet())
			errors.put(entry.getKey(), entry.getValue());

		int progressive = 0;
		for (var entry: addedRequests.entrySet()) {
			setRequest(progressive++, entry.getKey(), entry.getValue());
		}

		progressive = 0;
		for (var entry: addedResponses.entrySet())
			setResponse(progressive++, entry.getKey(), entry.getValue());

		for (var entry: addedHistories.entrySet())
			histories.put(entry.getKey(), entry.getValue());
    }

    @Override
    public Optional<TransactionResponse> getResponse(TransactionReference reference) {
    	return Optional.ofNullable(responses.get(reference));
    }

	@Override
	public Optional<String> getError(TransactionReference reference) {
		return Optional.ofNullable(errors.get(reference));
	}

	@Override
	public Stream<TransactionReference> getHistory(StorageReference object) {
		TransactionReference[] history = histories.get(object);
		return history == null ? Stream.empty() : Stream.of(history);
	}

	@Override
	public Optional<StorageReference> getManifest() {
		return Optional.ofNullable(manifest.get());
	}

	@Override
	public Optional<TransactionRequest<?>> getRequest(TransactionReference reference) {
		return Optional.ofNullable(requests.get(reference));
	}

	@Override
	protected DiskStoreTransaction beginTransaction(ExecutorService executors, ConsensusConfig<?,?> consensus, long now) {
		return new DiskStoreTransaction(this, executors, consensus, now);
	}

	private void setRequest(int progressive, TransactionReference reference, TransactionRequest<?> request) throws StoreException {
		requests.put(reference, request);

		try {
			Path requestPath = getPathFor(progressive, reference, "request");
			Path parent = requestPath.getParent();
			ensureDeleted(parent);
			Files.createDirectories(parent);
	
			Files.writeString(getPathFor(progressive, reference, "request.txt"), request.toString(), StandardCharsets.UTF_8);
	
			try (var context = NodeMarshallingContexts.of(Files.newOutputStream(requestPath))) {
				request.into(context);
			}
		}
		catch (IOException e) {
			throw new StoreException(e);
		}
	}

	private void setResponse(int progressive, TransactionReference reference, TransactionResponse response) throws StoreException {
		responses.put(reference, response);

		try {
			Path responsePath = getPathFor(progressive, reference, "response");
			Path parent = responsePath.getParent();
			Files.createDirectories(parent);

			Files.writeString(getPathFor(progressive, reference, "response.txt"), response.toString(), StandardCharsets.UTF_8);

			try (var context = NodeMarshallingContexts.of(Files.newOutputStream(responsePath))) {
				response.into(context);
			}
		}
		catch (IOException e) {
			throw new StoreException(e);
		}
	}

	/**
	 * Yields the path for a file inside the directory for the given transaction.
	 * 
	 * @param reference the transaction reference
	 * @param name the name of the file
	 * @return the resulting path
	 * @throws FileNotFoundException if the reference is unknown
	 */
	private Path getPathFor(int progressive, TransactionReference reference, String name) throws FileNotFoundException {
		return dir.resolve("b" + blockNumber.get()).resolve(progressive + "-" + reference).resolve(name);
	}

	/**
	 * Deletes the given directory, recursively, if it exists.
	 * 
	 * @param dir the directory
	 * @throws IOException if a disk error occurs
	 */
	private static void ensureDeleted(Path dir) throws IOException {
		if (Files.exists(dir))
			Files.walk(dir)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
	}

	@Override
	public void close() {}
}