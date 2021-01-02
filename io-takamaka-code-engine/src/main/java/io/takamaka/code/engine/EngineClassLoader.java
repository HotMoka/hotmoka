package io.takamaka.code.engine;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.AbstractJarStoreTransactionRequest;
import io.hotmoka.beans.requests.InitialTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.beans.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithInstrumentedJar;
import io.hotmoka.beans.values.StorageReference;
import io.takamaka.code.instrumentation.InstrumentationConstants;
import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.verification.VerifiedJar;
import io.takamaka.code.whitelisting.WhiteListingWizard;

/**
 * A class loader used to access the definition of the classes
 * of Takamaka methods or constructors executed during a transaction.
 */
public class EngineClassLoader implements TakamakaClassLoader {

	/**
	 * The maximal number of dependencies in the classpath used to create an engine class loader.
	 */
	public final static int MAX_DEPENDENCIES = 20;

	/**
	 * The maximal cumulative size (in bytes) of the instrumented jars of the dependencies
	 * in the classpath used to create an engine class loader.
	 */
	public final static int MAX_SIZE_OF_DEPENDENCIES = 1_000_000;

	/**
	 * The parent of this class loader;
	 */
	private final TakamakaClassLoader parent;

	/**
	 * Method {@link io.takamaka.code.lang.Storage#entry(io.takamaka.code.lang.Contract)}.
	 */
	private final Method entry;

	/**
	 * Method {@link io.takamaka.code.lang.Contract#payableEntry(io.takamaka.code.lang.Contract, int)}.
	 */
	private final Method payableEntryInt;

	/**
	 * Method {@link io.takamaka.code.lang.Contract#payableEntry(io.takamaka.code.lang.Contract, long)}.
	 */
	private final Method payableEntryLong;

	/**
	 * Method {@link io.takamaka.code.lang.Contract#payableEntry(io.takamaka.code.lang.Contract, BigInteger)}.
	 */
	private final Method payableEntryBigInteger;

	/**
	 * Method {@link io.takamaka.code.lang.RedGreenContract#redPayable(io.takamaka.code.lang.RedGreenContract, int)}.
	 */
	private final Method redPayableInt;

	/**
	 * Method {@link io.takamaka.code.lang.RedGreenContract#redPayable(io.takamaka.code.lang.RedGreenContract, long)}.
	 */
	private final Method redPayableLong;

	/**
	 * Method {@link io.takamaka.code.lang.RedGreenContract#redPayable(io.takamaka.code.lang.RedGreenContract, BigInteger)}.
	 */
	private final Method redPayableBigInteger;

	/**
	 * Field {@link io.takamaka.code.lang.ExternallyOwnedAccount#nonce}.
	 */
	private final Field externallyOwnedAccountNonce;

	/**
	 * Field {@link io.takamaka.code.lang.RedGreenExternallyOwnedAccount#nonce}.
	 */
	private final Field redGreenExternallyOwnedAccountNonce;

	/**
	 * Field {@link io.takamaka.code.lang.ExternallyOwnedAccount#publicKey}.
	 */
	private final Field externallyOwnedAccountPublicKey;

	/**
	 * Field {@link io.takamaka.code.lang.RedGreenExternallyOwnedAccount#publicKey}.
	 */
	private final Field redGreenExternallyOwnedAccountPublicKey;

	/**
	 * Field {@link io.takamaka.code.lang.Storage#storageReference}.
	 */
	private final Field storageReference;

	/**
	 * Field {@link io.takamaka.code.lang.Storage#inStorage}.
	 */
	private final Field inStorage;

	/**
	 * Field {@link io.takamaka.code.lang.Contract#balance}.
	 */
	private final Field balanceField;

	/**
	 * Field {@link io.takamaka.code.lang.RedGreenContract#redBalance}.
	 */
	private final Field redBalanceField;

	/**
	 * The lengths (in bytes) of the instrumented jars of the classpath and its dependencies
	 * used to create this class loader.
	 */
	private final int[] lengthsOfJars;

	/**
	 * The transactions that installed the jars of the classpath and its dependencies
	 * used to create this class loader.
	 */
	private final TransactionReference[] transactionsOfJars;

	/**
	 * A map from each class name to the transaction that installed the jar it belongs to.
	 */
	private final ConcurrentMap<String, TransactionReference> transactionsThatInstalledJarForClasses = new ConcurrentHashMap<>();

	/**
	 * Responses that should be pushed into the store of the node, since they have been found to have
	 * a distinct verification version than that of the node and have been consequently reverified.
	 */
	private final Map<TransactionReference, JarStoreTransactionResponse> reverified = new HashMap<>();

	/**
	 * Builds the class loader for the given class path and its dependencies.
	 * 
	 * @param classpath the class path
	 * @param node the node for which the class loader is created
	 * @throws Exception if an error occurs
	 */
	public EngineClassLoader(TransactionReference classpath, AbstractLocalNode<?,?> node) throws Exception {
		this(null, Stream.of(classpath), node);
	}

	/**
	 * Builds the class loader for the given jar and its dependencies.
	 * 
	 * @param jar the jar
	 * @param dependencies the dependencies
	 * @param node the node for which the class loader is created
	 * @throws Exception if an error occurs
	 */
	public EngineClassLoader(byte[] jar, Stream<TransactionReference> dependencies, AbstractLocalNode<?,?> node) throws Exception {
		List<byte[]> jars = new ArrayList<>();
		ArrayList<TransactionReference> transactionsOfJars = new ArrayList<>();

		int verificationVersion = node.getVerificationVersion();
		List<TransactionReference> dependenciesAsList = dependencies.collect(Collectors.toList());
		dependenciesAsList.forEach(dependency -> reverify(dependency, node, verificationVersion));

		this.parent = mkTakamakaClassLoader(dependenciesAsList.stream(), jar, node, jars, transactionsOfJars);
		this.lengthsOfJars = jars.stream().mapToInt(bytes -> bytes.length).toArray();
		this.transactionsOfJars = transactionsOfJars.toArray(TransactionReference[]::new);
		Class<?> contract = getContract(), redGreenContract = getRedGreenContract(), storage = getStorage();
		this.entry = storage.getDeclaredMethod("entry", contract);
		this.entry.setAccessible(true); // it was private
		this.payableEntryInt = contract.getDeclaredMethod("payableEntry", contract, int.class);
		this.payableEntryInt.setAccessible(true); // it was private
		this.payableEntryLong = contract.getDeclaredMethod("payableEntry", contract, long.class);
		this.payableEntryLong.setAccessible(true); // it was private
		this.payableEntryBigInteger = contract.getDeclaredMethod("payableEntry", contract, BigInteger.class);
		this.payableEntryBigInteger.setAccessible(true); // it was private
		this.redPayableInt = redGreenContract.getDeclaredMethod("redPayable", redGreenContract, int.class);
		this.redPayableInt.setAccessible(true); // it was private
		this.redPayableLong = redGreenContract.getDeclaredMethod("redPayable", redGreenContract, long.class);
		this.redPayableLong.setAccessible(true); // it was private
		this.redPayableBigInteger = redGreenContract.getDeclaredMethod("redPayable", redGreenContract, BigInteger.class);
		this.redPayableBigInteger.setAccessible(true); // it was private
		this.redBalanceField = redGreenContract.getDeclaredField("balanceRed");
		this.redBalanceField.setAccessible(true); // it was private
		this.externallyOwnedAccountNonce = getExternallyOwnedAccount().getDeclaredField("nonce");
		this.externallyOwnedAccountNonce.setAccessible(true); // it was private
		this.redGreenExternallyOwnedAccountNonce = getRedGreenExternallyOwnedAccount().getDeclaredField("nonce");
		this.redGreenExternallyOwnedAccountNonce.setAccessible(true); // it was private
		this.externallyOwnedAccountPublicKey = getExternallyOwnedAccount().getDeclaredField("publicKey");
		this.externallyOwnedAccountPublicKey.setAccessible(true); // it was private
		this.redGreenExternallyOwnedAccountPublicKey = getRedGreenExternallyOwnedAccount().getDeclaredField("publicKey");
		this.redGreenExternallyOwnedAccountPublicKey.setAccessible(true); // it was private
		this.storageReference = storage.getDeclaredField(InstrumentationConstants.STORAGE_REFERENCE_FIELD_NAME);
		this.storageReference.setAccessible(true); // it was private
		this.inStorage = storage.getDeclaredField(InstrumentationConstants.IN_STORAGE);
		this.inStorage.setAccessible(true); // it was private
		this.balanceField = contract.getDeclaredField("balance");
		this.balanceField.setAccessible(true); // it was private
	}

	/**
	 * Reverifies the jars installed by the given transaction and by its dependencies,
	 * if they have a verification version different from that of the node.
	 * 
	 * @param classpath the transaction that has installed the jar
	 * @param node the node
	 * @param verificationVersion the version of the verification module of the node
	 * @return the responses of the requests that have tried to install classpath and all its dependencies;
	 *         this list can either be made of successfull responses only or it can contain a single failed response
	 */
	private List<JarStoreTransactionResponse> reverify(TransactionReference classpath, AbstractLocalNode<?,?> node, int verificationVersion) {
		
		// chiamate getResponseWithInstrumentedJarAtUncommitted per ottenere la risposta corrente R per classpath,
		// se generasse una NoSuchElementException ritornate una lista contenente solo node.getStore().getResponseUncommitted(classpath)
		// (castata)
		
		List<JarStoreTransactionResponse> list = new ArrayList<>();
		
		try {
			TransactionResponseWithInstrumentedJar currentResponseR = getResponseWithInstrumentedJarAtUncommitted(classpath, node);
			
			// chiamate reverify ricorsivamente su ciascuna delle dipendenze di tale risposta, ottenendo delle liste l_1...l_n			Stream<>  = ;
			List<TransactionReference> dependencies = currentResponseR.getDependencies().collect(Collectors.toList());
			for(TransactionReference d : dependencies) {
				List<JarStoreTransactionResponse> rev = reverify(d,node,verificationVersion);
				for(int i =0; i < rev.size();i++) {
					JarStoreTransactionResponse r = rev.get(i);
					if( r instanceof JarStoreTransactionFailedResponse){
						JarStoreTransactionFailedResponse failedResponse = ((JarStoreTransactionFailedResponse) r);
						
						// se una l_i contenesse una JarStoreTransactionFailedResponse, ritornate una lista contenente solo
						// una nuova JarStoreTransactionFailedResponse contenente le stesse informazioni di R e un messaggio
						// di errore del tipo "reverification failed for dependency i"; prima di ritornarla, memorizzatela
						// nella mappa reverified associandola alla chiave classpath
						JarStoreTransactionFailedResponse failedResponseR = new JarStoreTransactionFailedResponse(
								EngineClassLoader.class.getName(), "reverification failed for dependency " + i +" : " + d.toString(),
								failedResponse.getUpdates(), failedResponse.gasConsumedForCPU,
								failedResponse.gasConsumedForRAM, failedResponse.gasConsumedForStorage,
								failedResponse.gasConsumedForPenalty);
						
						reverified.put(classpath, failedResponseR);

						return List.of(failedResponseR);
						
					}else {
						// altrimenti concatenate tali liste per ottenere un'unica lista l
						list.addAll(rev);
					}

				}
			}
			
			
			// se la versione di R è verificationVersion, aggiungete R in fondo ad l e ritornate l
			if(currentResponseR.getVerificationVersion() == verificationVersion) {
				if(currentResponseR instanceof JarStoreTransactionResponse) {
					list.add((JarStoreTransactionResponse) currentResponseR);
				}else {
					//TODO: è sempre di tipo JarStoreTransactionResponse oppure ci possono essere altri casi da gestire ?
				}
			}else {
				// altrimenti costruite un TakamakaClassLoader usando i jars che ci sono dentro le l:
				// TakamakaClassLoader tcl = TakamakaClassLoader.of(jars, (name, pos) -> {})
				
					List<byte[]> jars = new ArrayList<>();
					dependencies.forEach(d -> {
						TransactionRequest<?> depRequest = node.getRequest(d);
						if(depRequest instanceof AbstractJarStoreTransactionRequest) {
							AbstractJarStoreTransactionRequest abstractJarReq = (AbstractJarStoreTransactionRequest) depRequest;
							jars.add(abstractJarReq.getJar());
						}
					});
					//TODO: è corretto come abbiamo preso i jars ? posso fare il cast senza controllo? 
					// sono sempre del tipo AbstractJarStoreTransactionRequest? o rischio di perdermi jar con il controllo di tipo?
					TakamakaClassLoader tcl = TakamakaClassLoader.of(jars.stream(), (name, pos) -> {});
					
					// poi prendete il jar della richiesta associata al classpath: node.getRequest(classpath) deve
					// essere una AbstractJarStoreTransactionRequest e dentro c'è un jar
					
					TransactionRequest<?> jarOfClassPath = node.getRequest(classpath);
					if(jarOfClassPath instanceof AbstractJarStoreTransactionRequest) {
						AbstractJarStoreTransactionRequest abstractJarReq = (AbstractJarStoreTransactionRequest) jarOfClassPath;
					
						// riverificate tale jar con le sue dipendenze:
						// VerifiedJar vj = VerifiedJar.of(jar, tcl, duringInitialization, allowSelfCharged);
						// il primo booleano dipende dal tipo di richiesta (iniziale o non iniziale)
						// il secondo booleano lo trovate dentro node.config
						
						VerifiedJar vj = VerifiedJar.of(abstractJarReq.getJar(), tcl, abstractJarReq instanceof InitialTransactionRequest, node.config.allowSelfCharged);
						
						// se vj non ha errori (!vj.hasErrors()):
						// create una JarStoreTransactionSuccessfulResponse
						// identica ad R ma con la versione aggiornata a verificationVersion e aggiungetela nella mappa reverified;
						// poi aggiungete questa nuova risposta in fondo ad l e ritornate l

						// se invece vj ha errori:
						// create una JarStoreTransactionFailedResponse usando l'informazione di R e "reverification failed" come messaggio
						// e aggiungetela nella mappa reverified; poi ritornate una lista contenente solo questa nuova risposta

						if((!vj.hasErrors())) {
							//TODO come faccio a ricavare il gas consumato se ho una InitialTransactionResponse?
							JarStoreTransactionSuccessfulResponse successfulResponse = new JarStoreTransactionSuccessfulResponse(currentResponseR.getInstrumentedJar(),
									currentResponseR.getDependencies(), verificationVersion, currentResponseR.getDependencies(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
						}else {
							//TODO come faccio a ricavare il gas consumato e updates se ho una InitialTransactionResponse?
							JarStoreTransactionFailedResponse failedResponseR = new JarStoreTransactionFailedResponse(
									EngineClassLoader.class.getName(), "reverification failed",
									abstractJarReq.getUpdates(), abstractJarReq.gasConsumedForCPU,
									abstractJarReq.gasConsumedForRAM, abstractJarReq.gasConsumedForStorage,
									abstractJarReq.gasConsumedForPenalty);
							
							reverified.put(classpath, failedResponseR);
							
							return List.of(failedResponseR);
						}
						
					}else {
						//TODO: posso fare il cast senza controllo? 
						// sono sempre del tipo AbstractJarStoreTransactionRequest? o rischio di perdermi il jar?
					}
			}
			

			

			
		}catch(NoSuchElementException e) {
			
			
			 Optional<TransactionResponse> response = node.getStore().getResponseUncommitted(classpath);
			if(response.isPresent() && response.get() instanceof JarStoreTransactionResponse) {
				return List.of(((JarStoreTransactionResponse) response.get()));
			}else {
				//TODO: è sempre di tipo JarStoreTransactionResponse oppure ci possono essere altri casi da gestire ?
			}
		}

		return list ;
	}

	/**
	 * Yields the Takamaka class loader for the components of the given classpaths.
	 * 
	 * @param classpaths the classpaths
	 * @param start an initial jar. This can be {@code null}
	 * @param node the node for which the class loader is created
	 * @return the class loader
	 */
	private TakamakaClassLoader mkTakamakaClassLoader(Stream<TransactionReference> classpaths, byte[] start, AbstractLocalNode<?,?> node, List<byte[]> jars, ArrayList<TransactionReference> transactionsOfJars) {
		if (start != null) {
			jars.add(start);
			transactionsOfJars.add(null);
		}

		classpaths.forEachOrdered(classpath -> addJars(classpath, jars, transactionsOfJars, node));

		return TakamakaClassLoader.of(jars.stream(), (name, pos) -> takeNoteOfTransactionThatInstalledJarFor(name, transactionsOfJars.get(pos)));
	}

	private void takeNoteOfTransactionThatInstalledJarFor(String className, TransactionReference transactionReference) {
		// if the transaction reference is null, it means that the class comes from a jar that is being installed
		// by the transaction that created this class loader. In that case, the storage reference of the class is not used
		if (transactionReference != null)
			transactionsThatInstalledJarForClasses.put(className, transactionReference);
	}

	/**
	 * Expands the given list of jars with the components of the given classpath.
	 * 
	 * @param classpath the classpath
	 * @param jars the list where the jars will be added
	 * @param jarTransactions the list of transactions where the {@code jars} have been installed
	 * @param node the node for which the class loader is created
	 */
	private void addJars(TransactionReference classpath, List<byte[]> jars, List<TransactionReference> jarTransactions, AbstractLocalNode<?,?> node) {
		if (jars.size() > MAX_DEPENDENCIES)
			throw new IllegalArgumentException("too many dependencies in classpath: max is " + MAX_DEPENDENCIES);

		if (jars.stream().mapToLong(bytes -> bytes.length).sum() > MAX_SIZE_OF_DEPENDENCIES)
			throw new IllegalArgumentException("too large cumulative size of dependencies in classpath: max is " + MAX_SIZE_OF_DEPENDENCIES + " bytes");

		TransactionResponseWithInstrumentedJar responseWithInstrumentedJar = getResponseWithInstrumentedJarAtUncommitted(classpath, node);

		// we consider its dependencies as well, recursively
		responseWithInstrumentedJar.getDependencies().forEachOrdered(dependency -> addJars(dependency, jars, jarTransactions, node));

		jars.add(responseWithInstrumentedJar.getInstrumentedJar());
		jarTransactions.add(classpath);
	}

	/**
	 * Yields the response generated by the transaction with the given reference, even
	 * before the transaction gets committed. The transaction must be a transaction that installed
	 * a jar in the store of the node.
	 * 
	 * @param reference the reference of the transaction
	 * @param node the node for which the class loader is created
	 * @return the response
	 * @throws NoSuchElementException if the transaction does not exist in the store, or
	 *                                did not generate a response with instrumented jar
	 */
	private TransactionResponseWithInstrumentedJar getResponseWithInstrumentedJarAtUncommitted(TransactionReference reference, AbstractLocalNode<?,?> node) throws NoSuchElementException {
		TransactionResponse response = (TransactionResponse) reverified.get(reference);
		if (response == null)
			response = node.getStore().getResponseUncommitted(reference)
				.orElseThrow(() -> new InternalFailureException("unknown transaction reference " + reference));
		
		if (!(response instanceof TransactionResponseWithInstrumentedJar))
			throw new NoSuchElementException("the transaction " + reference + " did not install a jar in store");
	
		return (TransactionResponseWithInstrumentedJar) response;
	}

	/**
	 * Yields the lengths (in bytes) of the instrumented jars of the classpath and its dependencies
	 * used to create this class loader.
	 * 
	 * @return the lengths
	 */
	public final IntStream getLengthsOfJars() {
		return IntStream.of(lengthsOfJars);
	}

	/**
	 * Yields the transactions that installed the jars of the classpath and its dependencies
	 * used to create this class loader.
	 * 
	 * @return the transactions
	 */
	public final Stream<TransactionReference> getTransactionsOfJars() {
		return Stream.of(transactionsOfJars);
	}

	/**
	 * Yields the transaction reference that installed the jar where the given class is defined.
	 * The class must belong to the class path used at creation time of this engine class loader
	 * (hence not to the extra jar provided in the second constructor).
	 * 
	 * @param clazz the class
	 * @return the transaction reference
	 */
	public final TransactionReference transactionThatInstalledJarFor(Class<?> clazz) {
		return transactionsThatInstalledJarForClasses.get(clazz.getName().replace('.', '/') + ".class");
	}

	/**
	 * Yields the value of the {@code storageReference} field of the given storage object in RAM.
	 * 
	 * @param object the object
	 * @return the value of the field
	 */
	public final StorageReference getStorageReferenceOf(Object object) {
		try {
			return (StorageReference) storageReference.get(object);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot read the storage reference of a storage object of class " + object.getClass().getName());
		}
	}

	/**
	 * Yields the value of the boolean {@code inStorage} field of the given storage object in RAM.
	 * 
	 * @param object the object
	 * @return the value of the field
	 */
	public final boolean getInStorageOf(Object object) {
		try {
			return (boolean) inStorage.get(object);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot read the inStorage tag of a storage object of class " + object.getClass().getName());
		}
	}

	/**
	 * Yields the value of the {@code nonce} field of the given account in RAM.
	 * 
	 * @param object the account
	 * @return the value of the field
	 */
	public final BigInteger getNonceOf(Object object) {
		Class<? extends Object> clazz = object.getClass();

		try {
			if (getExternallyOwnedAccount().isAssignableFrom(clazz))
				return (BigInteger) externallyOwnedAccountNonce.get(object);
			else if (getRedGreenExternallyOwnedAccount().isAssignableFrom(clazz))
				return (BigInteger) redGreenExternallyOwnedAccountNonce.get(object);
			else
				throw new IllegalArgumentException("unknown account class " + clazz);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot read the nonce of an account of class " + clazz.getName());
		}
	}

	/**
	 * Yields the value of the {@code publicKey} field of the given account in RAM.
	 * 
	 * @param object the account
	 * @return the value of the field
	 */
	public final String getPublicKeyOf(Object object) {
		Class<? extends Object> clazz = object.getClass();

		try {
			if (getExternallyOwnedAccount().isAssignableFrom(clazz))
				return (String) externallyOwnedAccountPublicKey.get(object);
			else if (getRedGreenExternallyOwnedAccount().isAssignableFrom(clazz))
				return (String) redGreenExternallyOwnedAccountPublicKey.get(object);
			else
				throw new IllegalArgumentException("unknown account class " + clazz);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot read the public key of an account of class " + clazz.getName());
		}
	}

	/**
	 * Yields the value of the {@code balance} field of the given contract in RAM.
	 * 
	 * @param object the contract
	 * @return the value of the field
	 */
	public final BigInteger getBalanceOf(Object object) {
		try {
			return (BigInteger) balanceField.get(object);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot read the balance field of a contract object of class " + object.getClass().getName());
		}
	}

	/**
	 * Yields the value of the {@code balanceRed} field of the given red/green contract in RAM.
	 * 
	 * @param object the contract
	 * @return the value of the field
	 */
	public final BigInteger getRedBalanceOf(Object object) {
		try {
			return (BigInteger) redBalanceField.get(object);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot read the red balance field of a contract object of class " + object.getClass().getName());
		}
	}

	/**
	 * Sets the value of the {@code balance} field of the given contract in RAM.
	 * 
	 * @param object the contract
	 * @param value to value to set for the balance of the contract
	 */
	public final void setBalanceOf(Object object, BigInteger value) {
		try {
			balanceField.set(object, value);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot write the balance field of a contract object of class " + object.getClass().getName());
		}
	}

	/**
	 * Sets the value of the {@code nonce} field of the given account in RAM.
	 * 
	 * @param object the account
	 * @param value to value to set for the nonce of the account
	 */
	public final void setNonceOf(Object object, BigInteger value) {
		Class<?> clazz = object.getClass();

		try {
			if (getExternallyOwnedAccount().isAssignableFrom(clazz))
				externallyOwnedAccountNonce.set(object, value);
			else if (getRedGreenExternallyOwnedAccount().isAssignableFrom(clazz))
				redGreenExternallyOwnedAccountNonce.set(object, value);
			else
				throw new IllegalArgumentException("unknown account class " + clazz);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot write the nonce field of an account object of class " + clazz.getName());
		}
	}

	/**
	 * Sets the val)ue of the {@code balanceRed} field of the given red/green contract in RAM.
	 * 
	 * @param object the contract
	 * @param value to value to set for the red balance of the contract
	 */
	public final void setRedBalanceOf(Object object, BigInteger value) {
		try {
			redBalanceField.set(object, value);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot write the red balance field of a contract object of class " + object.getClass().getName());
		}
	}

	/**
	 * Called at the beginning of the instrumentation of an entry method or constructor
	 * of a contract. It forwards the call to {@code io.takamaka.code.lang.Storage.entry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Storage.entry()}
	 */
	public final void entry(Object callee, Object caller) throws Throwable {
		// we call the private method of contract
		try {
			entry.invoke(callee, caller);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call Storage.entry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.entry() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.payableEntry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.payableEntry()}
	 */
	public final void payableEntry(Object callee, Object caller, BigInteger amount) throws Throwable {
		// we call the private method of contract
		try {
			payableEntryBigInteger.invoke(callee, caller, amount);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call Contract.payableEntry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.payableEntry() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a red payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.RedGreenContract.redPayable()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.RedGreenContract.redPayable()}
	 */
	public final void redPayableEntry(Object callee, Object caller, BigInteger amount) throws Throwable {
		try {
			redPayableBigInteger.invoke(callee, caller, amount);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call RedGreenContract.redPayableEntry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside RedGreenContract.redPayableEntry() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.payableEntry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.payableEntry()}
	 */
	public final void payableEntry(Object callee, Object caller, int amount) throws Throwable {
		// we call the private method of contract
		try {
			payableEntryInt.invoke(callee, caller, amount);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call Contract.payableEntry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.payableEntry() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a red payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.RedGreenContract.redPayable()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.RedGreenContract.redPayable()}
	 */
	public final void redPayableEntry(Object callee, Object caller, int amount) throws Throwable {
		try {
			redPayableInt.invoke(callee, caller, amount);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call RedGreenContract.redPayableEntry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside RedGreenContract.redPayableEntry(): we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.payableEntry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.payableEntry()}
	 */
	public final void payableEntry(Object callee, Object caller, long amount) throws Throwable {
		// we call the private method of contract
		try {
			payableEntryLong.invoke(callee, caller, amount);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call Contract.payableEntry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.payableEntry() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a red payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.RedGreenContract.redPayable()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.RedGreenContract.redPayable()}
	 */
	public final void redPayableEntry(Object callee, Object caller, long amount) throws Throwable {
		try {
			redPayableLong.invoke(callee, caller, amount);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call RedGreenContract.redPayableEntry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside RedGreenContract.redPayableEntry() itself: we forward it
			throw e.getCause();
		}
	}

	@Override
	public final Class<?> loadClass(String className) throws ClassNotFoundException {
		return parent.loadClass(className);
	}

	@Override
	public WhiteListingWizard getWhiteListingWizard() {
		return parent.getWhiteListingWizard();
	}

	@Override
	public Optional<Field> resolveField(String className, String name, Class<?> type) throws ClassNotFoundException {
		return parent.resolveField(className, name, type);
	}

	@Override
	public Optional<Field> resolveField(Class<?> clazz, String name, Class<?> type) {
		return parent.resolveField(clazz, name, type);
	}

	@Override
	public Optional<Constructor<?>> resolveConstructor(String className, Class<?>[] args) throws ClassNotFoundException {
		return parent.resolveConstructor(className, args);
	}

	@Override
	public Optional<Constructor<?>> resolveConstructor(Class<?> clazz, Class<?>[] args) {
		return parent.resolveConstructor(clazz, args);
	}

	@Override
	public Optional<Method> resolveMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		return parent.resolveMethod(className, methodName, args, returnType);
	}

	@Override
	public Optional<Method> resolveMethod(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) {
		return parent.resolveMethod(clazz, methodName, args, returnType);
	}

	@Override
	public Optional<Method> resolveInterfaceMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		return parent.resolveInterfaceMethod(className, methodName, args, returnType);
	}

	@Override
	public Optional<Method> resolveInterfaceMethod(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) {
		return parent.resolveInterfaceMethod(clazz, methodName, args, returnType);
	}

	@Override
	public boolean isStorage(String className) {
		return parent.isStorage(className);
	}

	@Override
	public boolean isContract(String className) {
		return parent.isContract(className);
	}

	@Override
	public boolean isRedGreenContract(String className) {
		return parent.isRedGreenContract(className);
	}

	@Override
	public boolean isInterface(String className) {
		return parent.isInterface(className);
	}

	@Override
	public boolean isExported(String className) {
		return parent.isExported(className);
	}

	@Override
	public boolean isLazilyLoaded(Class<?> type) {
		return parent.isLazilyLoaded(type);
	}

	@Override
	public boolean isEagerlyLoaded(Class<?> type) {
		return parent.isEagerlyLoaded(type);
	}

	@Override
	public Class<?> getContract() {
		return parent.getContract();
	}

	@Override
	public Class<?> getRedGreenContract() {
		return parent.getRedGreenContract();
	}

	@Override
	public Class<?> getStorage() {
		return parent.getStorage();
	}

	@Override
	public Class<?> getExternallyOwnedAccount() {
		return parent.getExternallyOwnedAccount();
	}

	@Override
	public Class<?> getRedGreenExternallyOwnedAccount() {
		return parent.getRedGreenExternallyOwnedAccount();
	}

	@Override
	public Class<?> getAccount() {
		return parent.getAccount();
	}

	@Override
	public Class<?> getAccountED25519() {
		return parent.getAccountED25519();
	}

	@Override
	public Class<?> getAccountQTESLA() {
		return parent.getAccountQTESLA();
	}

	@Override
	public Class<?> getAccountSHA256DSA() {
		return parent.getAccountSHA256DSA();
	}

	@Override
	public ClassLoader getJavaClassLoader() {
		return parent.getJavaClassLoader();
	}
}