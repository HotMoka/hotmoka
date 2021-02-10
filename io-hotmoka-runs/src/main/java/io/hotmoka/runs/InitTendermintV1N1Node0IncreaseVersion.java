package io.hotmoka.runs;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TWO;

import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.PrivateKey;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.nodes.Node;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.TendermintBlockchainConfig;
import io.hotmoka.tendermint.views.TendermintInitializedNode;

/**
 * An example that shows how to create a brand new Tendermint Hotmoka blockchain.
 * 
 * This class is meant to be run from the parent directory, after building the project,
 * with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.runs.InitTendermintV1N1Node0IncreaseVersion
 */
public class InitTendermintV1N1Node0IncreaseVersion extends Run {
	
	public static void main(String[] args) throws Exception {
		TendermintBlockchainConfig config = new TendermintBlockchainConfig.Builder()
			.setTendermintConfigurationToClone(Paths.get("io-hotmoka-runs/tendermint_configs/v1n1/node0"))
			.build();
		ConsensusParams consensus = new ConsensusParams.Builder().build();

		try (TendermintBlockchain node = TendermintBlockchain.init(config, consensus)) {
			PrivateKey privateKeyOfGamete = initialize(node);
			printManifest(node);
			runSillyTransactions(node, privateKeyOfGamete);
		}
	}

	private static PrivateKey initialize(TendermintBlockchain node) throws Exception {
		ConsensusParams consensus = new ConsensusParams.Builder().build();
		return TendermintInitializedNode.of(node, consensus, Paths.get("modules/explicit/io-takamaka-code-1.0.0.jar"), GREEN, RED).keysOfGamete().getPrivate();
	}

	private static void runSillyTransactions(Node node, PrivateKey privateKeyOfGamete) throws Exception {
		NonVoidMethodSignature takamakaNow = new NonVoidMethodSignature(ClassType.TAKAMAKA, "now", BasicTypes.LONG);

		TransactionReference takamakaCode = node.getTakamakaCode();
		StorageReference manifest = node.getManifest();

		StorageReference gamete = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_GAMETE, manifest));

		BigInteger nonce = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.NONCE, gamete))).value;

		String chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;

		Signer signer = Signer.with(node.getSignatureAlgorithmForRequests(), privateKeyOfGamete);

		
		System.out.println("\nPress p for generating a new poll:");
		System.out.println("\nThen, press y to approve or n to reject");
		System.out.println("\nPress q to exit");
		
		boolean end = false;
		StorageReference poll = null;
		boolean isPollOver = true;
		boolean isPollClosed = true;
		
		while(!end) {
			
			if(poll != null) {
				isPollOver = checkPollOver(node, gamete, poll);
				if (isPollOver && !isPollClosed) {
					tryToClosePoll(node, gamete, poll);
					nonce = nonce.add(ONE);
				}
			}
				
			String input = System.console().readLine();
			switch(input) {
			case "p": 
				if(isPollOver) {
					poll = createPoll(node, gamete);
					nonce = nonce.add(ONE);
					isPollClosed = false;
				}
				break;
			case "y":
				if(poll != null)
					voteYes(node, gamete, poll);
					nonce = nonce.add(ONE);
				break;
			case "n":
					voteNo(node, gamete, poll);
					nonce = nonce.add(ONE);
				break;
			case "q": end = true; 
				break;
			default: System.out.println("unrecognized option");
			}
		}
	}
	
	private static boolean checkPollOver(Node node, StorageReference gamete, StorageReference poll) throws TransactionRejectedException, TransactionException, CodeExecutionException{
		TransactionReference takamakaCode = node.getTakamakaCode();
		StorageReference manifest = node.getManifest();
		BooleanValue result = (BooleanValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(gamete, BigInteger.valueOf(10_000), takamakaCode, CodeSignature.IS_VOTE_OVER, poll));
		return result.value;
	}
	
	private static void voteYes(Node node, StorageReference gamete, StorageReference poll) throws TransactionRejectedException, TransactionException, CodeExecutionException{
		TransactionReference takamakaCode = node.getTakamakaCode();
		StorageReference manifest = node.getManifest();
		node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(gamete, BigInteger.valueOf(10_000), takamakaCode, CodeSignature.VOTE, poll));
	}
	
	private static void voteNo(Node node, StorageReference gamete, StorageReference poll) throws TransactionRejectedException, TransactionException, CodeExecutionException{
		TransactionReference takamakaCode = node.getTakamakaCode();
		StorageReference manifest = node.getManifest();
		node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(gamete, BigInteger.valueOf(10_000), takamakaCode, CodeSignature.VOTE, poll, new BigIntegerValue(BigInteger.ZERO)));
	}
	
	private static void tryToClosePoll(Node node, StorageReference gamete, StorageReference poll) throws TransactionRejectedException, TransactionException, CodeExecutionException{
		TransactionReference takamakaCode = node.getTakamakaCode();
		StorageReference manifest = node.getManifest();
		node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(gamete, BigInteger.valueOf(10_000), takamakaCode, CodeSignature.CLOSE_POLL, poll));
	}
	
	private static StorageReference createPoll(Node node, StorageReference gamete) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		TransactionReference takamakaCode = node.getTakamakaCode();
		StorageReference manifest = node.getManifest();
		StorageReference validators = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, BigInteger.valueOf(10_000), takamakaCode, CodeSignature.GET_VALIDATORS, manifest));
		return (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(gamete, BigInteger.valueOf(10_000), takamakaCode, CodeSignature.NEW_POLL, validators));
	}

	private static BigInteger getGasPrice(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		TransactionReference takamakaCode = node.getTakamakaCode();
		StorageReference manifest = node.getManifest();

		StorageReference gasStation = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, BigInteger.valueOf(10_000), takamakaCode, CodeSignature.GET_GAS_STATION, manifest));

		BigInteger minimalGasPrice = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, BigInteger.valueOf(10_000), takamakaCode, CodeSignature.GET_GAS_PRICE, gasStation))).value;

		// we double the minimal price, to be sure that the transaction won't be rejected
		return TWO.multiply(minimalGasPrice);
	}
}