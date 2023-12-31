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

package io.hotmoka.tests;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.node.NonWhiteListedCallException;

/**
 * A test for the call to an interface method actually inherited from Object.
 */
class InterfaceOverridesObject1 extends HotmokaTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("interfaceoverridesobject1.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000);
	}

	@Test @DisplayName("C.test()")
	void createC() {
		throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () ->
			addStaticMethodCallTransaction(privateKey(0), account(0), _1_000_000, BigInteger.ONE, jar(),
				new NonVoidMethodSignature(StorageTypes.classNamed("io.hotmoka.examples.interfaceoverridesobject1.C"), "test", StorageTypes.INT)));
	}
}