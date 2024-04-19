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

package io.hotmoka.node.internal.responses;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.api.responses.VoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.Updates;

/**
 * Implementation of a response for a successful transaction that calls a method
 * in the store of a node. The method has been called without problems and
 * without generating exceptions. The method returns {@code void}.
 */
@Immutable
public class VoidMethodCallTransactionSuccessfulResponseImpl extends MethodCallTransactionResponseImpl implements VoidMethodCallTransactionSuccessfulResponse {
	final static byte SELECTOR = 12;
	final static byte SELECTOR_NO_EVENTS = 16;

	/**
	 * The events generated by this transaction.
	 */
	private final StorageReference[] events;

	/**
	 * Builds the transaction response.
	 * 
	 * @param updates the updates resulting from the execution of the transaction
	 * @param events the events resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public VoidMethodCallTransactionSuccessfulResponseImpl(Stream<Update> updates, Stream<StorageReference> events, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.events = events.toArray(StorageReference[]::new);
		Stream.of(this.events).forEach(event -> Objects.requireNonNull(event, "events cannot hold null"));
	}

	@Override
	public Stream<StorageReference> getEvents() {
		return Stream.of(events);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof VoidMethodCallTransactionSuccessfulResponse vmctsr && super.equals(other) && Arrays.equals(events, vmctsr.getEvents().toArray(StorageReference[]::new));
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Arrays.hashCode(events);
	}

	@Override
	public String toString() {
        return super.toString() + "\n  events:\n" + getEvents().map(StorageReference::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		boolean optimized = events.length == 0;

		context.writeByte(optimized ? SELECTOR_NO_EVENTS : SELECTOR);
		super.into(context);

		if (!optimized)
			intoArrayWithoutSelector(events, context);
	}

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @param selector the selector
	 * @return the response
	 * @throws IOException if the response could not be unmarshalled
	 */
	public static VoidMethodCallTransactionSuccessfulResponseImpl from(UnmarshallingContext context, byte selector) throws IOException {
		Stream<Update> updates = Stream.of(context.readLengthAndArray(Updates::from, Update[]::new));
		var gasConsumedForCPU = context.readBigInteger();
		var gasConsumedForRAM = context.readBigInteger();
		var gasConsumedForStorage = context.readBigInteger();
		Stream<StorageReference> events;

		if (selector == SELECTOR)
			events = Stream.of(context.readLengthAndArray(StorageValues::referenceWithoutSelectorFrom, StorageReference[]::new));
		else if (selector == SELECTOR_NO_EVENTS)
			events = Stream.empty();
		else
			throw new IOException("unexpected response selector: " + selector);

		return new VoidMethodCallTransactionSuccessfulResponseImpl(updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}
}