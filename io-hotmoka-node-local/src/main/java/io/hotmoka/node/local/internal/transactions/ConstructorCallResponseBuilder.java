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

package io.hotmoka.node.local.internal.transactions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionResponses;
import io.hotmoka.beans.api.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.NonWhiteListedCallException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.local.internal.NodeInternal;

/**
 * The creator of a response for a transaction that executes a constructor of Takamaka code.
 */
public class ConstructorCallResponseBuilder extends CodeCallResponseBuilder<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse> {
	private final static Logger LOGGER = Logger.getLogger(ConstructorCallResponseBuilder.class.getName());

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public ConstructorCallResponseBuilder(TransactionReference reference, ConstructorCallTransactionRequest request, NodeInternal node) throws TransactionRejectedException {
		super(reference, request, node);
	}

	@Override
	protected final int gasForStoringFailedResponse() {
		BigInteger gas = request.getGasLimit();

		return TransactionResponses.constructorCallFailed
			("placeholder for the name of the exception", "placeholder for the message of the exception", "placeholder for where",
			Stream.empty(), gas, gas, gas, gas).size();
	}

	@Override
	public ConstructorCallTransactionResponse getResponse() throws TransactionRejectedException {
		return new ResponseCreator().create();
	}

	private class ResponseCreator extends CodeCallResponseBuilder<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse>.ResponseCreator {

		/**
		 * The deserialized actual arguments of the constructor.
		 */
		private Object[] deserializedActuals;

		private ResponseCreator() throws TransactionRejectedException {
		}

		@Override
		protected ConstructorCallTransactionResponse body() {
			try {
				init();
				this.deserializedActuals = request.actuals().map(deserializer::deserialize).toArray(Object[]::new);
		
				Object[] deserializedActuals;
				Constructor<?> constructorJVM;
		
				try {
					// we first try to call the constructor with exactly the parameter types explicitly provided
					constructorJVM = getConstructor();
					deserializedActuals = this.deserializedActuals;
				}
				catch (NoSuchMethodException e) {
					// if not found, we try to add the trailing types that characterize the @Entry constructors
					try {
						constructorJVM = getEntryConstructor();
						deserializedActuals = addExtraActualsForFromContract();
					}
					catch (NoSuchMethodException ee) {
						throw e; // the message must be relative to the constructor as the user sees it
					}
				}
		
				ensureWhiteListingOf(constructorJVM, deserializedActuals);
		
				Object result;
				try {
					result = constructorJVM.newInstance(deserializedActuals);
				}
				catch (InvocationTargetException e) {
					Throwable cause = e.getCause();
					if (isCheckedForThrowsExceptions(cause, constructorJVM)) {
						chargeGasForStorageOf(TransactionResponses.constructorCallException(cause.getClass().getName(), cause.getMessage(), where(cause), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
						refundPayerForAllRemainingGas();
						return TransactionResponses.constructorCallException(cause.getClass().getName(), cause.getMessage(), where(cause), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
					}
					else
						throw cause;
				}
		
				chargeGasForStorageOf(TransactionResponses.constructorCallSuccessful
					((StorageReference) serializer.serialize(result), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
				refundPayerForAllRemainingGas();
				return TransactionResponses.constructorCallSuccessful
					((StorageReference) serializer.serialize(result), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
			}
			catch (Throwable t) {
				LOGGER.log(Level.INFO, "constructor call failed", t);
				// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
				resetBalanceOfPayerToInitialValueMinusAllPromisedGas();
				return TransactionResponses.constructorCallFailed(t.getClass().getName(), t.getMessage(), where(t), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
			}
		}

		/**
		 * Resolves the constructor that must be called.
		 * 
		 * @return the constructor
		 * @throws NoSuchMethodException if the constructor could not be found
		 * @throws SecurityException if the constructor could not be accessed
		 * @throws ClassNotFoundException if the class of the constructor or of some parameter cannot be found
		 */
		private Constructor<?> getConstructor() throws ClassNotFoundException, NoSuchMethodException {
			Class<?>[] argTypes = formalsAsClass();

			return classLoader.resolveConstructor(request.constructor.getDefiningClass().getName(), argTypes)
				.orElseThrow(() -> new NoSuchMethodException(request.constructor.toString()));
		}

		/**
		 * Resolves the constructor that must be called, assuming that it is an entry.
		 * 
		 * @return the constructor
		 * @throws NoSuchMethodException if the constructor could not be found
		 * @throws SecurityException if the constructor could not be accessed
		 * @throws ClassNotFoundException if the class of the constructor or of some parameter cannot be found
		 */
		private Constructor<?> getEntryConstructor() throws ClassNotFoundException, NoSuchMethodException {
			Class<?>[] argTypes = formalsAsClassForFromContract();

			return classLoader.resolveConstructor(request.constructor.getDefiningClass().getName(), argTypes)
				.orElseThrow(() -> new NoSuchMethodException(request.constructor.toString()));
		}

		/**
		 * Adds to the actual parameters the implicit actuals that are passed
		 * to {@link io.takamaka.code.lang.FromContract} methods or constructors. They are the caller of
		 * the method or constructor and {@code null} for the dummy argument.
		 * 
		 * @return the resulting actual parameters
		 */
		private Object[] addExtraActualsForFromContract() {
			int al = deserializedActuals.length;
			Object[] result = new Object[al + 2];
			System.arraycopy(deserializedActuals, 0, result, 0, al);
			result[al] = getDeserializedCaller();
			result[al + 1] = null; // Dummy is not used
		
			return result;
		}

		/**
		 * Checks that the constructor called by this transaction is
		 * white-listed and its white-listing proof-obligations hold.
		 * 
		 * @param executable the constructor
		 * @param actuals the actual arguments passed to {@code executable}
		 */
		private void ensureWhiteListingOf(Constructor<?> executable, Object[] actuals) {
			Optional<Constructor<?>> model = classLoader.getWhiteListingWizard().whiteListingModelOf(executable);
			if (model.isEmpty())
				throw new NonWhiteListedCallException("illegal call to non-white-listed constructor of " + request.constructor.getDefiningClass());

			Annotation[][] anns = model.get().getParameterAnnotations();
			for (int pos = 0; pos < anns.length; pos++)
				checkWhiteListingProofObligations(model.get().getName(), actuals[pos], anns[pos]);
		}

		@Override
		protected final Stream<Object> getDeserializedActuals() {
			return Stream.of(deserializedActuals);
		}
	}
}