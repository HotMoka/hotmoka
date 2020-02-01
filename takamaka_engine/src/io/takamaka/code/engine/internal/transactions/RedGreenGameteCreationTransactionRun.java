package io.takamaka.code.engine.internal.transactions;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.internal.EngineClassLoaderImpl;

public class RedGreenGameteCreationTransactionRun extends AbstractTransactionRun<RedGreenGameteCreationTransactionRequest, GameteCreationTransactionResponse> {
	private final EngineClassLoaderImpl classLoader;

	/**
	 * The response computed at the end of the transaction.
	 */
	private final GameteCreationTransactionResponse response;

	public RedGreenGameteCreationTransactionRun(RedGreenGameteCreationTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node);

		try (EngineClassLoaderImpl classLoader = new EngineClassLoaderImpl(request.classpath, this)) {
			this.classLoader = classLoader;

			if (request.initialAmount.signum() < 0 || request.redInitialAmount.signum() < 0)
				throw new IllegalTransactionRequestException("The gamete must be initialized with a non-negative amount of coins");

			// we create an initial gamete RedGreenExternallyOwnedContract and we fund it with the initial amount
			Object gamete = classLoader.getRedGreenExternallyOwnedAccount().getDeclaredConstructor().newInstance();
			// we set the balance field of the gamete
			classLoader.setBalanceOf(gamete, request.initialAmount);

			// we set the red balance field of the gamete
			Field redBalanceField = classLoader.getRedGreenContract().getDeclaredField("balanceRed");
			redBalanceField.setAccessible(true); // since the field is private
			redBalanceField.set(gamete, request.redInitialAmount);

			this.response = new GameteCreationTransactionResponse(updatesExtractor.extractUpdatesFrom(Stream.of(gamete)), classLoader.getStorageReferenceOf(gamete));
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "cannot complete the transaction");
		}
	}

	@Override
	public EngineClassLoaderImpl getClassLoader() {
		return classLoader;
	}

	@Override
	public GameteCreationTransactionResponse getResponse() {
		return response;
	}
}