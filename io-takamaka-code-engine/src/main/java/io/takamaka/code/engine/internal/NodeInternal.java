package io.takamaka.code.engine.internal;

import java.math.BigInteger;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InitialTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.takamaka.code.engine.Config;
import io.takamaka.code.engine.NodeCaches;
import io.takamaka.code.engine.Store;
import io.takamaka.code.engine.StoreUtilities;

/**
 * The methods of a Hotmoka node that are used inside the internal
 * implementation of this module. This interface allows to enlarge
 * the visibility of some methods, only for the classes the implement
 * the module.
 */
public interface NodeInternal {

	/**
	 * Yields the configuration of this node.
	 * 
	 * @return the configuration
	 */
	Config getConfig();

	/**
	 * Yields the caches of this node.
	 * 
	 * @return the caches
	 */
	NodeCaches getCaches();

	/**
	 * Yields the gas cost model of this node.
	 * 
	 * @return the gas model
	 */
	GasCostModel getGasCostModel();

	/**
	 * Yields the store of this node.
	 * 
	 * @return the store of this node
	 */
	Store getStore();

	/**
	 * Yields an object that provides methods for reconstructing data from the store of this node.
	 * 
	 * @return the store utilities
	 */
	StoreUtilities getStoreUtilities();

	/**
	 * Yields the base cost of the given transaction. Normally, this is just
	 * {@code request.size(gasCostModel)}, but subclasses might redefine.
	 * 
	 * @param request the request of the transaction
	 * @return the base cost of the transaction
	 */
	BigInteger getRequestStorageCost(NonInitialTransactionRequest<?> request);

	/**
	 * Determines if the given initial transaction can still be run after the
	 * initialization of the node. Normally, this is false. However, specific
	 * implementations of the node might redefine and allow it.
	 * 
	 * @param request the request
	 * @return true if only if the execution of {@code request} is allowed
	 *         also after the initialization of this node
	 */
	boolean admitsAfterInitialization(InitialTransactionRequest<?> request);

	/**
	 * Checks that the given transaction reference looks syntactically correct.
	 * 
	 * @param reference the reference to check
	 * @throws IllegalArgumentException only if it does not look syntactically correct
	 */
	void checkTransactionReference(TransactionReference reference);

	/**
	 * Yields the reference, in the store of the node, where the base Takamaka base classes are installed.
	 * If this node has some form of commit, then this method returns a reference
	 * only if the installation of the jar with the Takamaka base classes has been
	 * already committed.
	 * 
	 * @throws NoSuchElementException if the node has not been initialized yet
	 */
	TransactionReference getTakamakaCode() throws NoSuchElementException;

	/**
	 * Yields the request that generated the transaction with the given reference.
	 * If this node has some form of commit, then this method can only succeed
	 * when the transaction has been definitely committed in this node.
	 * Nodes are allowed to keep in store all, some or none of the requests
	 * that they received during their lifetime.
	 * 
	 * @param reference the reference of the transaction
	 * @return the request
	 * @throws NoSuchElementException if there is no request with that reference
	 */
	TransactionRequest<?> getRequest(TransactionReference reference) throws NoSuchElementException;

	/**
	 * Yields the response generated for the request for the given transaction.
	 * If this node has some form of commit, then this method can only succeed
	 * or yield a {@linkplain TransactionRejectedException} only
	 * when the transaction has been definitely committed in this node.
	 * Nodes are allowed to keep in store all, some or none of the responses
	 * that they computed during their lifetime.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response
	 * @throws TransactionRejectedException if there is a request for that transaction but it failed with this exception
	 * @throws NoSuchElementException if there is no request, and hence no response, with that reference
	 */
	TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException;

	/**
	 * Yields the class tag of the object with the given storage reference.
	 * If this method succeeds and this node has some form of commit, then the transaction
	 * of the storage reference has been definitely committed in this node.
	 * A node is allowed to keep in store all, some or none of the objects.
	 * Hence, this method might fail to find the class tag although the object previously
	 * existed in store.
	 * 
	 * @param object the storage reference of the object
	 * @return the class tag, if any
	 * @throws NoSuchElementException if there is no object with that reference or
	 *                                if the class tag could not be found
	 */
	ClassTag getClassTag(StorageReference object) throws NoSuchElementException;

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
	StorageValue runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException;

	/**
	 * Runs the given task with the executor service of this node.
	 * 
	 * @param <T> the type of the result of the task
	 * @param task the task
	 * @return the value computed by the task
	 */
	<T> Future<T> submit(Callable<T> task);

	/**
	 * Runs the given task with the executor service of this node.
	 * 
	 * @param task the task
	 */
	void submit(Runnable task);
}