package io.hotmoka.nodes;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A node of the Hotmoka network, that provides the storage
 * facilities for the execution of Takamaka code.
 */
public interface Node extends AutoCloseable {

	/**
	 * Yields the response generated by the transaction with the given reference.
	 * 
	 * @param transactionReference the reference to the transaction
	 * @return the response
	 * @throws Exception if the response cannot be found
	 */
	TransactionResponse getResponseAt(TransactionReference transactionReference) throws Exception;

	/**
	 * Yields the transaction reference that has been generated for a request
	 * whose posting got the given identifier. This method will block
	 * until the transaction reference is available or a timeout expires.
	 * 
	 * @param id the identifier
	 * @return the transaction reference
	 * @throws TimeoutException if the timeout expired but the transaction reference is not available yet
	 * @throws InterruptedException if the current thread was interrupted while waiting for the transaction reference
	 * @throws Exception if the transaction reference cannot be retrieved or if the execution of the
	 *                   transaction led into an exception or if a timeout expires
	 */
	TransactionReference pollTransactionReference(String id) throws TimeoutException, InterruptedException, Exception;

	/**
	 * Yields the most recent eager updates for the given storage reference.
	 * 
	 * @param storageReference the storage reference
	 * @param chargeForCPU a function called to charge CPU costs
	 * @param eagerFields a function that extracts the eager instance fields of a class, also inherited, given the name of the class
	 * @return the updates; these include the class tag update for the reference
	 * @throws Exception if the updates cannot be found
	 */
	Stream<Update> getLastEagerUpdatesFor(StorageReference storageReference, Consumer<BigInteger> chargeForCPU, Function<String, Stream<Field>> eagerFields) throws Exception;

	/**
	 * Yields the most recent update for the given non-{@code final} field,
	 * of lazy type, of the object with the given storage reference.
	 * 
	 * @param storageReference the storage reference
	 * @param field the field whose update is being looked for
	 * @param chargeForCPU a function called to charge CPU costs
	 * @return the update
	 * @throws Exception if the update could not be found
	 */
	UpdateOfField getLastLazyUpdateToNonFinalFieldOf(StorageReference storageReference, FieldSignature field, Consumer<BigInteger> chargeForCPU) throws Exception;

	/**
	 * Yields the most recent update for the given {@code final} field,
	 * of lazy type, of the object with the given storage reference.
	 * Its implementation can be identical to
	 * that of {@link #getLastLazyUpdateToNonFinalFieldOf(StorageReference, FieldSignature, Consumer<BigInteger>)},
	 * or instead exploit the fact that the field is {@code final}, for an optimized look-up.
	 * 
	 * @param storageReference the storage reference
	 * @param field the field whose update is being looked for
	 * @param chargeForCPU a function called to charge CPU costs
	 * @return the update
	 * @throws Exception if the update could not be found
	 */
	UpdateOfField getLastLazyUpdateToFinalFieldOf(StorageReference storageReference, FieldSignature field, Consumer<BigInteger> chargeForCPU) throws Exception;

	/**
	 * Yields the class tag of the object with the given storage reference.
	 * 
	 * @param storageReference the storage reference
	 * @param chargeForCPU a function called to charge CPU costs
	 * @return the class tag
	 * @throws Exception if the class tag could not be found
	 */
	ClassTag getClassTagOf(StorageReference storageReference, Consumer<BigInteger> chargeForCPU) throws Exception;

	/**
	 * Yields the UTC time that must be used for a transaction, if it is executed
	 * with this node in this moment.
	 * 
	 * @return the UTC time, as returned by {@link java.lang.System#currentTimeMillis()}
	 */
	long getNow() throws Exception;

	/**
	 * Yields the gas cost model of this node.
	 * 
	 * @return the gas cost model
	 */
	GasCostModel getGasCostModel();

	/**
	 * Expands the store of this node with a transaction that
	 * installs a jar in it. This transaction can only occur during initialization
	 * of the node. It has no caller and requires no gas. The goal is to install, in the
	 * node, some basic jars that are likely needed as dependencies by future jars.
	 * For instance, the jar containing the basic contract classes.
	 * This installation have special privileges, such as that of installing
	 * packages in {@code io.takamaka.code.lang.*}.
	 * 
	 * @param request the transaction request
	 * @return the reference to the transaction that can be used to refer to the jar in a class path or as future dependency of other jars
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 */
	TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException;

	/**
	 * Expands the store of this node with a transaction that creates a gamete, that is,
	 * an externally owned contract with the given initial amount of coins.
	 * This transaction can only occur during initialization of the node. It has
	 * no caller and requires no gas.
	 * 
	 * @param request the transaction request
	 * @return the reference to the freshly created gamete
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 */
	StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException;

	/**
	 * Expands the store of this blockchain with a transaction that creates a red/green gamete, that is,
	 * a red/green externally owned contract with the given initial amount of coins.
	 * This transaction can only occur during initialization of the node. It has
	 * no caller and requires no gas.
	 * 
	 * @param request the transaction request
	 * @return the reference to the freshly created gamete
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 */
	StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionRejectedException;

	/**
	 * Expands the store of this node with a transaction that installs a jar in it.
	 * 
	 * @param request the transaction request
	 * @return the reference to the transaction, that can be used to refer to the jar in a class path or as future dependency of other jars
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 * @throws TransactionException if the transaction could not be executed and the store of the node has been expanded with a failed transaction
	 */
	TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException;

	/**
	 * Expands this node's store with a transaction that runs a constructor of a class.
	 * 
	 * @param request the request of the transaction
	 * @return the created object, if the constructor was successfully executed, without exception
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 * @throws CodeExecutionException if the transaction could be executed and the node has been expanded with a failed transaction,
	 *                                because of an exception in the user code in blockchain, that is allowed to be thrown by the constructor
	 * @throws TransactionException if the transaction could be executed and the node has been expanded with a failed transaction,
	 *                              because of an exception outside the user code in blockchain, or not allowed to be thrown by the constructor
	 */
	StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException;

	/**
	 * Expands this node's store with a transaction that runs an instance method of an object already in this node's store.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 * @throws CodeExecutionException if the transaction could be executed and the node has been expanded with a failed transaction,
	 *                                because of an exception in the user code in blockchain, that is allowed to be thrown by the method
	 * @throws TransactionException if the transaction could be executed and the node has been expanded with a failed transaction,
	 *                              because of an exception outside the user code in blockchain, or not allowed to be thrown by the method
	 */
	StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException;

	/**
	 * Expands this node's store with a transaction that runs a static method of a class in this node.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 * @throws CodeExecutionException if the transaction could be executed and the node has been expanded with a failed transaction,
	 *                                because of an exception in the user code in blockchain, that is allowed to be thrown by the method
	 * @throws TransactionException if the transaction could be executed and the node has been expanded with a failed transaction,
	 *                              because of an exception outside the user code in blockchain, or not allowed to be thrown by the method
	 */
	StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException;

	/**
	 * Runs an instance {@code @@View} method of an object already in this node's store.
	 * The node's store is not expanded, since the execution of the method has no side-effects.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception
	 * @throws TransactionRejectedException if the transaction could not be executed
	 * @throws CodeExecutionException if the transaction could be executed but led to an exception in the user code in blockchain,
	 *                                that is allowed to be thrown by the method
	 * @throws TransactionException if the transaction could be executed but led to an exception outside the user code in blockchain,
	 *                              or that is not allowed to be thrown by the method
	 */
	StorageValue runViewInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException;

	/**
	 * Runs a static {@code @@View} method of a class in this node.
	 * The node's store is not expanded, since the execution of the method has no side-effects.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception
	 * @throws TransactionRejectedException if the transaction could not be executed
	 * @throws CodeExecutionException if the transaction could be executed but led to an exception in the user code in blockchain,
	 *                                that is allowed to be thrown by the method
	 * @throws TransactionException if the transaction could be executed but led to an exception outside the user code in blockchain,
	 *                              or that is not allowed to be thrown by the method
	 */
	StorageValue runViewStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException;

	/**
	 * Posts a transaction that expands the store of this node with a transaction that installs a jar in it.
	 * 
	 * @param request the transaction request
	 * @return the future holding the reference to the transaction where the jar has been installed
	 * @throws TransactionRejectedException if the transaction could not be posted
	 */
	JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException ;

	/**
	 * Posts a transaction that runs a constructor of a class in this node.
	 * 
	 * @param request the request of the transaction
	 * @return the future holding the result of the computation
	 * @throws TransactionRejectedException if the transaction could not be posted
	 */
	CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException;

	/**
	 * Posts a transaction that runs an instance method of an object already in this node's store.
	 * 
	 * @param request the transaction request
	 * @return the future holding the result of the transaction
	 * @throws TransactionRejectedException if the transaction could not be posted
	 */
	CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException;

	/**
	 * Posts a request that runs a static method of a class in this node.
	 * 
	 * @param request the transaction request
	 * @return the future holding the result of the transaction
	 * @throws TransactionRejectedException if the transaction could not be posted
	 */
	CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException;

	/**
	 * The future of a transaction that executes code in blockchain.
	 * 
	 * @param <V> the type of the value computed by the transaction
	 */
	interface CodeSupplier<V extends StorageValue> {
	
		/**
	     * Waits if necessary for the transaction to complete, and then retrieves its result.
	     *
	     * @return the computed result of the transaction
	     * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	     * @throws CodeExecutionException if the transaction could be executed but led to an exception in the user code in blockchain,
	     *                                that is allowed to be thrown by the constructor
	     * @throws TransactionException if the transaction could not be executed and the store of the node has been expanded with a failed transaction
	     */
	    V get() throws TransactionRejectedException, TransactionException, CodeExecutionException;
	
	    /**
	     * Yields an identifier of the transaction, that can be used for polling its result.
	     * This can be, for instance, a hash of the transaction.
	     * 
	     * @return the identifier
	     * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	     */
	    String id() throws TransactionRejectedException;
	}

	 /**
	 * The future of a transaction that stores a jar in blockchain.
	 */
	interface JarSupplier {

		/**
	     * Waits if necessary for the transaction to complete, and then retrieves its result.
	     *
	     * @return the reference to the transaction, that can be used to refer to the jar in a class path or as future dependency of other jars
	     * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	     * @throws TransactionException if the transaction could not be executed and the store of the node has been expanded with a failed transaction
	     */
	    TransactionReference get() throws TransactionRejectedException, TransactionException;

	    /**
	     * Yields an identifier of the transaction, that can be used for polling its result.
	     * This can be, for instance, a hash of the transaction.
	     * 
	     * @return the identifier
	     * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	     */
	    String id() throws TransactionRejectedException;
	}
}