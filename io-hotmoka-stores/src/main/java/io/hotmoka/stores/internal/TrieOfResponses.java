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

package io.hotmoka.stores.internal;

import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.beans.marshalling.BeanUnmarshallingContext;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithInstrumentedJar;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.patricia.PatriciaTries;
import io.hotmoka.patricia.api.PatriciaTrie;
import io.hotmoka.xodus.env.Store;
import io.hotmoka.xodus.env.Transaction;

/**
 * A Merkle-Patricia trie that maps references to transaction requests into their responses.
 * It optimizes the trie by sharing identical jars in responses containing an instrumented jar.
 */
public class TrieOfResponses implements PatriciaTrie<TransactionReference, TransactionResponse> {

	private final static Logger logger = Logger.getLogger(TrieOfResponses.class.getName());

	/**
	 * The supporting trie.
	 */
	private final PatriciaTrie<TransactionReference, TransactionResponse> parent;

	/**
	 * The hasher used for the jars in the responses that included a jar.
	 */
	private final Hasher<byte[]> hasherForJars;

	/**
	 * The store of the underlying Patricia trie.
	 */
	private final KeyValueStoreOnXodus keyValueStoreOfResponses;

	/**
	 * Builds a Merkle-Patricia trie that maps references to transaction requests into their responses.
	 * 
	 * @param store the supporting store of the database
	 * @param txn the transaction where updates are reported
	 * @param root the root of the trie to check out; use {@code null} if the trie is empty
	 * @param numberOfCommits the current number of commits already executed on the store; this trie
	 *                        will record which data must be garbage collected (eventually)
	 *                        as result of the store updates performed during that commit; you can pass
	 *                        -1L if the trie is used only for reading
	 */
	public TrieOfResponses(Store store, Transaction txn, byte[] root, long numberOfCommits) {
		try {
			this.keyValueStoreOfResponses = new KeyValueStoreOnXodus(store, txn, root);
			this.hasherForJars = HashingAlgorithms.sha256().getHasher(Function.identity());
			parent = PatriciaTries.of(keyValueStoreOfResponses, HashingAlgorithms.identity32().getHasher(TransactionReference::getHashAsBytes), HashingAlgorithms.sha256(),
					TransactionResponse::from, BeanUnmarshallingContext::new, numberOfCommits);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("unexpected exception", e);
		}
	}

	/**
	 * A function called on each value before being stored in the trie;
	 * the result is actually stored at its place; the goal is
	 * to implement an optimization that shares the jar in the response.
	 * 
	 * @param response the actual response inserted in this trie
	 * @return the response that is put in its place in the parent trie
	 */
	private TransactionResponse writeTransformation(TransactionResponse response) {
		if (response instanceof TransactionResponseWithInstrumentedJar) {
			var trwij = (TransactionResponseWithInstrumentedJar) response;
			byte[] jar = trwij.getInstrumentedJar();
			// we store the jar in the store: if it was already installed before, it gets shared
			byte[] reference = hasherForJars.hash(jar);
			keyValueStoreOfResponses.put(reference, jar);

			// we replace the jar with its hash
			response = replaceJar(trwij, reference);
		}

		return response;
	}

	/**
	 * A function called on each value read from the trie;
	 * the result is actually returned at its place; the goal is to
	 * recover a jar shared with other responses.
	 * 
	 * @param response the response read from the parent trie
	 * @return return the actual response returned by this trie
	 */
	private TransactionResponse readTransformation(TransactionResponse response) {
		if (response instanceof TransactionResponseWithInstrumentedJar) {
			var trwij = (TransactionResponseWithInstrumentedJar) response;

			// we replace the hash of the jar with the actual jar
			try {
				byte[] jar = keyValueStoreOfResponses.get(trwij.getInstrumentedJar());
				response = replaceJar(trwij, jar);
			}
			catch (NoSuchElementException e) {
				logger.log(Level.SEVERE, "cannot find the jar for the transaction response");
				throw e;
			}
		}

		return response;
	}

	private TransactionResponse replaceJar(TransactionResponseWithInstrumentedJar response, byte[] newJar) {
		if (response instanceof JarStoreTransactionSuccessfulResponse) {
			var jstsr = (JarStoreTransactionSuccessfulResponse) response;
			return new JarStoreTransactionSuccessfulResponse
				(newJar, jstsr.getDependencies(), jstsr.getVerificationVersion(), jstsr.getUpdates(),
				jstsr.gasConsumedForCPU, jstsr.gasConsumedForRAM, jstsr.gasConsumedForStorage);
		}
		else if (response instanceof JarStoreInitialTransactionResponse) {
			var jsitr = (JarStoreInitialTransactionResponse) response;
			return new JarStoreInitialTransactionResponse(newJar, jsitr.getDependencies(), jsitr.getVerificationVersion());
		}
		else {
			logger.log(Level.SEVERE, "unexpected response containing jar, of class " + response.getClass().getName());
			return (TransactionResponse) response;
		}
	}

	@Override
	public Optional<TransactionResponse> get(TransactionReference key) {
		return parent.get(key).map(this::readTransformation);
	}

	@Override
	public void put(TransactionReference key, TransactionResponse value) {
		parent.put(key, writeTransformation(value));
	}

	@Override
	public byte[] getRoot() {
		return parent.getRoot();
	}

	@Override
	public void garbageCollect(long commitNumber) {
		parent.garbageCollect(commitNumber);
	}
}