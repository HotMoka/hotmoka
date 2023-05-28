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

import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;

/**
 * A test for generating many coin transfers and count their speed.
 */
class Bombing extends HotmokaTest {
	private final static int NUMBER_OF_TRANSFERS = 1000;
	private static int NUMBER_OF_ACCOUNTS = 500;

	@BeforeAll
	static void beforeAll() {
		String cheapTests = System.getProperty("cheapTests");
		if ("true".equals(cheapTests)) {
			System.out.println("Running in cheap mode since cheapTests = true");
			NUMBER_OF_ACCOUNTS = 4;
		}
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(Stream.generate(() -> _50_000).limit(NUMBER_OF_ACCOUNTS)); // NUMBER_OF_ACCOUNTS accounts
	}

	private final AtomicInteger ticket = new AtomicInteger();

	private void run(int num) {
		StorageReference from = account(num);
		PrivateKey key = privateKey(num);
		Random random = new Random();

		while (ticket.getAndIncrement() < NUMBER_OF_TRANSFERS) {
			StorageReference to = random.ints(0, NUMBER_OF_ACCOUNTS).filter(i -> i != num).mapToObj(this::account).findAny().get();
			int amount = 1 + random.nextInt(10);

			try {
				addInstanceMethodCallTransaction(key, from, _50_000, ZERO, takamakaCode(), CodeSignature.RECEIVE_INT, to, new IntValue(amount));
			}
			catch (InvalidKeyException | SignatureException | TransactionException | CodeExecutionException | TransactionRejectedException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	@DisplayName(NUMBER_OF_TRANSFERS + " random transfers between accounts")
	void randomTransfers() throws InterruptedException, TransactionException, CodeExecutionException, TransactionRejectedException, ExecutionException {
		long start = System.currentTimeMillis();
		try (var customThreadPool = new ForkJoinPool(NUMBER_OF_ACCOUNTS)) {
			customThreadPool.submit(() -> IntStream.range(0, NUMBER_OF_ACCOUNTS).parallel().forEach(this::run)).get();
		}
		long time = System.currentTimeMillis() - start;
		System.out.printf("%d money transfer transactions in %d ms [%d tx/s]\n", NUMBER_OF_TRANSFERS, time, NUMBER_OF_TRANSFERS * 1000L / time);

		// we compute the sum of the balances of the accounts
		BigInteger sum = ZERO;
		for (int i = 0; i < NUMBER_OF_ACCOUNTS; i++)
			sum = sum.add(((BigIntegerValue) runInstanceMethodCallTransaction(account(0), _50_000, takamakaCode(), CodeSignature.BALANCE, account(i))).value);

		// no money got lost in translation
		assertEquals(sum, BigInteger.valueOf(NUMBER_OF_ACCOUNTS).multiply(_50_000));
	}
}