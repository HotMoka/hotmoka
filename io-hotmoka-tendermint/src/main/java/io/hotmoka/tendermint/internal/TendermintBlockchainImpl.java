package io.hotmoka.tendermint.internal;

import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.updates.UpdateOfStorage;
import io.hotmoka.beans.updates.UpdateOfString;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.TendermintBlockchainConfig;
import io.hotmoka.tendermint.TendermintValidator;
import io.hotmoka.tendermintdependencies.server.Server;
import io.takamaka.code.engine.AbstractLocalNode;

/**
 * An implementation of a blockchain working over the Tendermint generic blockchain engine.
 * Requests sent to this blockchain are forwarded to a Tendermint process. This process
 * checks and delivers such requests, by calling the ABCI interface. This blockchain keeps
 * its state in a transactional database implemented by the {@linkplain Store} class.
 */
@ThreadSafe
public class TendermintBlockchainImpl extends AbstractLocalNode<TendermintBlockchainConfig, Store> implements TendermintBlockchain {

	/**
	 * The GRPC server that runs the ABCI process.
	 */
	private final Server abci;

	/**
	 * A proxy to the Tendermint process.
	 */
	private final Tendermint tendermint;

	/**
	 * Builds a Tendermint blockchain. This constructor spawns the Tendermint process on localhost
	 * and connects it to an ABCI application for handling its transactions.
	 * 
	 * @param config the configuration of the blockchain
	 */
	public TendermintBlockchainImpl(TendermintBlockchainConfig config) {
		super(config);

		try {
			this.abci = new Server(config.abciPort, new ABCI(this));
			this.abci.start();
			this.tendermint = new Tendermint(this);
		}
		catch (Exception e) {
			logger.error("failed creating the Tendermint blockchain", e);

			try {
				close();
			}
			catch (Exception e1) {
				logger.error("cannot close the blockchain", e1);
			}

			throw InternalFailureException.of(e);
		}
	}

	
	@Override
	public void close() throws Exception {
		if (isNotYetClosed()) {
			super.close();

			if (tendermint != null)
				tendermint.close();

			if (abci != null && !abci.isShutdown()) {
				abci.shutdown();
				abci.awaitTermination();
			}
		}
	}

	@Override
	public String getTendermintChainId() {
		try {
			return tendermint.getChainId();
		}
		catch (Exception e) {
			logger.error("the Tendermint chain id is not set for this node", e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public Stream<TendermintValidator> getTendermintValidators() {
		try {
			return tendermint.getValidators();
		}
		catch (Exception e) {
			logger.error("the Tendermint validators cannot be retrieved for this node", e);
			throw InternalFailureException.of(e);
		} 
	}

	@Override
	protected Store mkStore() {
		return new Store(this);
	}


	@Override
	protected void postRequest(TransactionRequest<?> request) {
		try {
			String response = tendermint.broadcastTxAsync(request);
			tendermint.checkBroadcastTxResponse(response);
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}


	/**
	 * Yields the proxy to the Tendermint process.
	 */
	Tendermint getTendermint() {
		return tendermint;
	}

	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);
	private static final byte[] NO_SIG = new byte[0];
	private static final ClassType storageMapView = new ClassType("io.takamaka.code.util.StorageMapView");
	private static final MethodSignature SIZE = new NonVoidMethodSignature(storageMapView, "size", BasicTypes.INT);
	private static final MethodSignature GET_SHARES = new NonVoidMethodSignature(ClassType.VALIDATORS, "getShares", storageMapView);
	private static final MethodSignature SELECT = new NonVoidMethodSignature(storageMapView, "select", ClassType.OBJECT, BasicTypes.INT);
	private static final MethodSignature ID = new NonVoidMethodSignature(ClassType.VALIDATOR, "id", ClassType.STRING);
	private static final MethodSignature GET = new NonVoidMethodSignature(storageMapView, "get", ClassType.OBJECT, ClassType.OBJECT);

	Optional<TendermintValidator[]> getTendermintValidatorsInStore() throws TransactionRejectedException, TransactionException, CodeExecutionException {
		StorageReference manifest;

		try {
			manifest = getManifest();
		}
		catch (NoSuchElementException e) {
			return Optional.empty();
		}

		StorageReference validators = getValidators();
		StorageReference gamete = ((UpdateOfStorage) getLastUpdateToFieldUncommitted(manifest, FieldSignature.MANIFEST_GAMETE_FIELD)).value;
		TransactionReference takamakaCode = getTakamakaCode();

		StorageReference shares = (StorageReference) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(NO_SIG, gamete, ZERO, "", _10_000, ZERO, takamakaCode, GET_SHARES, validators));

		int numOfValidators = ((IntValue) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(NO_SIG, gamete, ZERO, "", _10_000, ZERO, takamakaCode, SIZE, shares))).value;

		TendermintValidator[] result = new TendermintValidator[numOfValidators];

		for (int num = 0; num < numOfValidators; num++) {
			StorageReference validator = (StorageReference) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(NO_SIG, gamete, ZERO, "", _10_000, ZERO, takamakaCode, SELECT, shares, new IntValue(num)));

			String id = ((StringValue) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(NO_SIG, gamete, ZERO, "", _10_000, ZERO, takamakaCode, ID, validator))).value;

			long power = ((BigIntegerValue) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(NO_SIG, gamete, ZERO, "", _10_000, ZERO, takamakaCode, GET, shares, validator))).value.longValue();

			String publicKey = ((UpdateOfString) getLastUpdateToFieldUncommitted(validator, FieldSignature.EOA_PUBLIC_KEY_FIELD)).value;
			// Tendermint stores the public key without the leading 12 bytes
			byte[] raw = Base64.getDecoder().decode(publicKey);
			byte[] raw2 = new byte[raw.length - 12];
			System.arraycopy(raw, 12, raw2, 0, raw2.length);
			publicKey = Base64.getEncoder().encodeToString(raw2);

			result[num] = new TendermintValidator(id, power, publicKey, "tendermint/PubKeyEd25519");
		}

		return Optional.of(result);
	}
}