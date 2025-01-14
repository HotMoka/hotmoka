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

package io.hotmoka.node.tendermint.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import io.hotmoka.crypto.Base64;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.tendermint.api.TendermintNodeConfig;
import io.hotmoka.node.tendermint.internal.beans.TendermintBroadcastTxResponse;
import io.hotmoka.node.tendermint.internal.beans.TendermintGenesisResponse;
import io.hotmoka.node.tendermint.internal.beans.TendermintStatusResponse;
import io.hotmoka.node.tendermint.internal.beans.TendermintValidatorPriority;
import io.hotmoka.node.tendermint.internal.beans.TendermintValidatorsResponse;
import io.hotmoka.node.tendermint.internal.beans.TxError;

/**
 * An object that posts requests to a Tendermint process.
 */
public class TendermintPoster {
	private final static Logger logger = Logger.getLogger(TendermintPoster.class.getName());

	private final TendermintNodeConfig config;

	/**
	 * The port of the Tendermint process on localhost.
	 */
	private final int tendermintPort;

	/**
	 * An object for JSON manipulation.
	 */
	private final Gson gson = new Gson();

	private final AtomicInteger nextId = new AtomicInteger();

	TendermintPoster(TendermintNodeConfig config, int tendermintPort) {
		this.config = config;
		this.tendermintPort = tendermintPort;
	}

	/**
	 * Sends the given {@code request} to the Tendermint process, inside a {@code broadcast_tx_async} Tendermint request.
	 * 
	 * @param request the request to send
	 * @throws InterruptedException 
	 * @throws TimeoutException 
	 */
	void postRequest(TransactionRequest<?> request) throws NodeException, TimeoutException, InterruptedException {
		try {
			String jsonTendermintRequest = "{\"method\": \"broadcast_tx_async\", \"params\": {\"tx\": \"" + Base64.toBase64String(request.toByteArray()) + "\"}, \"id\": " + nextId.getAndIncrement() + "}";
			String response = postToTendermint(jsonTendermintRequest);

			TendermintBroadcastTxResponse parsedResponse = gson.fromJson(response, TendermintBroadcastTxResponse.class);
			TxError error = parsedResponse.error;
			if (error != null)
				throw new NodeException("Tendermint transaction failed: " + error.message + ": " + error.data);
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	String getTendermintChainId() {
		TendermintGenesisResponse response;

		try {
			response = gson.fromJson(genesis(), TendermintGenesisResponse.class);
		}
		catch (IOException | TimeoutException | InterruptedException e) {
			logger.log(Level.WARNING, "could not determine the Tendermint chain id for this node", e);
			throw new RuntimeException(e);
		}

		if (response.error != null)
			throw new RuntimeException(response.error);

		String chainId = response.result.genesis.chain_id;
		if (chainId == null)
			throw new RuntimeException("no chain id in Tendermint response");

		return chainId;
	}

	/**
	 * Yields the genesis time, in UTC pattern.
	 * 
	 * @return the genesis time, in UTC pattern
	 */
	String getGenesisTime() {
		TendermintGenesisResponse response;

		try {
			response = gson.fromJson(genesis(), TendermintGenesisResponse.class);
		}
		catch (IOException | TimeoutException | InterruptedException e) {
			logger.log(Level.WARNING, "could not determine the Tendermint genesis time for this node", e);
			throw new RuntimeException("unexpected exception", e);
		}

		if (response.error != null)
			throw new RuntimeException(response.error);

		String genesisTime = response.result.genesis.genesis_time;
		if (genesisTime == null)
			throw new RuntimeException("no genesis time in Tendermint response");

		return genesisTime;
	}

	/**
	 * Yields the Tendermint identifier of the node. This is the hexadecimal
	 * hash of the public key of the node and is used to identify the node as a peer in the network.
	 * 
	 * @return the hexadecimal ID of the node
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws TimeoutException 
	 * @throws JsonSyntaxException 
	 */
	String getNodeID() throws IOException, JsonSyntaxException, TimeoutException, InterruptedException {
		TendermintStatusResponse response = gson.fromJson(status(), TendermintStatusResponse.class);

		if (response == null)
			throw new IOException("null Tendermint status response");

		if (response.error != null)
			throw new IOException(response.error);

		if (response.result == null)
			throw new IOException("null Tendermint status result");

		if (response.result.node_info == null)
			throw new IOException("null Tendermint status node info");

		String id = response.result.node_info.id;
		if (id == null)
			throw new IOException("no node ID in Tendermint response");

		return id;
	}

	Stream<TendermintValidator> getTendermintValidators() {
		String jsonResponse;

		try {
			// the parameters of the validators() query seem to be ignored, no count nor total is returned
			jsonResponse = validators(1, 100);
		}
		catch (IOException | TimeoutException | InterruptedException e) {
			logger.log(Level.WARNING, "failed retrieving the validators of this node", e);
			throw new RuntimeException(e);
		} 

		TendermintValidatorsResponse response = gson.fromJson(jsonResponse, TendermintValidatorsResponse.class);
		if (response.error != null)
			throw new RuntimeException(response.error);

		return response.result.validators.stream().map(TendermintPoster::intoTendermintValidator);
	}

	/**
	 * Opens a http POST connection to the Tendermint process.
	 * 
	 * @return the connection
	 * @throws IOException if the connection cannot be opened
	 */
	HttpURLConnection openPostConnectionToTendermint() throws IOException {
		HttpURLConnection con = (HttpURLConnection) url().openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json; UTF-8");
		con.setRequestProperty("Accept", "application/json");
		con.setDoOutput(true);
	
		return con;
	}

	/**
	 * Yields the URL of the Tendermint process.
	 * 
	 * @return the URL
	 * @throws URISyntaxException if the URL is not well formed
	 */
	URL url() throws MalformedURLException {
		return URI.create("http://127.0.0.1:" + tendermintPort).toURL();
	}

	private static TendermintValidator intoTendermintValidator(TendermintValidatorPriority validatorPriority) {
		if (validatorPriority.address == null)
			throw new RuntimeException("unexpected null address in Tendermint validator");
		else if (validatorPriority.voting_power <= 0L)
			throw new RuntimeException("unexpected non-positive voting power in Tendermint validator");
		else if (validatorPriority.pub_key.value == null)
			throw new RuntimeException("unexpected null public key for Tendermint validator");
		else if (validatorPriority.pub_key.type == null)
			throw new RuntimeException("unexpected null public key type for Tendermint validator");
		else
			return new TendermintValidator(validatorPriority.address, validatorPriority.voting_power, validatorPriority.pub_key.value, validatorPriority.pub_key.type);
	}

	/**
	 * Sends a {@code validators} request to the Tendermint process, to read the
	 * list of current validators of the Tendermint network.
	 * 
	 * @param page the page number
	 * @return number of entries per page (max 100)
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing the request failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing the request
	 */
	private String validators(int page, int perPage) throws IOException, TimeoutException, InterruptedException {
		String jsonTendermintRequest = "{\"method\": \"validators\", \"params\": {\"page\": \"" + page + "\", \"per_page\": \"" + perPage + "\"}, \"id\": " + nextId.getAndIncrement() + "}";
		return postToTendermint(jsonTendermintRequest);
	}

	/**
	 * Sends a {@code genesis} request to the Tendermint process, to read the
	 * genesis information, containing for instance the chain id of the node
	 * and the initial list of validators.
	 * 
	 * @return the response of Tendermint
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing the request failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing the request
	 */
	private String genesis() throws IOException, TimeoutException, InterruptedException {
		return postToTendermint("{\"method\": \"genesis\", \"id\": " + nextId.getAndIncrement() + "}");
	}

	/**
	 * Sends a {@code status} request to the Tendermint process, to read the
	 * node status information, containing for instance the node id.
	 * 
	 * @return the response of Tendermint
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing the request failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing the request
	 */
	private String status() throws IOException, TimeoutException, InterruptedException {
		return postToTendermint("{\"method\": \"status\", \"id\": " + nextId.getAndIncrement() + "}");
	}

	/**
	 * Sends a POST request to the Tendermint process and yields the response.
	 * 
	 * @param jsonTendermintRequest the request to post, in JSON format
	 * @return the response
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing
	 */
	private String postToTendermint(String jsonTendermintRequest) throws IOException, TimeoutException, InterruptedException {
		HttpURLConnection connection = openPostConnectionToTendermint();
		writeInto(connection, jsonTendermintRequest);
		return readFrom(connection);
	}

	/**
	 * Reads the response from the given connection.
	 * 
	 * @param connection the connection
	 * @return the response
	 * @throws IOException if the response couldn't be read
	 */
	private static String readFrom(HttpURLConnection connection) throws IOException {
		try (var br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
			return br.lines().collect(Collectors.joining());
		}
	}

	/**
	 * Writes the given request into the given connection.
	 * 
	 * @param connection the connection
	 * @param jsonTendermintRequest the request
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing
	 */
	private void writeInto(HttpURLConnection connection, String jsonTendermintRequest) throws IOException, TimeoutException, InterruptedException {
		byte[] input = jsonTendermintRequest.getBytes(StandardCharsets.UTF_8);

		for (long i = 0; i < config.getMaxPingAttempts(); i++) {
			try (var os = connection.getOutputStream()) {
				os.write(input, 0, input.length);
				return;
			}
			catch (ConnectException e) {
				// not sure why this happens, randomly. It seems that the connection to the Tendermint process is flaky
				Thread.sleep(config.getPingDelay());
			}
		}

		throw new TimeoutException("Cannot write into Tendermint's connection. Tried " + config.getMaxPingAttempts() + " times");
	}
}