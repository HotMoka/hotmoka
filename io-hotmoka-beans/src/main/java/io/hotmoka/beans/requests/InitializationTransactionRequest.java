package io.hotmoka.beans.requests;

import java.io.IOException;

import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.InitializationTransactionResponse;
import io.hotmoka.beans.values.StorageReference;

/**
 * A request to initialize a node. It sets the manifest of a node.
 * After the manifest has been set, no more initial transactions can be executed,
 * hence the node is considered initialized. The manifest cannot be set twice.
 */
@Immutable
public class InitializationTransactionRequest extends InitialTransactionRequest<InitializationTransactionResponse> {
	final static byte SELECTOR = 10;

	/**
	 * The reference to the jar containing the basic Takamaka classes. This must
	 * have been already installed by a previous transaction.
	 */
	public final TransactionReference classpath;

	/**
	 * The storage reference that must be set as manifest.
	 */
	public final StorageReference manifest;

	/**
	 * Builds the transaction request.
	 * 
	 * @param classpath the reference to the jar containing the basic Takamaka classes. This must
	 *                  have been already installed by a previous transaction
	 * @param manifest the storage reference that must be set as manifest
	 */
	public InitializationTransactionRequest(TransactionReference classpath, StorageReference manifest) {
		if (classpath == null)
			throw new IllegalArgumentException("classpath cannot be null");

		if (manifest == null)
			throw new IllegalArgumentException("manifest cannot be null");

		this.classpath = classpath;
		this.manifest = manifest;
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n"
        	+ "  class path: " + classpath + "\n"
        	+ "  manifest: " + manifest;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof InitializationTransactionRequest) {
			InitializationTransactionRequest otherCast = (InitializationTransactionRequest) other;
			return classpath.equals(otherCast.classpath) && manifest.equals(otherCast.manifest);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return classpath.hashCode() ^ manifest.hashCode();
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		classpath.into(context);
		manifest.intoWithoutSelector(context);
	}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * The selector has been already unmarshalled.
	 * 
	 * @param ois the stream
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 * @throws ClassNotFoundException if the request could not be unmarshalled
	 */
	public static InitializationTransactionRequest from(UnmarshallingContext context) throws IOException, ClassNotFoundException {
		TransactionReference classpath = TransactionReference.from(context);
		StorageReference manifest = StorageReference.from(context);

		return new InitializationTransactionRequest(classpath, manifest);
	}
}