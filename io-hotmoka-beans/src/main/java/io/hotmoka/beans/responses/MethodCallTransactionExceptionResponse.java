package io.hotmoka.beans.responses;

import java.math.BigInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithEvents;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;

/**
 * A response for a successful transaction that calls a method in blockchain.
 * The method is annotated as {@link io.takamaka.code.lang.ThrowsExceptions}.
 * It has been called without problems but it threw an instance of {@link java.lang.Exception}.
 */
@Immutable
public class MethodCallTransactionExceptionResponse extends MethodCallTransactionResponse implements TransactionResponseWithEvents {

	private static final long serialVersionUID = 5236790249190745461L;

	/**
	 * The updates resulting from the execution of the transaction.
	 */
	private final Update[] updates;

	/**
	 * The events generated by this transaction.
	 */
	private final StorageReference[] events;

	/**
	 * The fully-qualified class name of the cause exception.
	 */
	public final String classNameOfCause;

	/**
	 * The message of the cause exception. This might be {@code null}.
	 */
	public final String messageOfCause;

	/**
	 * The program point where the exception occurred.
	 */
	public final String where;

	/**
	 * Builds the transaction response.
	 * 
	 * @param exception the exception that has been thrown by the method
	 * @param updates the updates resulting from the execution of the transaction
	 * @param events the events resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public MethodCallTransactionExceptionResponse(Exception exception, Stream<Update> updates, Stream<StorageReference> events, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.updates = updates.toArray(Update[]::new);
		this.events = events.toArray(StorageReference[]::new);
		this.classNameOfCause = exception.getClass().getName();
		this.messageOfCause = exception.getMessage();

		StackTraceElement[] stackTrace = exception.getStackTrace();
		this.where = (stackTrace != null && stackTrace.length > 0) ?
			stackTrace[0].getFileName() + ":" + stackTrace[0].getLineNumber() : "<unknown line>";
	}

	@Override
	public final Stream<Update> getUpdates() {
		return Stream.of(updates);
	}

	@Override
	public Stream<StorageReference> getEvents() {
		return Stream.of(events);
	}

	@Override
	public String toString() {
		if (messageOfCause == null)
			return super.toString() + "\n  throws: " + classNameOfCause + "\n  events:\n" + getEvents().map(StorageReference::toString).collect(Collectors.joining("\n    ", "    ", ""));
		else
			return super.toString() + "\n  throws: " + classNameOfCause + ":" + messageOfCause + "\n  events:\n" + getEvents().map(StorageReference::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}
}