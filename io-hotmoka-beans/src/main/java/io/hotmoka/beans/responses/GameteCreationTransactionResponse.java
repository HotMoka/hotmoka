package io.hotmoka.beans.responses;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;

/**
 * A response for a transaction that installs a jar in a yet not initialized blockchain.
 */
@Immutable
public class GameteCreationTransactionResponse implements InitialTransactionResponse, TransactionResponseWithUpdates {

	private static final long serialVersionUID = -95476487153660743L;
	final static byte SELECTOR = 0;

	/**
	 * The updates resulting from the execution of the transaction.
	 */
	private final Update[] updates;

	/**
	 * The created gamete.
	 */
	public final StorageReference gamete;

	/**
	 * Builds the transaction response.
	 * 
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gamete the created gamete
	 */
	public GameteCreationTransactionResponse(Stream<Update> updates, StorageReference gamete) {
		this.updates = updates.toArray(Update[]::new);
		this.gamete = gamete;
	}

	@Override
	public final Stream<Update> getUpdates() {
		return Stream.of(updates);
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n"
        	+ "  gamete: " + gamete + "\n"
       		+ "  updates:\n" + getUpdates().map(Update::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}

	/**
	 * Yields the outcome of the execution having this response.
	 * 
	 * @return the outcome
	 */
	public StorageReference getOutcome() {
		return gamete;
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);

		oos.writeInt(updates.length);
		for (Update update: updates)
			update.into(oos);

		gamete.into(oos);
	}
}