/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.closeables.internal;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.closeables.CloseHandlersManagers;
import io.hotmoka.closeables.api.CloseHandler;
import io.hotmoka.closeables.api.CloseHandlersContainer;
import io.hotmoka.closeables.api.CloseHandlersManager;

/**
 * Partial implementation of an autocloseable with support close handlers, that must be called when the
 * object gets closed.
 */
@ThreadSafe
public abstract class AutoCloseableWithCloseHandlersImpl implements CloseHandlersContainer, AutoCloseable {

	/**
	 * The close handlers manager.
	 */
	private final CloseHandlersManager manager = CloseHandlersManagers.create();

	/**
	 * Creates the autocloseable.
	 */
	protected AutoCloseableWithCloseHandlersImpl() {}

	@Override
	public void addCloseHandler(CloseHandler handler) {
		manager.addCloseHandler(handler);
	}

	@Override
	public void removeCloseHandler(CloseHandler handler) {
		manager.removeCloseHandler(handler);
	}

	/**
	 * Calls all close handlers added to this object. If any of them fails with an exception,
	 * it tries to close the others before giving up and throwing the exception.
	 * 
	 * @throws InterruptedException if the closure operation gets interrupted
	 * @throws Exception if some handler throws this exception
	 */
	protected void callCloseHandlers() throws InterruptedException, Exception {
		manager.close();
	}
}