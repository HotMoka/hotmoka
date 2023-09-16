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

package io.hotmoka.tendermint;

import java.nio.file.Path;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.local.Config;

/**
 * The configuration of a Tendermint blockchain.
 */
@Immutable
public class TendermintBlockchainConfig extends Config {

	/**
	 * The directory that contains the Tendermint configuration that must be cloned
	 * if a brand new Tendermint blockchain is created.
	 * That configuration will then be used for the execution of Tendermint.
	 * This might be {@code null}, in which case a default Tendermint configuration is created,
	 * with the same node as single validator. It defaults to {@code null}.
	 */
	public final Path tendermintConfigurationToClone;

	/**
	 * The maximal number of connection attempts to the Tendermint process during ping.
	 * Defaults to 20.
	 */
	public final int maxPingAttempts;

	/**
	 * The delay between two successive ping attempts, in milliseconds. Defaults to 200.
	 */
	public final int pingDelay;

	/**
	 * Full constructor for the builder pattern.
	 */
	protected TendermintBlockchainConfig(Builder builder) {
		super(builder);

		this.tendermintConfigurationToClone = builder.tendermintConfigurationToClone;
		this.maxPingAttempts = builder.maxPingAttempts;
		this.pingDelay = builder.pingDelay;
	}

	/**
	 * The builder of a configuration object.
	 */
	public static class Builder extends io.hotmoka.node.local.Config.Builder<Builder> {
		private int maxPingAttempts = 20;
		private int pingDelay = 200;
		private Path tendermintConfigurationToClone;

		/**
		 * Sets the directory that contains the Tendermint configuration that must be cloned
		 * if a brand new Tendermint blockchain is created.
		 * That configuration will then be used for the execution of Tendermint.
		 * This might be {@code null}, in which case a default Tendermint configuration is created,
		 * with the same node as single validator. It defaults to {@code null}.
		 * 
		 * @param tendermintConfigurationToClone the directory of the Tendermint configuration
		 *                                       to clone and use for Tendermint
		 * @return this builder
		 */
		public Builder setTendermintConfigurationToClone(Path tendermintConfigurationToClone) {
			this.tendermintConfigurationToClone = tendermintConfigurationToClone;
			return this;
		}

		/**
		 * Sets the maximal number of connection attempts to the Tendermint process during ping.
		 * Defaults to 20.
		 * 
		 * @param maxPingAttempts the max number of attempts
		 * @return this builder
		 */
		public Builder setMaxPingAttempts(int maxPingAttempts) {
			this.maxPingAttempts = maxPingAttempts;
			return this;
		}

		/**
		 * Sets the delay between two successive ping attempts, in milliseconds. Defaults to 200.
		 * 
		 * @param pingDelay the delay
		 * @return this builder
		 */
		public Builder setPingDelay(int pingDelay) {
			this.pingDelay = pingDelay;
			return this;
		}

		@Override
		public TendermintBlockchainConfig build() {
			return new TendermintBlockchainConfig(this);
		}

		@Override
		protected Builder getThis() {
			return this;
		}
	}
}