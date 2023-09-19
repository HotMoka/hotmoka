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

import java.math.BigInteger;
import java.nio.file.Path;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.helpers.ManifestHelpers;
import io.hotmoka.node.tendermint.TendermintBlockchainConfigBuilders;
import io.hotmoka.node.tendermint.TendermintBlockchains;
import io.hotmoka.node.tendermint.api.TendermintBlockchain;
import io.hotmoka.service.NodeService;
import io.hotmoka.service.NodeServiceConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "resume-tendermint",
	description = "Resumes an existing node based on Tendermint",
	showDefaultValues = true)
public class ResumeTendermint extends AbstractCommand {

	@Option(names = { "--tendermint-config" }, description = "the directory of the Tendermint configuration of the node", defaultValue = "io-hotmoka-tools/tendermint_configs/v1n0/node0")
	private Path tendermintConfig;

	@Option(names = { "--dir" }, description = "the directory that contains blocks and state of the node", defaultValue = "chain")
	private Path dir;

	@Option(names = { "--max-gas-per-view" }, description = "the maximal gas limit accepted for calls to @View methods", defaultValue = "1000000") 
	private BigInteger maxGasPerView;

	@Option(names = { "--port" }, description = "the network port for the publication of the service", defaultValue="8080")
	private int port;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final NodeServiceConfig networkConfig;
		private final TendermintBlockchain node;

		private Run() throws Exception {
			var nodeConfig = TendermintBlockchainConfigBuilders.defaults()
				.setTendermintConfigurationToClone(tendermintConfig)
				.setMaxGasPerViewTransaction(maxGasPerView)
				.setDir(dir)
				.build();

			networkConfig = new NodeServiceConfig.Builder()
				.setPort(port)
				.build();

			try (var node = this.node = TendermintBlockchains.resume(nodeConfig); var service = NodeService.of(networkConfig, node)) {
				printManifest();
				printBanner();
				waitForEnterKey();
			}
		}

		private void waitForEnterKey() {
			System.out.println("Press enter to exit this program and turn off the node");
			System.console().readLine();
		}

		private void printBanner() {
			System.out.println("The node has been published at localhost:" + networkConfig.port);
			System.out.println("Try for instance in a browser: http://localhost:" + networkConfig.port + "/get/manifest");
		}

		private void printManifest() throws TransactionRejectedException, TransactionException, CodeExecutionException {
			System.out.println("\nThe following node has been restarted:\n" + ManifestHelpers.of(node));
		}
	}
}