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

package io.hotmoka.xodus.env;

import java.util.function.Consumer;
import java.util.function.Function;

import io.hotmoka.xodus.ExodusException;
import jetbrains.exodus.env.StoreConfig;

public class Environment {
	private final jetbrains.exodus.env.Environment parent;
	
	public Environment(String dir) {
		this.parent = jetbrains.exodus.env.Environments.newInstance(dir);
	}

	public Environment(String dir, EnvironmentConfig config) {
		this.parent = jetbrains.exodus.env.Environments.newInstance(dir, config.toNative());
	}

	public void close() throws ExodusException {
		try {
			parent.close();
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}

	public Transaction beginTransaction() throws ExodusException {
		try {
			return new Transaction(parent.beginTransaction());
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}

	public void executeInTransaction(Consumer<Transaction> executable) throws ExodusException {
		try {
			parent.executeInTransaction(txn -> executable.accept(new Transaction(txn)));
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}

	public void executeInReadonlyTransaction(Consumer<Transaction> executable) throws ExodusException {
		try {
			parent.executeInReadonlyTransaction(txn -> executable.accept(new Transaction(txn)));
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}

	public <T> T computeInReadonlyTransaction(Function<Transaction, T> computable) throws ExodusException {
		try {
			return parent.computeInReadonlyTransaction(txn -> computable.apply(new Transaction(txn)));
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}

	public <T> T computeInTransaction(Function<Transaction, T> computable) throws ExodusException {
		try {
			return parent.computeInTransaction(txn -> computable.apply(new Transaction(txn)));
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}

	public Store openStoreWithoutDuplicates(String name, Transaction txn) throws ExodusException {
		try {
			return new Store(parent.openStore(name, StoreConfig.WITHOUT_DUPLICATES, txn.toNative()));
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}
}