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

package io.hotmoka.tools.internal.moka;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.util.Base64;

import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.Base58;
import io.hotmoka.helpers.ManifestHelpers;
import io.hotmoka.helpers.MintBurnHelpers;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.SignatureAlgorithmForTransactionRequests;
import io.hotmoka.node.api.Node;
import io.hotmoka.remote.RemoteNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "burn",
	description = "Burns coins from an account, if the node allows it",
	showDefaultValues = true)
public class Burn extends AbstractCommand {

	@Parameters(description = "the Base58-encoded public key of the account")
    private String keyOfAccount;

	@Parameters(description = "the amount of coins to burn", defaultValue = "0")
    private BigInteger amount;

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
	private String url;

	@Option(names = { "--password-of-gamete" }, description = "the password of the gamete account; if not specified, it will be asked interactively")
    private String passwordOfGamete;

	@Option(names = { "--interactive" }, description = "run in interactive mode", defaultValue = "true") 
	private boolean interactive;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final Node node;

		private Run() throws Exception {
			// TODO: add graceful error message if the node does not allow to burn
			checkPublicKey(keyOfAccount);
			if (amount.signum() < 0)
				throw new CommandException("The amount of coins to burn cannot be negative");

			passwordOfGamete = ensurePassword(passwordOfGamete, "the gamete account", interactive, false);

			try (Node node = this.node = RemoteNode.of(remoteNodeConfig(url))) {
				burn();
			}
		}

		private void burn() throws Exception {
			var manifestHelper = ManifestHelpers.of(node);
			StorageReference gamete = manifestHelper.getGamete();
			KeyPair keys;

			try {
				keys = readKeys(Accounts.of(gamete), node, passwordOfGamete);
			}
			catch (IOException | ClassNotFoundException e) {
				System.err.println("Cannot read the keys of the gamete: they were expected to be stored in file " + gamete + ".pem");
				throw e;
			}

			// from Base58 to Base64
			String publicKeyOfAccountBase64Encoded = Base64.getEncoder().encodeToString(Base58.decode(keyOfAccount));

			StorageReference account = MintBurnHelpers.of(node).burn(keys, SignatureAlgorithmForTransactionRequests.ed25519(), publicKeyOfAccountBase64Encoded, amount);

			System.out.println("Burned " + amount + " coins from account " + account);
		}
	}
}