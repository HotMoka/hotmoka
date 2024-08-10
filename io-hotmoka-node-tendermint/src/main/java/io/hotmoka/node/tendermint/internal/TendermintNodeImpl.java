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

package io.hotmoka.node.tendermint.internal;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonSyntaxException;
import com.google.protobuf.ByteString;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.Hex;
import io.hotmoka.exceptions.CheckRunnable;
import io.hotmoka.exceptions.CheckSupplier;
import io.hotmoka.exceptions.UncheckConsumer;
import io.hotmoka.exceptions.UncheckFunction;
import io.hotmoka.node.NodeInfos;
import io.hotmoka.node.NodeUnmarshallingContexts;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.local.AbstractTrieBasedLocalNode;
import io.hotmoka.node.local.StateIds;
import io.hotmoka.node.local.api.StateId;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.api.UnknownStateIdException;
import io.hotmoka.node.tendermint.api.TendermintNode;
import io.hotmoka.node.tendermint.api.TendermintNodeConfig;
import io.hotmoka.tendermint.abci.ABCI;
import io.hotmoka.tendermint.abci.Server;
import io.hotmoka.xodus.ByteIterable;
import io.hotmoka.xodus.ExodusException;
import io.hotmoka.xodus.env.Transaction;
import tendermint.abci.Types.Evidence;
import tendermint.abci.Types.RequestBeginBlock;
import tendermint.abci.Types.RequestCheckTx;
import tendermint.abci.Types.RequestCommit;
import tendermint.abci.Types.RequestDeliverTx;
import tendermint.abci.Types.RequestEcho;
import tendermint.abci.Types.RequestEndBlock;
import tendermint.abci.Types.RequestFlush;
import tendermint.abci.Types.RequestInfo;
import tendermint.abci.Types.RequestInitChain;
import tendermint.abci.Types.RequestQuery;
import tendermint.abci.Types.ResponseBeginBlock;
import tendermint.abci.Types.ResponseCheckTx;
import tendermint.abci.Types.ResponseCommit;
import tendermint.abci.Types.ResponseDeliverTx;
import tendermint.abci.Types.ResponseEcho;
import tendermint.abci.Types.ResponseEndBlock;
import tendermint.abci.Types.ResponseFlush;
import tendermint.abci.Types.ResponseInfo;
import tendermint.abci.Types.ResponseInitChain;
import tendermint.abci.Types.ResponseQuery;
import tendermint.abci.Types.Validator;
import tendermint.abci.Types.ValidatorUpdate;
import tendermint.abci.Types.VoteInfo;
import tendermint.crypto.Keys.PublicKey;

/**
 * An implementation of a node working over the Tendermint generic blockchain engine.
 * Requests sent to this node are forwarded to a Tendermint process. This process
 * checks and delivers such requests, by calling the ABCI interface. This blockchain keeps
 * its state in a transactional database implemented by the {@link TendermintStore} class.
 */
@ThreadSafe
public class TendermintNodeImpl extends AbstractTrieBasedLocalNode<TendermintNodeImpl, TendermintNodeConfig, TendermintStore, TendermintStoreTransformation> implements TendermintNode {

	private final static Logger LOGGER = Logger.getLogger(TendermintNodeImpl.class.getName());

	/**
	 * The key used inside {@link #storeOfNode} to keep the height of the head of this node.
	 */
	private final static ByteIterable HEIGHT = ByteIterable.fromBytes("height".getBytes());

	/**
	 * The key used inside {@link #storeOfNode} to keep the root of the store of this node.
	 */
	private final static ByteIterable ROOT = ByteIterable.fromBytes("root".getBytes());

	/**
	 * The GRPC server that runs the ABCI process.
	 */
	private final Server abci;

	/**
	 * A proxy to the Tendermint process.
	 */
	private final Tendermint tendermint;

	/**
	 * An object for posting requests to the Tendermint process.
	 */
	private final TendermintPoster poster;

	/**
	 * True if and only if we are running on Windows.
	 */
	private final boolean isWindows;

	/**
	 * The store of the head of this node.
	 */
	private volatile TendermintStore storeOfHead;

	/**
	 * Builds a Tendermint node. This constructor spawns the Tendermint process on localhost
	 * and connects it to an ABCI application for handling its transactions.
	 * 
	 * @param config the configuration of the blockchain
	 * @param init if true, the working directory of the node gets initialized
	 * @throws NodeException if the operation cannot be completed correctly
	 * @throws InterruptedException the the currently thread is interrupted before completing the construction
	 */
	public TendermintNodeImpl(TendermintNodeConfig config, boolean init) throws NodeException, InterruptedException {
		super(config, init);

		this.isWindows = System.getProperty("os.name").startsWith("Windows");

		try {
			if (init) {
				initWorkingDirectoryOfTendermintProcess(config);
				storeOfHead = mkStore();
			}
			else
				checkOutRootBranch();

			var tendermintConfigFile = new TendermintConfigFile(config);
			this.poster = new TendermintPoster(config, tendermintConfigFile.tendermintPort);
			this.abci = new Server(tendermintConfigFile.abciPort, new TendermintApplication());
			this.abci.start();
			LOGGER.info("Tendermint ABCI started at port " + tendermintConfigFile.abciPort);
			this.tendermint = new Tendermint(config);
			LOGGER.info("Tendermint started at port " + tendermintConfigFile.tendermintPort);
		}
		catch (IOException | TimeoutException e) {
			LOGGER.log(Level.SEVERE, "the creation of the Tendermint node failed. Is Tendermint installed?", e);
			close();
			throw new NodeException("The creation of the Tendermint node failed. Is Tendermint installed?", e);
		}
	}

	@Override
	public NodeInfo getNodeInfo() throws NodeException, TimeoutException, InterruptedException {
		try (var scope = mkScope()) {
			return NodeInfos.of(TendermintNode.class.getName(), HOTMOKA_VERSION, poster.getNodeID());
		}
		catch (JsonSyntaxException | IOException e) {
			throw new NodeException(e);
		}
	}

	@Override
	protected void closeResources() throws NodeException, InterruptedException {
		try {
			closeTendermintAndABCI();
		}
		finally {
			super.closeResources();
		}
	}

	@Override
	protected TendermintStore mkStore() throws NodeException {
		try {
			return new TendermintStore(this);
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	@Override
	protected TendermintStore getStoreOfHead() {
		return storeOfHead;
	}

	@Override
	protected void postRequest(TransactionRequest<?> request) throws NodeException, TimeoutException, InterruptedException {
		poster.postRequest(request);
	}

	private void setRootBranch(StateId stateId, Transaction txn) throws NodeException {
		byte[] id = stateId.getBytes();
		var rootAsBI = ByteIterable.fromBytes(id);

		try {
			// we set the root branch, that will be used if the node is resumed
			getStoreOfNode().put(txn, ROOT, rootAsBI); // set the root branch
			// we keep extra information about the height
			getStoreOfNode().put(txn, HEIGHT, ByteIterable.fromBytes(longToBytes(getHeight(txn) + 1)));
		}
		catch (ExodusException e) {
			throw new NodeException(e);
		}
	}

	private void checkOutRootBranch() throws NodeException, InterruptedException {
		try {
			var root = getEnvironment().computeInTransaction(txn -> Optional.ofNullable(getStoreOfNode().get(txn, ROOT)).map(ByteIterable::getBytes))
				.orElseThrow(() -> new NodeException("Cannot find the root of the saved store of the node"));

			storeOfHead = mkStore(StateIds.of(root), Optional.empty());
		}
		catch (UnknownStateIdException | ExodusException e) {
			throw new NodeException(e);
		}
	}

	private long getHeight(Transaction txn) throws ExodusException {
		ByteIterable bi = getStoreOfNode().get(txn, HEIGHT);
		return bi == null ? 0L : bytesToLong(bi.getBytes());
	}

	private static byte[] longToBytes(long l) {
		var result = new byte[Long.BYTES];
	    for (int i = Long.BYTES - 1; i >= 0; i--) {
	        result[i] = (byte)(l & 0xFF);
	        l >>= Byte.SIZE;
	    }

	    return result;
	}

	private static long bytesToLong(final byte[] b) {
	    long result = 0;
	    for (int i = 0; i < Long.BYTES; i++) {
	        result <<= Byte.SIZE;
	        result |= (b[i] & 0xFF);
	    }

	    return result;
	}

	private void closeTendermintAndABCI() throws NodeException, InterruptedException {
		try {
			if (tendermint != null)
				tendermint.close();
		}
		catch (IOException e) {
			throw new NodeException("Could not close Tendermint", e);
		}
		finally {
			closeABCI();
		}
	}

	private void closeABCI() throws InterruptedException {
		if (abci != null && !abci.isShutdown()) {
			abci.shutdown();
			abci.awaitTermination();
		}		
	}

	private static void copyRecursively(Path src, Path dest) throws IOException {
	    try (Stream<Path> stream = Files.walk(src)) {
	        stream.forEach(source -> copy(source, dest.resolve(src.relativize(source))));
	    }
	    catch (UncheckedIOException e) {
	    	throw e.getCause();
	    }
	}

	private static void copy(Path source, Path dest) {
		try {
			Files.copy(source, dest);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Runs the given command in the operating system shell.
	 * 
	 * @param command the command to run, as if in a shell
	 * @param redirection the file into which the standard output of the command must be redirected
	 * @throws IOException if the command cannot be run
	 */
	private Process run(String command, Optional<String> redirection) throws IOException {
		var processBuilder = new ProcessBuilder();
		processBuilder.command(command.split(" "));
		redirection.map(File::new).ifPresent(processBuilder::redirectOutput);

        return processBuilder.start();
	}

	/**
	 * Initialize the working directory for Tendermint.
	 * If that directory is required to be deleted on start-up (which is the default)
	 * there are two possibilities: either it clones a Tendermint configuration directory specified
	 * in the configuration of the node, or it creates a default Tendermint configuration
	 * with a single node, that acts as unique validator of the network.
	 * 
	 * @param config the configuration of the node
	 * @throws NodeException 
	 */
	private void initWorkingDirectoryOfTendermintProcess(TendermintNodeConfig config) throws InterruptedException, NodeException {
		Optional<Path> tendermintConfigurationToClone = config.getTendermintConfigurationToClone();
		Path tendermintHome = config.getDir().resolve("tendermint");

		try {
			if (tendermintConfigurationToClone.isEmpty()) {
				// if there is no configuration to clone, we create a default network of a single node
				// that plays the role of the unique validator of the network
				String executableName = isWindows ? "cmd.exe /c tendermint.exe" : "tendermint";
				//if (run("tendermint testnet --v 1 --o " + tendermintHome + " --populate-persistent-peers", Optional.empty()).waitFor() != 0)
				if (run(executableName + " init --home " + tendermintHome, Optional.empty()).waitFor() != 0) // TODO: add timeout
					throw new NodeException("Tendermint initialization failed");
			}
			else
				// we clone the configuration files inside config.tendermintConfigurationToClone
				// into the blocks subdirectory of the node directory
				copyRecursively(tendermintConfigurationToClone.get(), tendermintHome);
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * A proxy object that connects to the Tendermint process, sends requests to it
	 * and gets responses from it.
	 */
	private class Tendermint implements AutoCloseable {
	
		/**
		 * The Tendermint process;
		 */
		private final Process process;
	
		/**
		 * Spawns the Tendermint process and creates a proxy to it. It assumes that
		 * the {@code tendermint} command can be executed from the command path.
		 * 
		 * @param config the configuration of the blockchain that is using Tendermint
		 * @throws IOException if an I/O error occurred
		 * @throws TimeoutException if Tendermint did not spawn up in the expected time
		 * @throws InterruptedException if the current thread was interrupted while waiting for the Tendermint process to run
		 */
		private Tendermint(TendermintNodeConfig config) throws IOException, InterruptedException, TimeoutException {
			this.process = spawnTendermintProcess(config);
			waitUntilTendermintProcessIsUp(config);
	
			LOGGER.info("the Tendermint process is up and running");
		}
	
		@Override
		public void close() throws InterruptedException, IOException {
			// the following is important under Windows, since the shell script thats starts Tendermint
			// under Windows spawns it as a subprocess
			process.descendants().forEach(ProcessHandle::destroy);
			process.destroy();
			process.waitFor();
	
			if (isWindows)
				// this seems important under Windows
				try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
					LOGGER.info(br.lines().collect(Collectors.joining()));
				}
	
			LOGGER.info("the Tendermint process has been shut down");
		}
	
		/**
		 * Spawns a Tendermint process using the working directory that has been previously initialized.
		 * 
		 * @param config the configuration of the node
		 * @return the Tendermint process
		 */
		private Process spawnTendermintProcess(TendermintNodeConfig config) throws IOException {
			// spawns a process that remains in background
			Path tendermintHome = config.getDir().resolve("tendermint");
			String executableName = isWindows ? "cmd.exe /c tendermint.exe" : "tendermint";
			return run(executableName + " node --home " + tendermintHome + " --abci grpc", Optional.of("tendermint.log"));
		}
	
		/**
		 * Waits until the Tendermint process acknowledges a ping.
		 * 
		 * @param config the configuration of the node
		 * @throws IOException if it is not possible to connect to the Tendermint process
		 * @throws TimeoutException if tried many times, but never got a reply
		 * @throws InterruptedException if interrupted while pinging
		 */
		private void waitUntilTendermintProcessIsUp(TendermintNodeConfig config) throws TimeoutException, InterruptedException, IOException {
			for (long reconnections = 1; reconnections <= config.getMaxPingAttempts(); reconnections++) {
				try {
					HttpURLConnection connection = poster.openPostConnectionToTendermint();
					try (var os = connection.getOutputStream(); var is = connection.getInputStream()) {
						return;
					}
				}
				catch (ConnectException e) {
					// take a nap, then try again
					Thread.sleep(config.getPingDelay());
				}
			}
	
			try {
				close();
			}
			catch (Exception e) {
				LOGGER.log(Level.SEVERE, "cannot close the Tendermint process", e);
			}
	
			throw new TimeoutException("Cannot connect to Tendermint process at " + poster.url() + ". Tried " + config.getMaxPingAttempts() + " times");
		}
	}

	/**
	 * The Tendermint interface that links a Hotmoka Tendermint node to a Tendermint process.
	 * It implements a set of handlers that Tendermint calls to notify events.
	 */
	private class TendermintApplication extends ABCI {

		private final static Logger LOGGER = Logger.getLogger(TendermintApplication.class.getName());

		/**
		 * The Tendermint validators at the time of the last {@link #beginBlock(RequestBeginBlock, StreamObserver)}
		 * that has been executed.
		 */
		private volatile TendermintValidator[] validatorsAtPreviousBlock;

		private volatile String behaving;

		private volatile String misbehaving;

		/**
		 * The current store transformation, if any.
		 */
		private volatile TendermintStoreTransformation transformation;

		/**
	     * Builds the Tendermint ABCI interface that executes Takamaka transactions.
	     */
	    private TendermintApplication() {}

	    private static String getAddressOfValidator(Validator validator) {
	    	return Hex.toHexString(validator.getAddress().toByteArray()).toUpperCase();
	    }

	    private static long timeOfBlock(RequestBeginBlock request) {
	    	var time = request.getHeader().getTime();
	    	return time.getSeconds() * 1_000L + time.getNanos() / 1_000_000L;
	    }

	    private static String spaceSeparatedSequenceOfMisbehavingValidatorsAddresses(RequestBeginBlock request) {
			return request.getByzantineValidatorsList().stream()
	    		.map(Evidence::getValidator)
	    		.map(TendermintApplication::getAddressOfValidator)
	    		.collect(Collectors.joining(" "));
		}

		private static String spaceSeparatedSequenceOfBehavingValidatorsAddresses(RequestBeginBlock request) {
			return request.getLastCommitInfo().getVotesList().stream()
	    		.filter(VoteInfo::getSignedLastBlock)
	    		.map(VoteInfo::getValidator)
	    		.map(TendermintApplication::getAddressOfValidator)
	    		.collect(Collectors.joining(" "));
		}

		private static void updateValidatorsThatChangedPower(TendermintValidator[] currentValidators, TendermintValidator[] nextValidators, ResponseEndBlock.Builder builder) {
			Stream.of(nextValidators)
				.filter(validator -> isContainedWithDistinctPower(validator.address, validator.power, currentValidators))
				.forEachOrdered(validator -> updateValidator(validator, builder));
		}

		private static void addNextValidatorsThatAreNotCurrentValidators(TendermintValidator[] currentValidators, TendermintValidator[] nextValidators, ResponseEndBlock.Builder builder) {
			Stream.of(nextValidators)
				.filter(validator -> isNotContained(validator.address, currentValidators))
				.forEachOrdered(validator -> addValidator(validator, builder));
		}

		private static void removeCurrentValidatorsThatAreNotNextValidators(TendermintValidator[] currentValidators, TendermintValidator[] nextValidators, ResponseEndBlock.Builder builder) {
			/*String current = Stream.of(currentValidators).map(validator -> validator.address).collect(Collectors.joining(",", "[", "]"));
			String next = Stream.of(nextValidators).map(validator -> validator.address).collect(Collectors.joining(",", "[", "]"));
			LOGGER.info("validators remove: " + current + " -> " + next);*/
			Stream.of(currentValidators)
				.filter(validator -> isNotContained(validator.address, nextValidators))
				.forEachOrdered(validator -> removeValidator(validator, builder));
		}

	    private static void removeValidator(TendermintValidator tv, ResponseEndBlock.Builder builder) {
	    	builder.addValidatorUpdates(intoValidatorUpdate(tv, 0L));
	    	LOGGER.info("removed Tendermint validator with address " + tv.address + " and power " + tv.power);
	    }

	    private static void addValidator(TendermintValidator tv, ResponseEndBlock.Builder builder) {
	    	builder.addValidatorUpdates(intoValidatorUpdate(tv, tv.power));
	    	LOGGER.info("added Tendermint validator with address " + tv.address + " and power " + tv.power);
	    }

	    private static void updateValidator(TendermintValidator tv, ResponseEndBlock.Builder builder) {
	    	builder.addValidatorUpdates(intoValidatorUpdate(tv, tv.power));
	    	LOGGER.info("updated Tendermint validator with address " + tv.address + " by setting its new power to " + tv.power);
	    }

	    private static ValidatorUpdate intoValidatorUpdate(TendermintValidator validator, long newPower) {
	    	byte[] raw;

	    	try {
	    		raw = Base64.fromBase64String(validator.publicKey);
	    	}
	    	catch (Base64ConversionException e) {
	    		throw new RuntimeException(e); // TODO
	    	}
	    	PublicKey publicKey = PublicKey.newBuilder().setEd25519(ByteString.copyFrom(raw)).build();

	    	return ValidatorUpdate.newBuilder()
	    		.setPubKey(publicKey)
	    		.setPower(newPower)
	    		.build();
	    }

	    private static boolean isNotContained(String address, TendermintValidator[] validators) {
	    	return Stream.of(validators).map(validator -> validator.address).noneMatch(address::equals);
	    }

	    private static boolean isContainedWithDistinctPower(String address, long power, TendermintValidator[] validators) {
	    	return Stream.of(validators).anyMatch(validator -> validator.address.equals(address) && validator.power != power);
	    }

	    /**
		 * Yields the hash of the store at the current head of the blockchain.
		 * 
		 * @return the hash
		 */
		private byte[] getLastBlockApplicationHash() throws NodeException {
			return storeOfHead.getStateId().getBytes();
		}

		/**
		 * Yields the blockchain height.
		 * 
		 * @return the height of the blockchain of this node
		 * @throws NodeException if the operation cannot be completed correctly
		 */
		private long getLastBlockHeight() throws NodeException {
			try {
				return getEnvironment().computeInReadonlyTransaction(TendermintNodeImpl.this::getHeight);
			}
			catch (ExodusException e) {
				throw new NodeException(e);
			}
		}

		@Override
		protected ResponseInitChain initChain(RequestInitChain request) {
			request.getInitialHeight();
			return ResponseInitChain.newBuilder().build();
		}

		@Override
		protected ResponseEcho echo(RequestEcho request) {
			return ResponseEcho.newBuilder().build();
		}

		@Override
		protected ResponseInfo info(RequestInfo request) throws NodeException {
			return ResponseInfo.newBuilder()
				.setLastBlockAppHash(ByteString.copyFrom(getLastBlockApplicationHash())) // hash of the store used for consensus
				.setLastBlockHeight(getLastBlockHeight()).build();
		}

		@Override
		protected ResponseCheckTx checkTx(RequestCheckTx request) {
			var tx = request.getTx();
	        ResponseCheckTx.Builder responseBuilder = ResponseCheckTx.newBuilder();

	        try (var context = NodeUnmarshallingContexts.of(new ByteArrayInputStream(tx.toByteArray()))) {
	        	var hotmokaRequest = TransactionRequests.from(context);

	        	try {
	        		checkTransaction(hotmokaRequest);
	        	}
	        	catch (TransactionRejectedException e) {
	        		signalRejected(hotmokaRequest, e);
	        		throw e;
	        	}

	        	responseBuilder.setCode(0);
	        }
	        catch (Throwable t) {
	        	responseBuilder.setCode(t instanceof TransactionRejectedException ? 1 : 2);
	        	responseBuilder.setData(ByteString.copyFromUtf8(t.getMessage()));
			}

	        return responseBuilder.build();
		}

		@Override
		protected ResponseBeginBlock beginBlock(RequestBeginBlock request) {
			behaving = spaceSeparatedSequenceOfBehavingValidatorsAddresses(request);
	    	misbehaving = spaceSeparatedSequenceOfMisbehavingValidatorsAddresses(request);

	    	try {
	    		transformation = storeOfHead.beginTransformation(timeOfBlock(request));
	    	}
	    	catch (StoreException e) {
	    		throw new RuntimeException(e); // TODO
	    	}

	    	// the ABCI might start too early, before the Tendermint process is up
	        if (validatorsAtPreviousBlock == null)
	        	validatorsAtPreviousBlock = poster.getTendermintValidators().toArray(TendermintValidator[]::new);

	        return ResponseBeginBlock.newBuilder().build();
		}

		@Override
		protected ResponseDeliverTx deliverTx(RequestDeliverTx request) {
			var tx = request.getTx();
	        ResponseDeliverTx.Builder responseBuilder = ResponseDeliverTx.newBuilder();

	        try (var context = NodeUnmarshallingContexts.of(new ByteArrayInputStream(tx.toByteArray()))) {
	        	var hotmokaRequest = TransactionRequests.from(context);

	        	try {
	        		transformation.deliverTransaction(hotmokaRequest);
	        		responseBuilder.setCode(0);
	        	}
	        	catch (TransactionRejectedException e) {
	        		signalRejected(hotmokaRequest, e);
	        		responseBuilder.setCode(1);
	            	responseBuilder.setData(ByteString.copyFromUtf8(e.getMessage()));
	        	}
	        }
	        catch (Throwable t) {
	        	responseBuilder.setCode(2);
	        	responseBuilder.setData(ByteString.copyFromUtf8(t.getMessage()));
	        }

	        return responseBuilder.build();
		}

		@Override
		protected ResponseEndBlock endBlock(RequestEndBlock request) {
	    	ResponseEndBlock.Builder builder = ResponseEndBlock.newBuilder();
	    	TendermintValidator[] currentValidators = validatorsAtPreviousBlock;

	    	if (currentValidators != null) {
	    		Optional<TendermintValidator[]> validatorsInStore = transformation.getTendermintValidators();
	    		if (validatorsInStore.isPresent()) {
	    			TendermintValidator[] nextValidators = validatorsInStore.get();
	    			if (nextValidators.length == 0)
	    				LOGGER.info("refusing to remove all validators; please initialize the node with TendermintInitializedNode");
	    			else {
	    				removeCurrentValidatorsThatAreNotNextValidators(currentValidators, nextValidators, builder);
	    				addNextValidatorsThatAreNotCurrentValidators(currentValidators, nextValidators, builder);
	    				updateValidatorsThatChangedPower(currentValidators, nextValidators, builder);
	    				validatorsAtPreviousBlock = nextValidators;
	    			}
	    		}
	    	}

	    	return builder.build();
		}

		@Override
		protected ResponseCommit commit(RequestCommit request) throws NodeException {
			try {
				transformation.deliverRewardTransaction(behaving, misbehaving);

				StateId idOfNewStoreOfHead = CheckSupplier.check(NodeException.class, StoreException.class, () -> getEnvironment().computeInTransaction(UncheckFunction.uncheck(txn -> {
					StateId stateIdOfFinalStore = transformation.getIdOfFinalStore(txn);
					setRootBranch(stateIdOfFinalStore, txn);
					persist(stateIdOfFinalStore, transformation.getNow(), txn);
					keepPersistedOnlyNotOlderThan(transformation.getNow(), txn);
					return stateIdOfFinalStore;
				})));

				persisted++;

				storeOfHead = mkStore(idOfNewStoreOfHead, Optional.of(transformation.getCache()));
				CheckRunnable.check(NodeException.class, () -> transformation.getDeliveredTransactions().forEachOrdered(UncheckConsumer.uncheck(reference -> publish(reference, storeOfHead))));

				byte[] hash = getLastBlockApplicationHash();
				LOGGER.info("committed Tendermint state " + Hex.toHexString(hash).toUpperCase());
				return ResponseCommit.newBuilder().setData(ByteString.copyFrom(hash)).build();
			}
			catch (StoreException | ExodusException | UnknownStateIdException e) {
				LOGGER.log(Level.SEVERE, "commit failed", e);
				throw new NodeException(e);
			}
			catch (InterruptedException e) {
				LOGGER.log(Level.WARNING, "commit interrupted", e);
				Thread.currentThread().interrupt();
				throw new NodeException(e);
			}
		}

		@Override
		protected ResponseQuery query(RequestQuery request) {
			return ResponseQuery.newBuilder().setLog("nop").build();
		}

		@Override
		protected ResponseFlush flush(RequestFlush request) {
			return ResponseFlush.newBuilder().build();
		}
	}
}