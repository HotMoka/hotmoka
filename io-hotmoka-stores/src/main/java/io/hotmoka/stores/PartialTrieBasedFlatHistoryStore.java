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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.beans.BeanUnmarshallingContexts;
import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.stores.internal.MarshallableArrayOfTransactionReferences;
import io.hotmoka.xodus.ByteIterable;

/**
 * A historical store of a node. It is a transactional database that keeps
 * the successful responses of the Hotmoka transactions
 * but not their requests nor errors (for this reason it is <i>partial</i>).
 * It keeps histories in a <i>flat</i> way, that is, it is not possible
 * to come back to a previous history after it has been updated. Hence, this
 * store has no the ability of changing its <i>world view</i> by checking out different
 * hashes of its roots. However, it has a compact representation of histories.
 * Hence, it is useful when coming back in time is not relevant for a node.
 * Responses are kept in a Merkle-Patricia trie, supported by JetBrains' Xodus transactional database.
 * 
 * The information kept in this store consists of:
 * 
 * <ul>
 * <li> a trie that maps each Hotmoka request reference to the response computed for that request
 * <li> a map (non-trie) from each storage reference to the transaction references that contribute
 *      to provide values to the fields of the storage object at that reference (its <i>history</i>);
 *      this is used by a node to reconstruct the state of the objects in store
 * <li> miscellaneous control information, such as where the node's manifest
 *      is installed or the current number of commits
 * </ul>
 * 
 * This information is added in store by push methods and accessed through get methods.
 * 
 * This class is meant to be subclassed by specifying where errors and requests are kept.
 */
@ThreadSafe
public abstract class PartialTrieBasedFlatHistoryStore extends PartialTrieBasedStore {

	/**
	 * The Xodus store that holds the history of each storage reference, ie, a list of
	 * transaction references that contribute
	 * to provide values to the fields of the storage object at that reference.
	 */
	private final io.hotmoka.xodus.env.Store storeOfHistory;

    /**
	 * Creates a store. Its roots are not yet initialized. Hence, after this constructor,
	 * a call to {@link #setRootsTo(byte[])} or {@link #setRootsAsCheckedOut()}
	 * should occur, to set the roots of the store.
	 * 
	 * @param dir the path where the database of the store gets created
	 * @param getResponseUncommittedCached a function that yields the transaction response for the given transaction reference, if any, using a cache
	 * @param checkableDepth the number of last commits that can be checked out, in order to
	 *                       change the world-view of the store (see {@link #checkout(byte[])}).
	 *                       This entails that such commits are not garbage-collected, until
	 *                       new commits get created on top and they end up being deeper.
	 *                       This is useful if we expect an old state to be checked out, for
	 *                       instance because a blockchain swaps to another history, but we can
	 *                       assume a reasonable depth for that to happen. Use 0 if the store
	 *                       is not checkable, so that all its successive commits can be immediately
	 *                       garbage-collected as soon as a new commit is created on top
	 *                       (which corresponds to a blockchain that never swaps to a previous
	 *                       state, because it has deterministic finality). Use a negative
	 *                       number if all commits must be checkable (hence garbage-collection
	 *                       is disabled)
	 */
    protected PartialTrieBasedFlatHistoryStore(Function<TransactionReference, Optional<TransactionResponse>> getResponseUncommittedCached, Path dir, long checkableDepth) {
    	super(getResponseUncommittedCached, dir, checkableDepth);

    	AtomicReference<io.hotmoka.xodus.env.Store> storeOfHistory = new AtomicReference<>();
    	env.executeInTransaction(txn -> storeOfHistory.set(env.openStoreWithoutDuplicates("history", txn)));
    	this.storeOfHistory = storeOfHistory.get();
    }

	@Override
	public Stream<TransactionReference> getHistory(StorageReference object) {
		ByteIterable historyAsByteArray;
		ByteIterable ba = intoByteArray(object);

		synchronized (lock) {
			historyAsByteArray = env.computeInReadonlyTransaction(txn -> storeOfHistory.get(txn, ba));
		}

		return historyAsByteArray == null ? Stream.empty() : Stream.of(fromByteArray(historyAsByteArray));
	}

	@Override
	public Stream<TransactionReference> getHistoryUncommitted(StorageReference object) {
		synchronized (lock) {
			if (duringTransaction()) {
				ByteIterable historyAsByteArray = storeOfHistory.get(getCurrentTransaction(), intoByteArray(object));
				return historyAsByteArray == null ? Stream.empty() : Stream.of(fromByteArray(historyAsByteArray));
			}
			else
				return getHistory(object);
		}
	}

	@Override
	protected void setHistory(StorageReference object, Stream<TransactionReference> history) {
		TransactionReference[] references = history.toArray(TransactionReference[]::new);
		ByteIterable historyAsByteArray = ByteIterable.fromBytes(new MarshallableArrayOfTransactionReferences(references).toByteArray());
		ByteIterable objectAsByteArray = intoByteArray(object);
		storeOfHistory.put(getCurrentTransaction(), objectAsByteArray, historyAsByteArray);
	}

	private static ByteIterable intoByteArray(StorageReference reference) throws UncheckedIOException {
		return ByteIterable.fromBytes(reference.toByteArrayWithoutSelector()); // more optimized than a normal marshallable
	}

	private static TransactionReference[] fromByteArray(ByteIterable bytes) throws UncheckedIOException {
		try (var context = BeanUnmarshallingContexts.of(new ByteArrayInputStream(bytes.getBytes()))) {
			return context.readLengthAndArray(TransactionReferences::from, TransactionReference[]::new);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}