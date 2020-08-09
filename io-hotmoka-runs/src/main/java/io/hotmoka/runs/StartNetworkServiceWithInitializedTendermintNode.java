package io.hotmoka.runs;

import static io.takamaka.code.constants.Constants.MANIFEST_NAME;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import io.hotmoka.network.NodeService;
import io.hotmoka.network.NodeServiceConfig;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.views.InitializedNode;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.TendermintBlockchainConfig;

/**
 * An example that shows how to create a brand new Tendermint blockchain and publish a server bound to it.
 * 
 * This class is meant to be run from the parent directory, after building the project, with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.network/io.hotmoka.runs.StartNetworkServiceWithInitializedTendermintNode
 */
public class StartNetworkServiceWithInitializedTendermintNode {

	/**
	 * Initial stakes.
	 */
	private final static BigInteger GREEN = BigInteger.valueOf(1_000_000);
	private final static BigInteger RED = GREEN;

	public static void main(String[] args) throws Exception {
		TendermintBlockchainConfig nodeConfig = new TendermintBlockchainConfig.Builder().build();
		NodeServiceConfig networkConfig = new NodeServiceConfig.Builder().setSpringBannerModeOn(true).build();
		Path takamakaCodeJar = Paths.get("modules/explicit/io-takamaka-code-1.0.0.jar");

		try (Node original = TendermintBlockchain.of(nodeConfig);
			 Node initialized = InitializedNode.of(original, takamakaCodeJar, MANIFEST_NAME, StartNetworkServiceWithInitializedTendermintNode.class.getName(), GREEN, RED);
			 NodeService service = NodeService.of(networkConfig, initialized)) {

			System.out.println("\nio-takamaka-code-1.0.0.jar installed at " + curl(new URL("http://localhost:8080/get/takamakaCode")));
			System.out.println("\nPress enter to turn off the server and exit this program");
			System.console().readLine();
		}
	}

	private static String curl(URL url) throws IOException {
	    try (InputStream is = url.openStream();
	    	 BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
	    	return br.lines().collect(Collectors.joining("\n"));
	    }
	}
}