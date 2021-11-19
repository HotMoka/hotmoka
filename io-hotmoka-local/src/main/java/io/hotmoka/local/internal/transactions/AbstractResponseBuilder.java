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

package io.hotmoka.local.internal.transactions;

import static io.hotmoka.local.internal.runtime.Runtime.responseCreators;

import java.math.BigInteger;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.SystemTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.local.EngineClassLoader;
import io.hotmoka.local.ResponseBuilder;
import io.hotmoka.local.internal.Deserializer;
import io.hotmoka.local.internal.EngineClassLoaderImpl;
import io.hotmoka.local.internal.NodeInternal;
import io.hotmoka.local.internal.StorageTypeToClass;
import io.hotmoka.local.internal.UpdatesExtractorFromRAM;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.nodes.DeserializationError;
import io.hotmoka.nodes.OutOfGasError;

/**
 * A generic implementation of the creator of a response.
 *
 * @param <Request> the type of the request of the transaction
 * @param <Response> the type of the response of the transaction
 */
public abstract class AbstractResponseBuilder<Request extends TransactionRequest<Response>, Response extends TransactionResponse> implements ResponseBuilder<Request, Response> {

	/**
	 * The HotMoka node that is creating the response.
	 */
	public final NodeInternal node;

	/**
	 * The object that translates storage types into their run-time class tag.
	 */
	public final StorageTypeToClass storageTypeToClass;

	/**
	 * The class loader used for the transaction.
	 */
	public final EngineClassLoader classLoader;

	/**
	 * The request of the transaction.
	 */
	protected final Request request;

	/**
	 * The reference used to refer to the transaction generated by the request.
	 */
	protected final TransactionReference reference;

	/**
	 * The consensus parameters when this builder was created.
	 */
	protected final ConsensusParams consensus;

	/**
	 * Creates the builder of a response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request for which the response is being built
	 * @param node the node that is creating the response
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	protected AbstractResponseBuilder(TransactionReference reference, Request request, NodeInternal node) throws TransactionRejectedException {
		try {
			this.request = request;
			this.reference = reference;
			this.node = node;
			this.consensus = node.getCaches().getConsensusParams();
			this.classLoader = mkClassLoader();
			this.storageTypeToClass = new StorageTypeToClass(this);
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	@Override
	public final Request getRequest() {
		return request;
	}

	@Override
	public final EngineClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	public final void replaceReverifiedResponses() {
		((EngineClassLoaderImpl) classLoader).replaceReverifiedResponses();
	}

	/**
	 * Creates the class loader for computing the response.
	 * 
	 * @return the class loader
	 */
	protected abstract EngineClassLoader mkClassLoader();

	/**
	 * Wraps the given throwable in a {@link io.hotmoka.beans.TransactionException}, if it not
	 * already an instance of that exception.
	 * 
	 * @param t the throwable to wrap
	 * @return the wrapped or original exception
	 */
	protected static TransactionRejectedException wrapAsTransactionRejectedException(Throwable t) {
		return t instanceof TransactionRejectedException ? (TransactionRejectedException) t : new TransactionRejectedException(t);
	}

	/**
	 * The creator of a response. Its body runs in a thread, so that the
	 * {@linkplain io.hotmoka.local.internal.runtime.Runtime} class
	 * can recover it from its thread-local table.
	 */
	public abstract class ResponseCreator {

		/**
		 * The object that deserializes storage objects into RAM values.
		 */
		protected final Deserializer deserializer;

		/**
		 * The object that can be used to extract the updates to a set of storage objects,
		 * induced by the run of the transaction.
		 */
		protected final UpdatesExtractorFromRAM updatesExtractor;

		/**
		 * The time of execution of the transaction.
		 */
		private final long now;

		/**
		 * The counter for the next storage object created during the transaction.
		 */
		private BigInteger nextProgressive = BigInteger.ZERO;

		protected ResponseCreator() throws TransactionRejectedException {
			try {
				this.deserializer = new Deserializer(AbstractResponseBuilder.this, node.getStoreUtilities());
				this.updatesExtractor = new UpdatesExtractorFromRAM(AbstractResponseBuilder.this);
				this.now = node.getStore().getNow();
			}
			catch (Throwable t) {
				throw new TransactionRejectedException(t);
			}
		}

		public final Response create() throws TransactionRejectedException {
			try {
				return node.submit(new TakamakaCallable(this::body)).get();
			}
			catch (ExecutionException e) {
				throw wrapAsTransactionRejectedException(e.getCause());
			}
			catch (Throwable t) {
				throw wrapAsTransactionRejectedException(t);
			}
		}

		/**
		 * The body of the creation of the response.
		 * 
		 * @return the response
		 */
		protected abstract Response body();

		/**
		 * Yields the UTC time when the transaction is being run.
		 * This might be for instance the time of creation of a block where the transaction
		 * will be stored, but the detail is left to the implementation.
		 * 
		 * @return the UTC time, as returned by {@link java.lang.System#currentTimeMillis()}
		 */
		public final long now() {
			return now;
		}

		/**
		 * Determines if the execution was started by the node itself.
		 * This is always false if the node has no notion of commit.
		 * If the execution has been started by a user request, this will
		 * always be false.
		 * 
		 * @return true if and only if that condition occurs
		 */
		public final boolean isSystemCall() {
			return request instanceof SystemTransactionRequest;
		}

		/**
		 * Determines if the response can call the mint and burn methods of the
		 * externally owned account, with the given caller.
		 * 
		 * @return true if and only if that condition holds
		 */
		public final boolean canCallMintBurnFromGamete(Object caller) {
			StorageReference storageReferenceOfCaller = classLoader.getStorageReferenceOf(caller);
			return consensus.allowsMintBurnFromGamete && node.getCaches().getGamete().filter(storageReferenceOfCaller::equals).isPresent();
		}

		/**
		 * Takes note of the given event, emitted during this execution.
		 * 
		 * @param event the event
		 */
		public abstract void event(Object event);

		/**
		 * Runs a given piece of code with a subset of the available gas.
		 * It first charges the given amount of gas. Then runs the code
		 * with the charged gas only. At its end, the remaining gas is added
		 * to the available gas to continue the computation.
		 * 
		 * @param amount the amount of gas provided to the code
		 * @param what the code to run
		 * @return the result of the execution of the code
		 * @throws OutOfGasError if there is not enough gas
		 * @throws Exception if the code runs into this exception
		 */
		public abstract <T> T withGas(BigInteger amount, Callable<T> what) throws Exception;

		/**
		 * Decreases the available gas by the given amount, for CPU execution.
		 * 
		 * @param amount the amount of gas to consume
		 */
		public abstract void chargeGasForCPU(BigInteger amount);

		/**
		 * Decreases the available gas by the given amount, for RAM execution.
		 * 
		 * @param amount the amount of gas to consume
		 */
		public abstract void chargeGasForRAM(BigInteger amount);

		/**
		 * Yields the latest value for the given field of the object with the given storage reference.
		 * The field is not {@code final}. Conceptually, this method looks for the value of the field
		 * in the last transaction where the reference was updated.
		 * 
		 * @param object the storage reference
		 * @param field the field
		 * @return the value of the field
		 */
		public final Object deserializeLastUpdateFor(StorageReference object, FieldSignature field) {
			UpdateOfField update = node.getStoreUtilities().getLastUpdateToFieldUncommitted(object, field)
				.orElseThrow(() -> new DeserializationError("did not find the last update for " + field + " of " + object));

			return deserializer.deserialize(update.getValue());
		}

		/**
		 * Yields the latest value for the given field of the object with the given storage reference.
		 * The field is {@code final}. Conceptually, this method looks for the value of the field
		 * in the transaction where the reference was created.
		 * 
		 * @param object the storage reference
		 * @param field the field
		 * @return the value of the field
		 */
		public final Object deserializeLastUpdateForFinal(StorageReference object, FieldSignature field) {
			UpdateOfField update = node.getStoreUtilities().getLastUpdateToFinalFieldUncommitted(object, field)
				.orElseThrow(() -> new DeserializationError("did not find the last update for " + field + " of " + object));

			return deserializer.deserialize(update.getValue());
		}

		/**
		 * Yields the next storage reference for the current transaction.
		 * This can be used to associate a storage reference to each new
		 * storage object created during a transaction.
		 * 
		 * @return the next storage reference
		 */
		public final StorageReference getNextStorageReference() {
			BigInteger result = nextProgressive;
			nextProgressive = nextProgressive.add(BigInteger.ONE);
			return new StorageReference(reference, result);
		}

		/**
		 * Yields the class loader used for the transaction being created.
		 * 
		 * @return the class loader
		 */
		public final EngineClassLoaderImpl getClassLoader() {
			return (EngineClassLoaderImpl) classLoader;
		}

		/**
		 * Yields the updates extracted from the given storage objects and from the objects
		 * reachable from them, recursively.
		 * 
		 * @param objects the storage objects whose updates must be computed (for them and
		 *                for the objects recursively reachable from them)
		 * @return the updates, sorted
		 */
		protected final Stream<Update> extractUpdatesFrom(Stream<Object> objects) {
			return updatesExtractor.extractUpdatesFrom(objects);
		}

		/**
		 * A task that executes Takamaka code as part of this transaction.
		 * It sets the response creator in the thread-local of the runtime.
		 */
		private final class TakamakaCallable implements Callable<Response> {
			private final Callable<Response> body;

			private TakamakaCallable(Callable<Response> body) {
				this.body = body;
			}

			@Override
			public Response call() throws Exception {
				try {
					responseCreators.set(ResponseCreator.this);
					return body.call();
				}
				finally {
					responseCreators.remove();
				}
			}
		}
	}
}