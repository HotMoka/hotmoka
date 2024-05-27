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

package io.hotmoka.node.local;

import java.util.Optional;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.local.api.CheckableStore;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.internal.AbstractCheckableLocalNodeImpl;

/**
 * Partial implementation of a local (ie., non-remote) node.
 * 
 * @param <C> the type of the configuration object used by the node
 * @param <S> the type of the store of the node
 * @param <T> the type of the store transformations that can be started from this store
 */
@ThreadSafe
public abstract class AbstractCheckableLocalNode<C extends LocalNodeConfig<C,?>, S extends AbstractStore<S, T> & CheckableStore<S, T>, T extends AbstractStoreTranformation<S, T>> extends AbstractCheckableLocalNodeImpl<C, S, T> {

	/**
	 * Creates a new node.
	 * 
	 * @param consensus the consensus configuration of the node; if missing, this will be extracted
	 *                  from the saved state of the node
	 * @param config the configuration of the node
	 * @throws NodeException if the operation cannot be completed correctly
	 */
	protected AbstractCheckableLocalNode(Optional<ConsensusConfig<?,?>> consensus, C config) throws NodeException {
		super(consensus, config);
	}
}