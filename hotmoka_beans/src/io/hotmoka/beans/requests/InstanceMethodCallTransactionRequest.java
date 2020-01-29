package io.hotmoka.beans.requests;

import java.math.BigInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A request for calling an instance method of a storage object in blockchain.
 */
@Immutable
public class InstanceMethodCallTransactionRequest extends CodeExecutionTransactionRequest<MethodCallTransactionResponse> {

	private static final long serialVersionUID = -1016861794592561931L;

	/**
	 * The constructor to call.
	 */
	public final MethodSignature method;

	/**
	 * The receiver of the call.
	 */
	public final StorageReference receiver;

	/**
	 * The actual arguments passed to the method.
	 */
	private final StorageValue[] actuals;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param gas the maximal amount of gas that can be consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param receiver the receiver of the call
	 * @param actuals the actual arguments passed to the method
	 */
	public InstanceMethodCallTransactionRequest(StorageReference caller, BigInteger gas, Classpath classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) {
		super(caller, gas, classpath);

		this.method = method;
		this.receiver = receiver;
		this.actuals = actuals;
	}

	/**
	 * Yields the actual arguments passed to the method.
	 * 
	 * @return the actual arguments
	 */
	public final Stream<StorageValue> getActuals() {
		return Stream.of(actuals);
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n"
        	+ "  caller: " + caller + "\n"
        	+ "  gas: " + gas + "\n"
        	+ "  class path: " + classpath + "\n"
			+ "  method: " + method + "\n"
			+ "  receiver: " + receiver + "\n"
			+ "  actuals:\n" + getActuals().map(StorageValue::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}
}