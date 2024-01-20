/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.beans;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import io.hotmoka.beans.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.api.requests.InitializationTransactionRequest;
import io.hotmoka.beans.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.api.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.signatures.ConstructorSignature;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.beans.internal.gson.TransactionReferenceDecoder;
import io.hotmoka.beans.internal.gson.TransactionReferenceEncoder;
import io.hotmoka.beans.internal.gson.TransactionReferenceJson;
import io.hotmoka.beans.internal.requests.ConstructorCallTransactionRequestImpl;
import io.hotmoka.beans.internal.requests.GameteCreationTransactionRequestImpl;
import io.hotmoka.beans.internal.requests.InitializationTransactionRequestImpl;
import io.hotmoka.beans.internal.requests.JarStoreInitialTransactionRequestImpl;
import io.hotmoka.beans.internal.requests.JarStoreTransactionRequestImpl;
import io.hotmoka.beans.internal.requests.TransactionRequestImpl;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * Providers of transaction requests.
 */
public abstract class TransactionRequests {

	private TransactionRequests() {}

	/**
	 * Yields a transaction request to install a jar in a yet non-initialized node.
	 * 
	 * @param jar the bytes of the jar to install
	 * @param dependencies the dependencies of the jar, already installed in blockchain
	 * @return the request
	 */
	public static JarStoreInitialTransactionRequest jarStoreInitial(byte[] jar, TransactionReference... dependencies) {
		return new JarStoreInitialTransactionRequestImpl(jar, dependencies);
	}

	/**
	 * Yields a transaction request to create a gamete.
	 * 
	 * @param classpath the reference to the jar containing the basic Takamaka classes. This must
	 *                  have been already installed by a previous transaction
	 * @param initialAmount the amount of green coins provided to the gamete
	 * @param redInitialAmount the amount of red coins provided to the gamete
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 * @return the request
	 */
	public static GameteCreationTransactionRequest gameteCreation(TransactionReference classpath, BigInteger initialAmount, BigInteger redInitialAmount, String publicKey) {
		return new GameteCreationTransactionRequestImpl(classpath, initialAmount, redInitialAmount, publicKey);
	}

	/**
	 * Yields a transaction request to mark the node as initialized.
	 * After this transaction, no more initial transactions can be executed.
	 * 
	 * @param classpath the reference to the jar containing the basic Takamaka classes. This must
	 *                  have been already installed by a previous transaction
	 * @param manifest the storage reference that must be set as manifest
	 * @return the request
	 */
	public static InitializationTransactionRequest initialization(TransactionReference classpath, StorageReference manifest) {
		return new InitializationTransactionRequestImpl(classpath, manifest);
	}

	/**
	 * Yields a transaction request to install a jar in an initialized node.
	 * 
	 * @param signature the signature of the request
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} is interpreted
	 * @param jar the bytes of the jar to install
	 * @param dependencies the dependencies of the jar, already installed in blockchain
	 * @return the request
	 */
	public static JarStoreTransactionRequest jarStore(byte[] signature, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, byte[] jar, TransactionReference... dependencies) {
		return new JarStoreTransactionRequestImpl(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, jar, dependencies);
	}

	/**
	 * Yields a transaction request to install a jar in an initialized node.
	 * 
	 * @param signer the signer of the request
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} is interpreted
	 * @param jar the bytes of the jar to install
	 * @param dependencies the dependencies of the jar, already installed in blockchain
	 * @return the request
	 * @throws SignatureException if the signer cannot sign the request
	 * @throws InvalidKeyException if the signer uses an invalid private key
	 */
	public static JarStoreTransactionRequest jarStore(Signer<? super JarStoreTransactionRequest> signer, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, byte[] jar, TransactionReference... dependencies) throws InvalidKeyException, SignatureException {
		return new JarStoreTransactionRequestImpl(signer, caller, nonce, chainId, gasLimit, gasPrice, classpath, jar, dependencies);
	}

	/**
	 * yields a transaction request to call a constructor in a node.
	 * 
	 * @param signature the signature of the request
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier where this request can be executed, to forbid transaction replay across chains; this can be {@code null}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param constructor the constructor that must be called
	 * @param actuals the actual arguments passed to the constructor
	 * @return the request
	 */
	public static ConstructorCallTransactionRequest constructorCall(byte[] signature, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue... actuals) {
		return new ConstructorCallTransactionRequestImpl(signature, caller, nonce, chainId, gasLimit, gasPrice, classpath, constructor, actuals);
	}

	/**
	 * yields a transaction request to call a constructor in a node.
	 * 
	 * @param signer the signer of the request
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param chainId the chain identifier of the network where the request will be sent
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param constructor the constructor that must be called
	 * @param actuals the actual arguments passed to the constructor
	 * @return the request
	 * @throws SignatureException if the signer cannot sign the request
	 * @throws InvalidKeyException if the signer uses an invalid private key
	 */
	public static ConstructorCallTransactionRequest constructorCall(Signer<? super ConstructorCallTransactionRequest> signer, StorageReference caller, BigInteger nonce, String chainId, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue... actuals) throws InvalidKeyException, SignatureException {
		return new ConstructorCallTransactionRequestImpl(signer, caller, nonce, chainId, gasLimit, gasPrice, classpath, constructor, actuals);
	}

	/**
	 * Yields a transaction request unmarshalled from the given context.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
     */
	public static TransactionRequest<?> from(UnmarshallingContext context) throws IOException {
		return TransactionRequestImpl.from(context);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends TransactionReferenceEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends TransactionReferenceDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

    /**
     * Json representation.
     */
    public static class Json extends TransactionReferenceJson {

    	/**
    	 * Creates the Json representation for the given transaction reference.
    	 * 
    	 * @param reference the transaction reference
    	 */
    	public Json(TransactionReference reference) {
    		super(reference);
    	}
    }
}