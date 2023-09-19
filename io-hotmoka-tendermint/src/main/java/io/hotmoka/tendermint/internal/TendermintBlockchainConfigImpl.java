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

package io.hotmoka.tendermint.internal;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.moandjiezana.toml.Toml;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.local.AbstractLocalNodeConfig;
import io.hotmoka.tendermint.TendermintBlockchainConfig;
import io.hotmoka.tendermint.TendermintBlockchainConfigBuilder;

/**
 * Implementation of the configuration of a Tendermint blockchain.
 */
@Immutable
public class TendermintBlockchainConfigImpl extends AbstractLocalNodeConfig implements TendermintBlockchainConfig {

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
	 * Creates a new configuration object from its builder.
	 * 
	 * @param the builder
	 */
	protected TendermintBlockchainConfigImpl(TendermintBlockchainConfigBuilderImpl builder) {
		super(builder);

		this.tendermintConfigurationToClone = builder.tendermintConfigurationToClone;
		this.maxPingAttempts = builder.maxPingAttempts;
		this.pingDelay = builder.pingDelay;
	}

	@Override
	public Path getTendermintConfigurationToClone() {
		return tendermintConfigurationToClone;
	}

	@Override
	public int getMaxPingAttempts() {
		return maxPingAttempts;
	}

	@Override
	public int getPingDelay() {
		return pingDelay;
	}

	@Override
	public String toToml() {
		var sb = new StringBuilder(super.toToml());

		sb.append("\n");
		sb.append("# the directory that contains the Tendermint configuration that must be cloned\n");
		sb.append("# if a brand new Tendermint blockchain is created. If this is missing,\n");
		sb.append("# a default Tendermint configuration will be created, with the node as single validator\n");
		sb.append("tendermint_configuration_to_clone = \"" + tendermintConfigurationToClone + "\"\n");
		sb.append("\n");
		sb.append("# the maximal number of connection attempts to the Tendermint process during ping\n");
		sb.append("max_ping_attempts = " + maxPingAttempts + "\n");
		sb.append("\n");
		sb.append("# the delay between two successive ping attempts, in milliseconds\n");
		sb.append("ping_delay = " + pingDelay + "\n");

		return sb.toString();
	}

	@Override
	public TendermintBlockchainConfigBuilder toBuilder() {
		return new TendermintBlockchainConfigBuilderImpl(this);
	}

	/**
	 * The builder of a configuration object.
	 */
	public static class TendermintBlockchainConfigBuilderImpl extends AbstractLocalNodeConfigBuilder<TendermintBlockchainConfigBuilder> implements TendermintBlockchainConfigBuilder {
		private int maxPingAttempts = 20;
		private int pingDelay = 200;
		private Path tendermintConfigurationToClone;

		/**
		 * Creates a builder with default values for the properties.
		 */
		public TendermintBlockchainConfigBuilderImpl() {}

		/**
		 * Creates a builder by reading the properties of the given TOML file and sets them for
		 * the corresponding fields of this builder.
		 * 
		 * @param toml the file
		 */
		protected TendermintBlockchainConfigBuilderImpl(Toml toml) {
			super(toml);

			// TODO: remove these type conversions
			var maxPingAttempts = toml.getLong("max_ping_attempts");
			if (maxPingAttempts != null)
				setMaxPingAttempts((int) (long) maxPingAttempts);

			var pingDelay = toml.getLong("ping_delay");
			if (pingDelay != null)
				setPingDelay((int) (long) pingDelay);

			var tendermintConfigurationToClone = toml.getString("tendermint_configuration_to_clone");
			if (tendermintConfigurationToClone != null)
				setTendermintConfigurationToClone(Paths.get(tendermintConfigurationToClone));
		}

		/**
		 * Creates a builder by reading the properties of the given TOML file and sets them for
		 * the corresponding fields of this builder.
		 * 
		 * @param toml the file
		 * @throws FileNotFoundException if the file cannot be found
		 */
		public TendermintBlockchainConfigBuilderImpl(Path toml) throws FileNotFoundException {
			super(readToml(toml));
		}

		/**
		 * Creates a builder with properties initialized to those of the given configuration object.
		 * 
		 * @param config the configuration object
		 */
		protected TendermintBlockchainConfigBuilderImpl(TendermintBlockchainConfig config) {
			super(config);

			setMaxPingAttempts(config.getMaxPingAttempts());
			setPingDelay(config.getPingDelay());
			setTendermintConfigurationToClone(config.getTendermintConfigurationToClone());
		}

		@Override
		public TendermintBlockchainConfigBuilder setTendermintConfigurationToClone(Path tendermintConfigurationToClone) {
			this.tendermintConfigurationToClone = tendermintConfigurationToClone;
			return getThis();
		}

		@Override
		public TendermintBlockchainConfigBuilder setMaxPingAttempts(int maxPingAttempts) {
			this.maxPingAttempts = maxPingAttempts;
			return getThis();
		}

		@Override
		public TendermintBlockchainConfigBuilder setPingDelay(int pingDelay) {
			this.pingDelay = pingDelay;
			return getThis();
		}

		@Override
		public TendermintBlockchainConfig build() {
			return new TendermintBlockchainConfigImpl(this);
		}

		@Override
		protected TendermintBlockchainConfigBuilder getThis() {
			return this;
		}
	}
}