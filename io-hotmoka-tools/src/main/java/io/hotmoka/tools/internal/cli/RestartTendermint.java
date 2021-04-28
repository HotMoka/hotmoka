package io.hotmoka.tools.internal.cli;

import java.math.BigInteger;
import java.nio.file.Path;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.nodes.ManifestHelper;
import io.hotmoka.service.NodeService;
import io.hotmoka.service.NodeServiceConfig;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.TendermintBlockchainConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "restart-tendermint",
	description = "Restarts an existing Hotmoka node based on Tendermint",
	showDefaultValues = true)
public class RestartTendermint extends AbstractCommand {

	@Option(names = { "--tendermint-config" }, description = "the directory of the Tendermint configuration of the node", defaultValue = "io-hotmoka-tools/tendermint_configs/v1n0/node0")
	private Path tendermintConfig;

	@Option(names = { "--max-gas-per-view" }, description = "the maximal gas limit accepted for calls to @View methods", defaultValue = "1000000") 
	private BigInteger maxGasPerView;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final NodeServiceConfig networkConfig;
		private final TendermintBlockchain node;

		private Run() throws Exception {
			TendermintBlockchainConfig nodeConfig = new TendermintBlockchainConfig.Builder()
				.setTendermintConfigurationToClone(tendermintConfig)
				.setMaxGasPerViewTransaction(maxGasPerView)
				.build();

			networkConfig = new NodeServiceConfig.Builder()
				.build();

			try (TendermintBlockchain node = this.node = TendermintBlockchain.restart(nodeConfig);
				NodeService service = NodeService.of(networkConfig, node)) {

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
			System.out.println("The Hotmoka node has been published at localhost:" + networkConfig.port);
			System.out.println("Try for instance in a browser: http://localhost:" + networkConfig.port + "/get/manifest");
		}

		private void printManifest() throws TransactionRejectedException, TransactionException, CodeExecutionException {
			System.out.println("\nThe following node has been restarted:\n" + new ManifestHelper(node));
		}
	}
}