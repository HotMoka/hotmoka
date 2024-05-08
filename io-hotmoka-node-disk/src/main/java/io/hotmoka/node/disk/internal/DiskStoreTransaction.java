package io.hotmoka.node.disk.internal;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.AbstractStoreTransaction;
import io.hotmoka.node.local.api.StoreException;

public class DiskStoreTransaction extends AbstractStoreTransaction<DiskStore> {
	private final ConcurrentMap<TransactionReference, TransactionRequest<?>> requests = new ConcurrentHashMap<>();
	private final ConcurrentMap<TransactionReference, TransactionResponse> responses = new ConcurrentHashMap<>();

	/**
	 * The histories of the objects created in blockchain. In a real implementation, this must
	 * be stored in a persistent state.
	 */
	private final ConcurrentMap<StorageReference, TransactionReference[]> histories = new ConcurrentHashMap<>();

	/**
	 * The errors generated by each transaction (if any). In a real implementation, this must
	 * be stored in a persistent memory such as a blockchain.
	 */
	private final ConcurrentMap<TransactionReference, String> errors = new ConcurrentHashMap<>();

	/**
	 * The storage reference of the manifest stored inside the node, if any.
	 */
	private final AtomicReference<StorageReference> manifest = new AtomicReference<>();

	public DiskStoreTransaction(DiskStore store) {
		super(store);
	}

	@Override
	public long getNow() {
		return System.currentTimeMillis();
	}

	public boolean isJustStore() {
		return requests.isEmpty() && responses.isEmpty() && histories.isEmpty();
	}

	@Override
	public Optional<TransactionResponse> getResponseUncommitted(TransactionReference reference) {
		var uncommittedResponse = responses.get(reference);
		if (uncommittedResponse != null)
			return Optional.of(uncommittedResponse);
		else
			return getStore().getResponse(reference);
	}

	@Override
	public Stream<TransactionReference> getHistoryUncommitted(StorageReference object) throws StoreException {
		var uncommittedHistory = histories.get(object);
		if (uncommittedHistory != null)
			return Stream.of(uncommittedHistory);
		else
			return getStore().getHistory(object);
	}

	@Override
	public Optional<StorageReference> getManifestUncommitted() {
		var uncommittedManifest = manifest.get();
		if (uncommittedManifest != null)
			return Optional.of(uncommittedManifest);
		else
			return getStore().getManifest();
	}

	@Override
	public DiskStore commit() throws StoreException {
		return new DiskStore(getStore(), getCheckedSignatures(), getClassLoaders(), getConfigUncommitted(), getGasPriceUncommitted(), getInflationUncommitted(), requests, responses, histories, errors, Optional.ofNullable(manifest.get()));
	}

	@Override
	public void abort() {}

	@Override
	protected void setRequest(TransactionReference reference, TransactionRequest<?> request) {
		requests.put(reference, request);
	}

	@Override
	protected void setResponse(TransactionReference reference, TransactionResponse response) {
		responses.put(reference, response);
	}

	@Override
	protected void setError(TransactionReference reference, String error) {
		errors.put(reference, error);
	}

	@Override
	protected void setHistory(StorageReference object, Stream<TransactionReference> history) {
		histories.put(object, history.toArray(TransactionReference[]::new));
	}

	@Override
	protected void setManifest(StorageReference manifest) {
		this.manifest.set(manifest);
	}
}