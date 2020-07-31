package io.hotmoka.beans.responses;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import io.hotmoka.beans.annotations.Immutable;

/**
 * A response for a transaction that initializes a node.
 * After that, no more initial transactions can be executed.
 */
@Immutable
public class InitializationTransactionResponse extends InitialTransactionResponse {
	final static byte SELECTOR = 14;

	/**
	 * Builds the transaction response.
	 */
	public InitializationTransactionResponse() {
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof InitializationTransactionResponse;
	}

	@Override
	public int hashCode() {
		return 13011973;
	}

	@Override
	public String toString() {
        return getClass().getSimpleName();
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
	}

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param ois the stream
	 * @return the request
	 * @throws IOException if the response could not be unmarshalled
	 * @throws ClassNotFoundException if the response could not be unmarshalled
	 */
	public static InitializationTransactionResponse from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		return new InitializationTransactionResponse();
	}
}