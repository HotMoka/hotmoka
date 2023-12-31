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

import static io.hotmoka.beans.Coin.filicudi;
import static io.hotmoka.beans.Coin.panarea;
import static io.hotmoka.beans.Coin.stromboli;
import static io.hotmoka.beans.types.internal.BasicTypes.BOOLEAN;
import static io.hotmoka.beans.types.internal.BasicTypes.SHORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.ShortValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.constants.Constants;

/**
 * A test for the ExampleCoin contract (a ERC20 contract).
 */
class ExampleCoin extends HotmokaTest {
    private static final ClassType EXAMPLECOIN = StorageTypes.classNamed("io.hotmoka.examples.tokens.ExampleCoin");
    private static final ClassType UBI = StorageTypes.UNSIGNED_BIG_INTEGER;
    private static final ConstructorSignature CONSTRUCTOR_EXAMPLECOIN = new ConstructorSignature(EXAMPLECOIN);
    private static final ConstructorSignature CONSTRUCTOR_UBI_STR = new ConstructorSignature(UBI, StorageTypes.STRING);

    /**
     * The classpath of the classes of code module.
     */
    private TransactionReference classpath_takamaka_code;

    /**
     * The creator of the coin.
     */
    private StorageReference creator;
    private PrivateKey creator_prv_key;

    /**
     * An investor.
     */
    private StorageReference investor1;
    private PrivateKey investor1_prv_key;

    /**
     * Another investor.
     */
    private StorageReference investor2;
    private PrivateKey investor2_prv_key;

    @BeforeAll
	static void beforeAll() throws Exception {
		setJar("tokens.jar");
	}

    @BeforeEach
    void beforeEach() throws Exception {
        setAccounts(stromboli(1), filicudi(100), filicudi(100), filicudi(100));
        creator = account(1);
        investor1 = account(2);
        investor2 = account(3);
        creator_prv_key = privateKey(1);
        investor1_prv_key = privateKey(2);
        investor2_prv_key = privateKey(3);
        classpath_takamaka_code = takamakaCode();
    }

    @Test @DisplayName("new ExampleCoin()")
    void createExampleCoin() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        addConstructorCallTransaction(
                creator_prv_key, // an object that signs with the payer's private key
                creator, // payer of the transaction
                _500_000, // gas provided to the transaction
                panarea(1), // gas price
                jar(), //reference to the jar being tested
                CONSTRUCTOR_EXAMPLECOIN // constructor signature
                );
    }

    @Test @DisplayName("Test of ERC20 name method: example_token.name() == 'ExampleCoin'")
    void name() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXAMPLECOIN);
        // Now creator has 200'000 EXC = 200'000 * 10 ^ 18 MiniEx

        StringValue token_name = (StringValue) runInstanceMethodCallTransaction(
                creator,
                _500_000, jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "name", StorageTypes.STRING),
                example_token);
        // token_name = example_token.name() = "ExampleCoin"

        assertEquals(token_name.value, "ExampleCoin");
    }

    @Test @DisplayName("Test of ERC20 symbol method: example_token.symbol() == 'EXC'")
    void symbol() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXAMPLECOIN);

        StringValue token_symbol = (StringValue) runInstanceMethodCallTransaction(
                creator,
                _500_000, jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "symbol", StorageTypes.STRING),
                example_token);
        // token_symbol = example_token.symbol() == "EXC"

        assertEquals(token_symbol.value, "EXC");
    }

    @Test @DisplayName("Test of ERC20 decimals method: example_token.decimals() == short@18")
    void decimals() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXAMPLECOIN);

        ShortValue token_decimals = (ShortValue) runInstanceMethodCallTransaction(
                creator,
                _500_000, jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "decimals", SHORT),
                example_token);
        // token_decimals = example_token.decimals() = short@18

        assertEquals(token_decimals.value, 18);
    }

    @Test @DisplayName("Test of ERC20 totalSupply method: example_token.totalSupply() == 200'000*10^18")
    void totalSupply() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXAMPLECOIN);
        StorageReference ubi_check = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("200000000000000000000000"));

        StorageReference supply = (StorageReference) runInstanceMethodCallTransaction(
                creator,
                _500_000, jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "totalSupply", UBI),
                example_token);
        // supply = example_token.totalSupply() == 200'000*10^18

        BooleanValue equals_result = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _500_000, classpath_takamaka_code,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT),
                supply, ubi_check);
        // equals_result = supply.equals(200'000*10^18) = true

        assertTrue(equals_result.value);
    }

    @Test @DisplayName("Test of ERC20 transfer method (and balanceOf): example_token.transfer(recipient, 5000) --> balances[caller]-=5000, balances[recipient]+=5000")
    void transfer() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXAMPLECOIN);
        StorageReference ubi_check = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("199999999999999999995000"));
        StorageReference ubi_5000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("5000"));
        StorageReference ubi_0 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("0"));

        BooleanValue transfer_result = (BooleanValue) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "transfer", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token, investor1, ubi_5000);
        // balances = [creator:199999999999999999995000, investor1:500, investor2:0]

        StorageReference creator_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _500_000, jar(), new NonVoidMethodSignature(EXAMPLECOIN, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, creator);
        // creator_balance = balances[creator] = 199999999999999999995000
        BooleanValue equals_result1 = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _500_000, classpath_takamaka_code,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT),
                creator_balance,
                ubi_check);
        // equals_result1 = creator_balance.equals(200'000*10^18-5000) = true

        StorageReference investor1_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _500_000, jar(), new NonVoidMethodSignature(EXAMPLECOIN, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, investor1);
        // investor1_balance = balances[investor1] = 5000
        BooleanValue equals_result2 = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _500_000, classpath_takamaka_code,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT),
                investor1_balance,
                ubi_5000);
        // equals_result2 = investor1_balance.equals(5000) = true

        StorageReference investor2_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _500_000, jar(), new NonVoidMethodSignature(EXAMPLECOIN, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, investor2);
        // investor2_balance = balances[investor2] = 0
        BooleanValue equals_result3 = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _500_000, classpath_takamaka_code,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT),
                investor2_balance,
                ubi_0);
        // equals_result3 = investor2_balance.equals(0) = true

        assertTrue(transfer_result.value && equals_result1.value && equals_result2.value && equals_result3.value);
    }

    @Test @DisplayName("Test of ERC20 transfer method with the generation of an Exception: example_token.transfer(recipient, 5000) when the caller has no funds ")
    void transferException() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXAMPLECOIN);
        StorageReference ubi_5000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("5000"));

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                addInstanceMethodCallTransaction(
                        investor1_prv_key, investor1,
                        _500_000, panarea(1), jar(),
                        new NonVoidMethodSignature(EXAMPLECOIN, "transfer", BOOLEAN, StorageTypes.CONTRACT, UBI),
                        example_token, investor2, ubi_5000)
                // investor1 has no funds --> Exception !!!
        );
    }

    @Test @DisplayName("Test of ERC20 approve method (and allowance): example_token.approve(spender, 4000) --> allowances[caller:[spender:4000]]")
    void approve() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXAMPLECOIN);
        StorageReference ubi_4000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("4000"));

        BooleanValue approve_result = (BooleanValue) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "approve", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_4000);
        // Now investor1 is able to spend 4000 MiniEx for creator

        StorageReference ubi_allowance = (StorageReference) runInstanceMethodCallTransaction(
                creator,
                _500_000, jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "allowance", UBI, StorageTypes.CONTRACT, StorageTypes.CONTRACT),
                example_token,
                creator, investor1);
        // ubi_allowance = allowances[creator[investor1]] = 4000

        BooleanValue equals_result = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _500_000, classpath_takamaka_code,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT),
                ubi_allowance,
                ubi_4000);
        // equals_result = ubi_allowance.equals(4000) = true

        assertTrue(approve_result.value && equals_result.value);
    }

    @Test @DisplayName("Test of ERC20 allowance method: as soon as the token is created example_token.allowance(X, Y) is always 0 for all X,Y")
    void allowance() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXAMPLECOIN);
        StorageReference ubi_0 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("0"));

        StorageReference ubi_allowance = (StorageReference) runInstanceMethodCallTransaction(creator, _500_000, jar(), new NonVoidMethodSignature(EXAMPLECOIN, "allowance", UBI, StorageTypes.CONTRACT, StorageTypes.CONTRACT), example_token, creator, investor1);
        // ubi_allowance = allowances[creator[investor1]] = 0
        BooleanValue equals_result = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _500_000, classpath_takamaka_code,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT),
                ubi_allowance,
                ubi_0);
        // equals_result = ubi_allowance.equals(0) = true

        StorageReference ubi_allowance2 = (StorageReference) runInstanceMethodCallTransaction(investor2, _500_000, jar(), new NonVoidMethodSignature(EXAMPLECOIN, "allowance", UBI, StorageTypes.CONTRACT, StorageTypes.CONTRACT), example_token, investor2, investor1);
        // ubi_allowance = allowances[investor1[investor2]] = 0
        BooleanValue equals_result2 = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _500_000, classpath_takamaka_code,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT),
                ubi_allowance2,
                ubi_0);
        // equals_result2 = ubi_allowance2.equals(0) = true

        assertTrue(equals_result.value && equals_result2.value);
    }

    @Test @DisplayName("Test of ERC20 transferFrom method: example_token.transferFrom(sender, recipient, 7000) --> balances[sender]-=7000, balances[recipient]+=7000")
    void transferFrom() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXAMPLECOIN);
        StorageReference ubi_check = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("199999999999999999996000"));
        StorageReference ubi_7000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("7000"));
        StorageReference ubi_4000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("4000"));
        StorageReference ubi_3000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("3000"));
        StorageReference ubi_0 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("0"));

        BooleanValue approve_result = (BooleanValue) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "approve", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_7000);
        // Now investor1 is able to spend 7000 MiniEx for creator

        BooleanValue transfer_from_result = (BooleanValue) addInstanceMethodCallTransaction(
                investor1_prv_key, investor1,
                _500_000, panarea(1), jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "transferFrom", BOOLEAN, StorageTypes.CONTRACT, StorageTypes.CONTRACT, UBI),
                example_token,
                creator, investor1, ubi_4000);
        // investor1 can spend on creator's behalf --> balances = [creator:..., investor1:4000, investor2:0]

        StorageReference creator_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _500_000, jar(), new NonVoidMethodSignature(EXAMPLECOIN, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, creator);
        // creator_balance = balances[creator] = 199999999999999999996000
        BooleanValue equals_result1 = (BooleanValue) runInstanceMethodCallTransaction(creator, _500_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), creator_balance, ubi_check);
        // equals_result1 = creator_balance.equals(200'000*10^18-4000) = true

        StorageReference investor1_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _500_000, jar(), new NonVoidMethodSignature(EXAMPLECOIN, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, investor1);
        // investor1_balance = balances[investor1] = 4000
        BooleanValue equals_result2 = (BooleanValue) runInstanceMethodCallTransaction(creator, _500_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), investor1_balance, ubi_4000);
        // equals_result2 = investor1_balance.equals(4000) = true

        StorageReference investor2_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _500_000, jar(), new NonVoidMethodSignature(EXAMPLECOIN, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, investor2);
        // investor2_balance = balances[investor2] = 0
        BooleanValue equals_result3 = (BooleanValue) runInstanceMethodCallTransaction(creator, _500_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), investor2_balance, ubi_0);
        // equals_result3 = investor2_balance.equals(0) = true

        StorageReference ubi_remaining_allowance = (StorageReference) runInstanceMethodCallTransaction(creator, _500_000, jar(), new NonVoidMethodSignature(EXAMPLECOIN, "allowance", UBI, StorageTypes.CONTRACT, StorageTypes.CONTRACT), example_token, creator, investor1);
        // ubi_remaining_allowance = allowances[creator[investor1]] = 7000 - 4000 (just spent) = 3000
        BooleanValue equals_result4 = (BooleanValue) runInstanceMethodCallTransaction(creator, _500_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), ubi_remaining_allowance, ubi_3000);
        // equals_result4 = ubi_remaining_allowance.equals(3000) = true

        assertTrue(approve_result.value && transfer_from_result.value);
        assertTrue(equals_result1.value && equals_result2.value && equals_result3.value && equals_result4.value);
    }

    @Test @DisplayName("Test of ERC20 transferFrom method with the generation of some Exceptions")
    void transferFromExceptions() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXAMPLECOIN);
        StorageReference ubi_check = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("200000000000000000000000"));
        StorageReference ubi_7000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("7000"));
        StorageReference ubi_8000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("8000"));
        StorageReference ubi_4000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("4000"));
        StorageReference ubi_0 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("0"));

        BooleanValue approve_result = (BooleanValue) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "approve", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_7000);
        // Now investor1 is able to spend 7000 MiniEx for creator

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                addInstanceMethodCallTransaction(
                        investor2_prv_key, investor2,
                        _500_000, panarea(1), jar(),
                        new NonVoidMethodSignature(EXAMPLECOIN, "transferFrom", BOOLEAN, StorageTypes.CONTRACT, StorageTypes.CONTRACT, UBI),
                        example_token,
                        creator, investor1, ubi_4000)
                // investor2 cannot spend on creator's behalf --> Exception !!!
        );

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                addInstanceMethodCallTransaction(
                        investor1_prv_key, investor1,
                        _500_000, panarea(1), jar(),
                        new NonVoidMethodSignature(EXAMPLECOIN, "transferFrom", BOOLEAN, StorageTypes.CONTRACT, StorageTypes.CONTRACT, UBI),
                        example_token,
                        creator, investor1, ubi_8000)
                // investor1 can spend on creator's behalf, but only 7000 token --> Exception !!!
        );

        StorageReference creator_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _500_000, jar(), new NonVoidMethodSignature(EXAMPLECOIN, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, creator);
        // creator_balance = balances[creator] = 200000000000000000000000
        BooleanValue equals_result1 = (BooleanValue) runInstanceMethodCallTransaction(creator, _500_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), creator_balance, ubi_check);
        // equals_result1 = creator_balance.equals(200'000*10^18) = true

        StorageReference investor1_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _500_000, jar(), new NonVoidMethodSignature(EXAMPLECOIN, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, investor1);
        // investor1_balance = balances[investor1] = 0
        BooleanValue equals_result2 = (BooleanValue) runInstanceMethodCallTransaction(creator, _500_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), investor1_balance, ubi_0);
        // equals_result2 = investor1_balance.equals(0) = true

        StorageReference investor2_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _500_000, jar(), new NonVoidMethodSignature(EXAMPLECOIN, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, investor2);
        // investor2_balance = balances[investor2] = 0
        BooleanValue equals_result3 = (BooleanValue) runInstanceMethodCallTransaction(creator, _500_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), investor2_balance, ubi_0);
        // equals_result3 = investor2_balance.equals(0) = true

        assertTrue(approve_result.value);
        assertTrue(equals_result1.value && equals_result2.value && equals_result3.value);
    }

    @Test @DisplayName("iToken Duplication Incident Test (based on transferFrom)")
    void iToken() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXAMPLECOIN);
        // Now creator has 200'000 EXC = 200'000 * 10 ^ 18 MiniEx

        StorageReference ubi_1000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("1000"));
        StorageReference ubi_900 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("900"));

        BooleanValue transfer_result = (BooleanValue) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "transfer", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_1000);
        // balances = [creator:..., investor1:1000, investor2:0]

        BooleanValue approve_result = (BooleanValue) addInstanceMethodCallTransaction(
                investor1_prv_key, investor1,
                _500_000, panarea(1), jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "approve", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_1000);
        // Now investor1 is able to spend his tokens through the transferFrom function

        BooleanValue transfer_from_result = (BooleanValue) addInstanceMethodCallTransaction(
                investor1_prv_key, investor1,
                _500_000, panarea(1), jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "transferFrom", BOOLEAN, StorageTypes.CONTRACT, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, investor1, ubi_900);
        // balances = [creator:..., investor1:1000, investor2:0]

        StorageReference investor1_tokens_ubi = (StorageReference) runInstanceMethodCallTransaction(
                creator,
                _500_000, jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "balanceOf", UBI, StorageTypes.CONTRACT),
                example_token,
                investor1);
        // investor1_tokens_ubi = 1000

        StringValue investor1_tokens_string = (StringValue) runInstanceMethodCallTransaction(
                creator,
                _500_000, jar(),
                new NonVoidMethodSignature(UBI, "toString", StorageTypes.STRING),
                investor1_tokens_ubi);
        // investor1_tokens_string = "1000"

        assertTrue(transfer_result.value && approve_result.value && transfer_from_result.value);
        assertEquals(investor1_tokens_string.value, "1000");
    }

    @Test @DisplayName("Test of ERC20 increaseAllowance method: example_token.increaseAllowance(spender, 999) --> allowances[caller:[spender:+=999]]")
    void increaseAllowance() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXAMPLECOIN);
        StorageReference ubi_4000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("4000"));
        StorageReference ubi_999 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("999"));
        StorageReference ubi_4999 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("4999"));

        BooleanValue approve_result = (BooleanValue) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "approve", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_4000);
        // Now investor1 is able to spend 4000 MiniEx for creator

        BooleanValue increase_result = (BooleanValue) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "increaseAllowance", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_999);
        // Now investor1 is able to spend 4000 + 999 = 4999 MiniEx for creator

        StorageReference ubi_allowance = (StorageReference) runInstanceMethodCallTransaction(
                creator,
                _500_000, jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "allowance", UBI, StorageTypes.CONTRACT, StorageTypes.CONTRACT),
                example_token,
                creator, investor1);
        // ubi_allowance = allowances[creator[investor1]] = 4999

        BooleanValue equals_result = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _500_000, classpath_takamaka_code,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT),
                ubi_allowance,
                ubi_4999);
        // equals_result = ubi_allowance.equals(4999) = true

        assertTrue(approve_result.value && increase_result.value && equals_result.value);
    }

    @Test @DisplayName("Test of ERC20 decreaseAllowance method: example_token.decreaseAllowance(spender, 999) --> allowances[caller:[spender:-=999]]")
    void decreaseAllowance() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXAMPLECOIN);
        StorageReference ubi_4000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("4000"));
        StorageReference ubi_999 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("999"));
        StorageReference ubi_3001 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("3001"));

        BooleanValue approve_result = (BooleanValue) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "approve", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_4000);
        // Now investor1 is able to spend 4000 MiniEx for creator

        BooleanValue decrease_result = (BooleanValue) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "decreaseAllowance", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_999);
        // Now investor1 is able to spend 4000 - 999 = 3001 MiniEx for creator

        StorageReference ubi_allowance = (StorageReference) runInstanceMethodCallTransaction(
                creator,
                _500_000, jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "allowance", UBI, StorageTypes.CONTRACT, StorageTypes.CONTRACT),
                example_token,
                creator, investor1);
        // ubi_allowance = allowances[creator[investor1]] = 3001

        BooleanValue equals_result = (BooleanValue) runInstanceMethodCallTransaction(
                creator,
                _500_000, classpath_takamaka_code,
                new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT),
                ubi_allowance,
                ubi_3001);
        // equals_result = ubi_allowance.equals(3001) = true

        assertTrue(approve_result.value && decrease_result.value && equals_result.value);
    }

    @Test @DisplayName("Test of ERC20 decreaseAllowance method with the generation of an Exception: example_token.decreaseAllowance(spender, 999) --> Exception!! allowances[caller:[spender]] < 999")
    void decreaseAllowanceException() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXAMPLECOIN);
        StorageReference ubi_998 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("998"));
        StorageReference ubi_999 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("999"));

        BooleanValue approve_result = (BooleanValue) addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "approve", BOOLEAN, StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_998);
        // Now investor1 is able to spend 998 MiniEx for creator

        throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
                addInstanceMethodCallTransaction(
                        creator_prv_key, creator,
                        _500_000, panarea(1), jar(),
                        new NonVoidMethodSignature(EXAMPLECOIN, "decreaseAllowance", BOOLEAN, StorageTypes.CONTRACT, UBI),
                        example_token,
                        investor1, ubi_999)
                // allowances[caller:[spender]] < 999 --> Exception !!!
        );

        assertTrue(approve_result.value);
    }

    @Test
    @DisplayName("Test of ERC20 _mint method: example_token.mint(account, 500'000) --> totalSupply+=500'000, balances[account]+=500'000")
    void mint() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXAMPLECOIN);
        StorageReference ubi_check = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("200000000000000000500000"));
        StorageReference ubi_check2 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("200000000000000001000000"));
        StorageReference ubi_500000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("500000"));

        addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                new VoidMethodSignature(EXAMPLECOIN, "mint", StorageTypes.CONTRACT, UBI),
                example_token,
                creator, ubi_500000);
        // balances = [creator:200000000000000000500000], totalSupply:200000000000000000500000

        addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                new VoidMethodSignature(EXAMPLECOIN, "mint", StorageTypes.CONTRACT, UBI),
                example_token,
                investor1, ubi_500000);
        // balances = [creator:200000000000000000500000, investor1:500000], totalSupply:200000000000000001000000

        StorageReference creator_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _500_000, jar(), new NonVoidMethodSignature(EXAMPLECOIN, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, creator);
        // creator_balance = balances[creator] = 200000000000000000500000
        BooleanValue equals_result1 = (BooleanValue) runInstanceMethodCallTransaction(creator, _500_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), creator_balance, ubi_check);
        // equals_result1 = creator_balance.equals(200'000*10^18 + 500000) = true

        StorageReference investor1_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _500_000, jar(), new NonVoidMethodSignature(EXAMPLECOIN, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, investor1);
        // investor1_balance = balances[investor1] = 500000
        BooleanValue equals_result2 = (BooleanValue) runInstanceMethodCallTransaction(creator, _500_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), investor1_balance, ubi_500000);
        // equals_result2 = investor1_balance.equals(500000) = true

        StorageReference supply = (StorageReference) runInstanceMethodCallTransaction(
                creator,
                _500_000, jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "totalSupply", UBI),
                example_token);
        // supply = example_token.totalSupply() == 200'000*10^18 + 500000 + 500000

        BooleanValue equals_result3 = (BooleanValue) runInstanceMethodCallTransaction(creator, _500_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), supply, ubi_check2);
        // equals_result3 = supply.equals(200'000*10^18 + 500000 + 500000) = true

        assertTrue(equals_result1.value && equals_result2.value && equals_result3.value);
    }

    @Test
    @DisplayName("Test of ERC20 _burn method: example_token.burn(account, 500'000) --> totalSupply-=500'000, balances[account]-=500'000")
    void burn() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_EXAMPLECOIN);
        StorageReference ubi_check = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("199999999999999999500000"));
        StorageReference ubi_500000 = addConstructorCallTransaction(creator_prv_key, creator, _500_000, panarea(1), classpath_takamaka_code, CONSTRUCTOR_UBI_STR, new StringValue("500000"));

        addInstanceMethodCallTransaction(
                creator_prv_key, creator,
                _500_000, panarea(1), jar(),
                new VoidMethodSignature(EXAMPLECOIN, "burn", StorageTypes.CONTRACT, UBI),
                example_token,
                creator, ubi_500000);
        // balances = [creator:199999999999999999500000], totalSupply:199999999999999999500000

        StorageReference creator_balance = (StorageReference) runInstanceMethodCallTransaction(creator, _500_000, jar(), new NonVoidMethodSignature(EXAMPLECOIN, "balanceOf", UBI, StorageTypes.CONTRACT), example_token, creator);
        // creator_balance = balances[creator] = 199999999999999999500000
        BooleanValue equals_result1 = (BooleanValue) runInstanceMethodCallTransaction(creator, _500_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), creator_balance, ubi_check);
        // equals_result1 = creator_balance.equals(200'000*10^18 - 500000) = true

        StorageReference supply = (StorageReference) runInstanceMethodCallTransaction(
                creator,
                _500_000, jar(),
                new NonVoidMethodSignature(EXAMPLECOIN, "totalSupply", UBI),
                example_token);
        // supply = example_token.totalSupply() == 200'000*10^18 - 500000

        BooleanValue equals_result2 = (BooleanValue) runInstanceMethodCallTransaction(creator, _500_000, classpath_takamaka_code, new NonVoidMethodSignature(UBI, "equals", BOOLEAN, StorageTypes.OBJECT), supply, ubi_check);
        // equals_result2 = supply.equals(200'000*10^18 - 500000) = true

        assertTrue(equals_result1.value && equals_result2.value);
    }
}