package io.hotmoka.beans.responses;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;

/**
 * A response for a successful transaction that calls a constructor of a storage
 * class in blockchain. The constructor has been called without problems and
 * without generating exceptions.
 */
@Immutable
public class ConstructorCallTransactionSuccessfulResponse extends ConstructorCallTransactionResponse implements TransactionResponseWithEvents {
	final static byte SELECTOR = 6;
	final static byte SELECTOR_NO_EVENTS = 13;

	/**
	 * The events generated by this transaction.
	 */
	private final StorageReference[] events;

	/**
	 * The object that has been created by the constructor call.
	 */
	public final StorageReference newObject;

	/**
	 * Builds the transaction response.
	 * 
	 * @param newObject the object that has been successfully created
	 * @param updates the updates resulting from the execution of the transaction
	 * @param events the events resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public ConstructorCallTransactionSuccessfulResponse(StorageReference newObject, Stream<Update> updates, Stream<StorageReference> events, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.newObject = newObject;
		this.events = events.toArray(StorageReference[]::new);
	}

	@Override
	public String toString() {
        return super.toString() + "\n"
       		+ "  new object: " + newObject + "\n"
        	+ "  events:\n" + getEvents().map(StorageReference::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}

	@Override
	public Stream<StorageReference> getEvents() {
		return Stream.of(events);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ConstructorCallTransactionSuccessfulResponse) {
			ConstructorCallTransactionSuccessfulResponse otherCast = (ConstructorCallTransactionSuccessfulResponse) other;
			return super.equals(other) && Arrays.equals(events, otherCast.events) && newObject.equals(otherCast.newObject);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Arrays.hashCode(events) ^ newObject.hashCode();
	}

	@Override
	public StorageReference getOutcome() {
		return newObject;
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(newObject.size(gasCostModel))
			.add(getEvents().map(event -> event.size(gasCostModel)).reduce(BigInteger.ZERO, BigInteger::add));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(events.length == 0 ? SELECTOR_NO_EVENTS : SELECTOR);
		super.into(context);
		if (events.length > 0)
			intoArrayWithoutSelector(events, context);
		newObject.intoWithoutSelector(context);
	}

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @param selector the selector
	 * @return the request
	 * @throws IOException if the response could not be unmarshalled
	 * @throws ClassNotFoundException if the response could not be unmarshalled
	 */
	public static ConstructorCallTransactionSuccessfulResponse from(UnmarshallingContext context, byte selector) throws IOException, ClassNotFoundException {
		Stream<Update> updates = Stream.of(unmarshallingOfArray(Update::from, Update[]::new, context));
		BigInteger gasConsumedForCPU = context.readBigInteger();
		BigInteger gasConsumedForRAM = context.readBigInteger();
		BigInteger gasConsumedForStorage = context.readBigInteger();
		Stream<StorageReference> events;
		if (selector == SELECTOR)
			events = Stream.of(unmarshallingOfArray(StorageReference::from, StorageReference[]::new, context));
		else if (selector == SELECTOR_NO_EVENTS)
			events = Stream.empty();
		else
			throw new IOException("unexpected response selector: " + selector);

		StorageReference newObject = StorageReference.from(context);
		return new ConstructorCallTransactionSuccessfulResponse(newObject, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}
}