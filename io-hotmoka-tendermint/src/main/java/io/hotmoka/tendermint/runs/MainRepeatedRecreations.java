package io.hotmoka.tendermint.runs;

import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.PrivateKey;

import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.views.InitializedNode;
import io.hotmoka.nodes.views.NodeWithAccounts;
import io.hotmoka.tendermint.Config;
import io.hotmoka.tendermint.TendermintBlockchain;

/**
 * Creates a brand new blockchain.
 */
public class MainRepeatedRecreations {

	private static final BigInteger _2_000_000 = BigInteger.valueOf(2_000_000);
	private static final BigInteger _100 = BigInteger.valueOf(100);

	/**
	 * Initial green stake.
	 */
	private final static BigInteger GREEN = BigInteger.valueOf(999_999_999).pow(5);

	/**
	 * Initial red stake.
	 */
	private final static BigInteger RED = BigInteger.valueOf(999_999_999).pow(5);

	public static void main(String[] args) throws Exception {
		Config config = new Config.Builder().build();
		StorageReference account;
		PrivateKey privateKey;
		StorageReference newAccount;

		try (Node node = TendermintBlockchain.of(config)) {
			// update version number when needed
			InitializedNode initializedView = InitializedNode.of(node, Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.0.jar"), GREEN, RED);
			NodeWithAccounts viewWithAccounts = NodeWithAccounts.of(initializedView, initializedView.keysOfGamete().getPrivate(), _2_000_000);
			System.out.println("takamakaCode: " + viewWithAccounts.getTakamakaCode());
			account = newAccount = viewWithAccounts.account(0);
			privateKey = viewWithAccounts.privateKey(0);
		}

		config = new Config.Builder()
			.setDelete(false) // reuse the state already created by a previous execution
			.build();

		for (int i = 0; i < 100; i++)
			try (Node node = TendermintBlockchain.of(config)) {
				// before creating a new account, we check if the previously created
				// is still accessible
				node.getState(newAccount).forEach(System.out::println);
				newAccount = NodeWithAccounts.of(node, account, privateKey, _100).account(0);
				System.out.println("done #" + i);
			}
			catch (Exception e) {
				e.printStackTrace();
				break;
			}
	}
}