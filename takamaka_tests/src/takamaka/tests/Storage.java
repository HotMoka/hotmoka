/**
 * 
 */
package takamaka.tests;

import static io.takamaka.code.blockchain.types.BasicTypes.INT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.takamaka.code.blockchain.AbstractSequentialBlockchain;
import io.takamaka.code.blockchain.Classpath;
import io.takamaka.code.blockchain.CodeExecutionException;
import io.takamaka.code.blockchain.ConstructorSignature;
import io.takamaka.code.blockchain.NonVoidMethodSignature;
import io.takamaka.code.blockchain.TransactionException;
import io.takamaka.code.blockchain.TransactionReference;
import io.takamaka.code.blockchain.VoidMethodSignature;
import io.takamaka.code.blockchain.request.ConstructorCallTransactionRequest;
import io.takamaka.code.blockchain.request.GameteCreationTransactionRequest;
import io.takamaka.code.blockchain.request.InstanceMethodCallTransactionRequest;
import io.takamaka.code.blockchain.request.JarStoreInitialTransactionRequest;
import io.takamaka.code.blockchain.request.JarStoreTransactionRequest;
import io.takamaka.code.blockchain.types.ClassType;
import io.takamaka.code.blockchain.values.IntValue;
import io.takamaka.code.blockchain.values.StorageReference;
import io.takamaka.code.blockchain.values.StorageValue;
import io.takamaka.code.memory.MemoryBlockchain;

/**
 * A test for the simple storage class.
 */
class Storage extends TakamakaTest {

	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);

	private static final ClassType SIMPLE_STORAGE = new ClassType("io.takamaka.tests.storage.SimpleStorage");

	private static final ConstructorSignature CONSTRUCTOR_SIMPLE_STORAGE = new ConstructorSignature("io.takamaka.tests.storage.SimpleStorage");

	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private AbstractSequentialBlockchain blockchain;

	/**
	 * The first object, that holds all funds initially.
	 */
	private StorageReference gamete;

	/**
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new MemoryBlockchain(Paths.get("chain"));

		TransactionReference takamaka_base = blockchain.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest
			(Files.readAllBytes(Paths.get("../takamaka_distribution/dist/io-takamaka-code-1.0.jar"))));
		Classpath takamakaBase = new Classpath(takamaka_base, false);  // true/false irrelevant here

		gamete = blockchain.addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaBase, ALL_FUNDS));

		TransactionReference storage = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(gamete, _20_000, takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/storage.jar")), takamakaBase));

		classpath = new Classpath(storage, true);
	}

	@Test @DisplayName("new SimpleStorage().get() is an int")
	void neverInitializedStorageYieldsInt() throws TransactionException, CodeExecutionException {
		StorageReference storage = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, classpath, CONSTRUCTOR_SIMPLE_STORAGE));
		StorageValue value = blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, classpath, new NonVoidMethodSignature(SIMPLE_STORAGE, "get", INT), storage));
		assertTrue(value instanceof IntValue);
	}

	@Test @DisplayName("new SimpleStorage().get() == 0")
	void neverInitializedStorageYields0() throws TransactionException, CodeExecutionException {
		StorageReference storage = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, classpath, CONSTRUCTOR_SIMPLE_STORAGE));
		IntValue value = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, classpath, new NonVoidMethodSignature(SIMPLE_STORAGE, "get", INT), storage));
		assertEquals(value.value, 0);
	}

	@Test @DisplayName("new SimpleStorage().set(13) then get() == 13")
	void set13ThenGet13() throws TransactionException, CodeExecutionException {
		StorageReference storage = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, classpath, CONSTRUCTOR_SIMPLE_STORAGE));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, classpath, new VoidMethodSignature(SIMPLE_STORAGE, "set", INT), storage, new IntValue(13)));
		IntValue value = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, classpath, new NonVoidMethodSignature(SIMPLE_STORAGE, "get", INT), storage));
		assertEquals(value.value, 13);
	}

	@Test @DisplayName("new SimpleStorage().set(13) then set(17) then get() == 17")
	void set13set17ThenGet17() throws TransactionException, CodeExecutionException {
		StorageReference storage = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, classpath, CONSTRUCTOR_SIMPLE_STORAGE));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, classpath, new VoidMethodSignature(SIMPLE_STORAGE, "set", INT), storage, new IntValue(13)));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, classpath, new VoidMethodSignature(SIMPLE_STORAGE, "set", INT), storage, new IntValue(17)));
		IntValue value = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, classpath, new NonVoidMethodSignature(SIMPLE_STORAGE, "get", INT), storage));
		assertEquals(value.value, 17);
	}
}