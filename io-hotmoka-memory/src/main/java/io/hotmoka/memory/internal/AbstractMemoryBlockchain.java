package io.hotmoka.memory.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.takamaka.code.engine.AbstractNode;
import io.takamaka.code.engine.ResponseBuilder;

/**
 * An implementation of a blockchain that stores transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining.
 * Updates are stored inside the blocks, rather than in an external database.
 */
public abstract class AbstractMemoryBlockchain extends AbstractNode {

	/**
	 * The name used for the file containing the serialized header of a block.
	 */
	private static final Path HEADER_NAME = Paths.get("header");

	/**
	 * The name used for the file containing the textual header of a block.
	 */
	private final static Path HEADER_TXT_NAME = Paths.get("header.txt");

	/**
	 * The name used for the file containing the serialized request of a transaction.
	 */
	private final static Path REQUEST_NAME = Paths.get("request");

	/**
	 * The name used for the file containing the serialized response of a transaction.
	 */
	private final static Path RESPONSE_NAME = Paths.get("response");

	/**
	 * The name used for the file containing the textual request of a transaction.
	 */
	private final static Path REQUEST_TXT_NAME = Paths.get("request.txt");

	/**
	 * The name used for the file containing the textual response of a transaction.
	 */
	private final static Path RESPONSE_TXT_NAME = Paths.get("response.txt");

	/**
	 * The number of transactions that fit inside a block.
	 */
	public final static int TRANSACTIONS_PER_BLOCK = 5;

	/**
	 * The root path where the blocks are stored.
	 */
	private final Path root;

	/**
	 * The reference, in the blockchain, where the base Takamaka classes have been installed.
	 */
	private final Classpath takamakaCode;

	/**
	 * The reference that identifies the next transaction, that will run for the next request.
	 */
	private MemoryTransactionReference next;

	/**
	 * The histories of the objects created in blockchain. In a real implementation, this must
	 * be stored in a persistent state.
	 */
	private final Map<StorageReference, TransactionReference[]> histories = new HashMap<>();

	/**
	 * True if and only if this node doesn't allow initial transactions anymore.
	 */
	private boolean initialized;

	/**
	 * The identifier that can be used in futures of posted transactions.
	 */
	private BigInteger id = BigInteger.ONE;

	/**
	 * Builds a blockchain that stores transaction in disk memory.
	 * 
	 * @param root the directory where blocks and transactions must be stored.
	 * @throws IOException if the root directory cannot be created
	 * @throws TransactionException if the initialization of the blockchain fails
	 */
	protected AbstractMemoryBlockchain(Path takamakaCodePath) throws IOException, TransactionRejectedException {
		this.root = Paths.get("chain");
		ensureDeleted(root);  // cleans the directory where the blockchain lives
		Files.createDirectories(root);
		this.next = new MemoryTransactionReference(BigInteger.ZERO, (short) 0);
		createHeaderOfCurrentBlock();
		TransactionReference support = addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(takamakaCodePath)));
		this.takamakaCode = new Classpath(support, false);
	}

	public final Classpath takamakaCode() {
		return takamakaCode;
	}

	@Override
	public long getNow() throws Exception {
		// we access the block header where the transaction would be added
		Path headerPath = getPathInBlockFor(next.blockNumber, HEADER_NAME);
		try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(headerPath)))) {
			return ((MemoryBlockHeader) in.readObject()).time;
		}
	}

	@Override
	public void close() throws Exception {
		super.close();
		// nothing to close
	}

	@Override
	protected Stream<TransactionReference> getHistoryOf(StorageReference object) {
		TransactionReference[] history = histories.get(object);
		return history == null ? Stream.empty() : Stream.of(history);
	}

	@Override
	protected void setHistory(StorageReference object, Stream<TransactionReference> history) {
		histories.put(object, history.toArray(TransactionReference[]::new));
	}

	@Override
	protected boolean isInitialized() {
		return initialized;
	}

	@Override
	protected void markAsInitialized() {
		initialized = true;
	}

	@Override
	protected TransactionReference next() {
		return next;
	}

	private final Object lockGetNextTransactionReferenceAndIncrement = new Object();

	@Override
	protected TransactionReference nextAndIncrement() {
		TransactionReference result;

		synchronized (lockGetNextTransactionReferenceAndIncrement) {
			result = next;
			next = next.getNext();

			if (next.transactionNumber == 0)
				try {
					createHeaderOfCurrentBlock();
				}
				catch (Exception e) {}
		}

		return result;
	}

	@Override
	protected TransactionReference addJarStoreInitialTransactionInternal(JarStoreInitialTransactionRequest request) throws Exception {
		TransactionReference transactionReference = nextAndIncrement();
		JarStoreInitialTransactionResponse response = ResponseBuilder.of(request, this).build(transactionReference);
		expandStoreWith(transactionReference, request, response);
		return response.getOutcomeAt(transactionReference);
	}

	@Override
	protected StorageReference addGameteCreationTransactionInternal(GameteCreationTransactionRequest request) throws Exception {
		TransactionReference reference = nextAndIncrement();
		GameteCreationTransactionResponse response = ResponseBuilder.of(request, this).build(reference);
		expandStoreWith(reference, request, response);
		return response.getOutcome();
	}

	@Override
	protected StorageReference addRedGreenGameteCreationTransactionInternal(RedGreenGameteCreationTransactionRequest request) throws Exception {
		TransactionReference reference = nextAndIncrement();
		GameteCreationTransactionResponse response = ResponseBuilder.of(request, this).build(reference);
		expandStoreWith(reference, request, response);
		return response.getOutcome();
	}

	@Override
	protected JarStoreFuture postJarStoreTransactionInternal(JarStoreTransactionRequest request) throws Exception {
		TransactionReference transactionReference = nextAndIncrement();
		JarStoreTransactionResponse response = ResponseBuilder.of(request, this).build(transactionReference);
		expandStoreWith(transactionReference, request, response);
		String hash = String.valueOf(id);
		id = id.add(BigInteger.ONE);

		return new JarStoreFuture() {

			@Override
			public TransactionReference get() throws TransactionException {
				return response.getOutcomeAt(transactionReference);
			}

			@Override
			public TransactionReference get(long timeout, TimeUnit unit) throws TransactionException, TimeoutException {
				return response.getOutcomeAt(transactionReference);
			}

			@Override
			public String id() {
				return hash;
			}
		};
	}

	@Override
	protected CodeExecutionFuture<StorageReference> postConstructorCallTransactionInternal(ConstructorCallTransactionRequest request) throws Exception {
		TransactionReference reference = nextAndIncrement();
		ConstructorCallTransactionResponse response = ResponseBuilder.of(request, this).build(reference);
		expandStoreWith(reference, request, response);
		String hash = String.valueOf(id);
		id = id.add(BigInteger.ONE);

		return new CodeExecutionFuture<StorageReference>() {

			@Override
			public StorageReference get() throws TransactionException, CodeExecutionException {
				return response.getOutcome();
			}

			@Override
			public StorageReference get(long timeout, TimeUnit unit) throws TransactionException, CodeExecutionException, TimeoutException {
				return response.getOutcome();
			}

			@Override
			public String id() {
				return hash;
			}
		};
	}

	@Override
	protected CodeExecutionFuture<StorageValue> postInstanceMethodCallTransactionInternal(InstanceMethodCallTransactionRequest request) throws Exception {
		TransactionReference reference = nextAndIncrement();
		MethodCallTransactionResponse response = ResponseBuilder.of(request, this).build(reference);
		expandStoreWith(reference, request, response);
		String hash = String.valueOf(id);
		id = id.add(BigInteger.ONE);

		return new CodeExecutionFuture<StorageValue>() {

			@Override
			public StorageValue get() throws TransactionException, CodeExecutionException {
				return response.getOutcome();
			}

			@Override
			public StorageValue get(long timeout, TimeUnit unit) throws TransactionException, CodeExecutionException, TimeoutException {
				return response.getOutcome();
			}

			@Override
			public String id() {
				return hash;
			}
		};
	}

	@Override
	protected CodeExecutionFuture<StorageValue> postStaticMethodCallTransactionInternal(StaticMethodCallTransactionRequest request) throws Exception {
		TransactionReference reference = nextAndIncrement();
		MethodCallTransactionResponse response = ResponseBuilder.of(request, this).build(reference);
		expandStoreWith(reference, request, response);
		String hash = String.valueOf(id);
		id = id.add(BigInteger.ONE);

		return new CodeExecutionFuture<StorageValue>() {

			@Override
			public StorageValue get() throws TransactionException, CodeExecutionException {
				return response.getOutcome();
			}

			@Override
			public StorageValue get(long timeout, TimeUnit unit) throws TransactionException, CodeExecutionException, TimeoutException {
				return response.getOutcome();
			}

			@Override
			public String id() {
				return hash;
			}
		};
	}

	@Override
	protected void expandStoreWith(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) throws Exception {
		MemoryTransactionReference next = (MemoryTransactionReference) reference;
		Path requestPath = getPathFor(next, REQUEST_NAME);
		Path parent = requestPath.getParent();
		ensureDeleted(parent);
		Files.createDirectories(parent);

		try (ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(requestPath)))) {
			os.writeObject(request);
		}

		try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(getPathFor(next, REQUEST_TXT_NAME)))) {
			output.print(request);
		}

		try (ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(getPathFor((MemoryTransactionReference) reference, RESPONSE_NAME))))) {
			os.writeObject(response);
		}

		try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(getPathFor((MemoryTransactionReference) reference, RESPONSE_TXT_NAME)))) {
			output.print(response);
		}

		super.expandStoreWith(reference, request, response);
	}

	/**
	 * Creates the header of the current block.
	 * 
	 * @throws IOException if the header cannot be created
	 */
	private void createHeaderOfCurrentBlock() throws IOException {
		Path headerPath = getPathInBlockFor(next.blockNumber, HEADER_NAME);
		ensureDeleted(headerPath.getParent());
		Files.createDirectories(headerPath.getParent());

		MemoryBlockHeader header = new MemoryBlockHeader();

		try (ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(headerPath)))) {
			os.writeObject(header);
		}

		try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(getPathInBlockFor(next.blockNumber, HEADER_TXT_NAME)))) {
			output.print(header);
		}
	}

	@Override
	protected TransactionResponse getResponseAtInternal(TransactionReference reference) throws IOException, ClassNotFoundException {
		Path response = getPathFor((MemoryTransactionReference) reference, RESPONSE_NAME);
		try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(response)))) {
			return (TransactionResponse) in.readObject();
		}
	}

	/**
	 * Yields the path for the given file name inside the directory for the given transaction.
	 * 
	 * @param fileName the name of the file
	 * @return the path
	 */
	private Path getPathFor(MemoryTransactionReference reference, Path fileName) {
		return root.resolve("b" + reference.blockNumber).resolve("t" + reference.transactionNumber).resolve(fileName);
	}

	/**
	 * Yields the path for a file inside the given block.
	 * 
	 * @param blockNumber the number of the block
	 * @param fileName the file name
	 * @return the path
	 */
	private Path getPathInBlockFor(BigInteger blockNumber, Path fileName) {
		return root.resolve("b" + blockNumber).resolve(fileName);
	}

	/**
	 * Deletes the given directory, if it exists.
	 * 
	 * @param dir the directory
	 * @throws IOException if a disk error occurs
	 */
	private static void ensureDeleted(Path dir) throws IOException {
		if (Files.exists(dir))
			Files.walk(dir)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
	}

	/**
	 * The header of a block. It contains the time that must be used
	 * as {@code now} by the transactions that will be added to the block.
	 */
	private static class MemoryBlockHeader implements Serializable {
		private static final long serialVersionUID = 6163345302977772036L;
		private final static DateFormat formatter;

		static {
			formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
			formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		}

		/**
		 * The time of creation of the block, as returned by {@link java.lang.System#currentTimeMillis()}.
		 */
		private final long time;

		/**
		 * Builds block header.
		 */
		private MemoryBlockHeader() {
			this.time = System.currentTimeMillis();
		}

		@Override
		public String toString() {
			return "block creation time: " + time + " [" + formatter.format(new Date(time)) + " UTC]";
		}
	}
}