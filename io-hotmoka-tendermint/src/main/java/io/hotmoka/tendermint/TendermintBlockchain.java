package io.hotmoka.tendermint;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Optional;

import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.InitializedNode;
import io.hotmoka.tendermint.internal.TendermintBlockchainImpl;

/**
 * An implementation of a blockchain that stores, sequentially, transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining.
 * Updates are stored inside the blocks, rather than in an external database.
 * It provides support for the creation of a given number of initial accounts.
 */
public interface TendermintBlockchain extends InitializedNode {

	/**
	 * Yields a fresh Tendermint blockchain and initializes user accounts with the given initial funds.
	 * This method spawns the Tendermint process on localhost and connects it to an ABCI application
	 * for handling its transactions. The blockchain gets deleted if it existed already at the given directory.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link io.hotmoka.memory.MemoryBlockchain#takamakaCode}
	 * @param funds the initial funds of the accounts that are created
	 * @throws Exception if the blockchain could not be created
	 */
	static TendermintBlockchain of(Config config, Path takamakaCodePath, BigInteger... funds) throws Exception {
		return new TendermintBlockchainImpl(config, takamakaCodePath, Optional.empty(), funds);
	}

	/**
	 * Yields a fresh Tendermint blockchain and initializes red/green user accounts with the given initial funds.
	 * The only different with respect to {@linkplain #of(Config, Path, BigInteger...)} is that the initial
	 * account are red/green externally owned accounts.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link io.hotmoka.memory.RedGreenMemoryBlockchain#takamakaCode()}
	 * @param funds the initial funds of the accounts that are created; they must be understood in pairs, each pair for the green/red
	 *              initial funds of each account (green before red)
	 * @throws Exception if the blockchain could not be created
	 */
	static TendermintBlockchain ofRedGreen(Config config, Path takamakaCodePath, BigInteger... funds) throws Exception {
		return new TendermintBlockchainImpl(config, takamakaCodePath, Optional.empty(), true, funds);
	}

	/**
	 * Yields a fresh Tendermint blockchain and initializes user accounts with the given initial funds.
	 * This method spawns the Tendermint process on localhost and connects it to an ABCI application
	 * for handling its transactions. The blockchain gets deleted if it existed already at the given directory.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link io.hotmoka.memory.MemoryBlockchain#takamakaCode}
	 * @param jar the path of a user jar that must be installed. This is optional and mainly useful to simplify the implementation of tests
	 * @param funds the initial funds of the accounts that are created
	 * @throws Exception if the blockchain could not be created
	 */
	static TendermintBlockchain of(Config config, Path takamakaCodePath, Path jar, BigInteger... funds) throws Exception {
		return new TendermintBlockchainImpl(config, takamakaCodePath, Optional.of(jar), funds);
	}

	/**
	 * Yields a fresh Tendermint blockchain and initializes red/green user accounts with the given initial funds.
	 * The only different with respect to {@linkplain #of(Config, Path, BigInteger...)} is that the initial
	 * account are red/green externally owned accounts.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link io.hotmoka.memory.RedGreenMemoryBlockchain#takamakaCode()}
	 * @param jar the path of a user jar that must be installed. This is optional and mainly useful to simplify the implementation of tests
	 * @param funds the initial funds of the accounts that are created; they must be understood in pairs, each pair for the green/red
	 *              initial funds of each account (green before red)
	 * @throws Exception if the blockchain could not be created
	 */
	static TendermintBlockchain ofRedGreen(Config config, Path takamakaCodePath, Path jar, BigInteger... funds) throws Exception {
		return new TendermintBlockchainImpl(config, takamakaCodePath, Optional.of(jar), true, funds);
	}

	/**
	 * Yields a Tendermint blockchain and initializes it with the information already
	 * existing at its configuration directory. This method can be used to
	 * recover a blockchain already created in the past, with all its information.
	 * A Tendermint blockchain must have been already successfully created at
	 * its configuration directory.
	 * 
	 * @param config the configuration of the blockchain
	 * @throws Exception if the blockchain could not be created
	 */
	static TendermintBlockchain of(Config config) throws Exception {
		return new TendermintBlockchainImpl(config);
	}

	/**
	 * Yields the reference, in the blockchain, where the base Takamaka classes have been installed.
	 */
	Classpath takamakaCode();

	/**
	 * Yields the {@code i}th account.
	 * 
	 * @param i the account number
	 * @return the reference to the account, in blockchain. This is a {@link #io.takamaka.code.lang.TestExternallyOwnedAccount}}
	 */
	StorageReference account(int i);
}