/**
 * 
 */
package io.takamaka.code.tests;

import static io.hotmoka.beans.types.BasicTypes.BOOLEAN;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.StorageReference;

/**
 * A test for the simplified crowd funding contract.
 */
class CrowdFundingSimplified extends TakamakaTest {
	private static final ClassType CAMPAIGN = new ClassType("io.takamaka.tests.crowdfunding.CrowdFundingSimplified$Campaign");
	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);
	private static final ClassType CROWD_FUNDING_SIMPLIFIED = new ClassType("io.takamaka.tests.crowdfunding.CrowdFundingSimplified");
	private static final ConstructorSignature CONSTRUCTOR_CROWD_FUNDING_SIMPLIFIED = new ConstructorSignature("io.takamaka.tests.crowdfunding.CrowdFundingSimplified");
	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000_000_000L);

	/**
	 * The first object, that holds all funds initially.
	 */
	private StorageReference account0;

	/**
	 * TYhe beneficiary of the crowd funding.
	 */
	private StorageReference beneficiary;

	/**
	 * A first funder.
	 */
	private StorageReference funder1;

	/**
	 * A second funder.
	 */
	private StorageReference funder2;

	/**
	 * The crowd funding contract.
	 */
	private StorageReference crowdFunding;

	@BeforeEach
	void beforeEach() throws Exception {
		setNode("crowdfunding.jar", ALL_FUNDS, BigInteger.ZERO, BigInteger.valueOf(10_000_000L), BigInteger.valueOf(10_000_000L));
		account0 = account(0);
		beneficiary = account(1);
		funder1 = account(2);
		funder2 = account(3);
		crowdFunding = addConstructorCallTransaction(privateKey(0), account0, _10_000, BigInteger.ONE, jar(), CONSTRUCTOR_CROWD_FUNDING_SIMPLIFIED);
	}

	@Test @DisplayName("new CrowdFundingSimplified().newCampaign(beneficiary, 50) != null")
	void createCampaign() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference campaign = (StorageReference) addInstanceMethodCallTransaction
			(privateKey(0), account0, _10_000, BigInteger.ONE, jar(),
			new NonVoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "newCampaign", CAMPAIGN, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER),
			crowdFunding, beneficiary, new BigIntegerValue(BigInteger.valueOf(50L)));

		assertNotNull(campaign);
	}

	@Test @DisplayName("contributions are not enough then checkGoalReached yields false")
	void contributionsAreNotEnough() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference campaign = (StorageReference) addInstanceMethodCallTransaction
			(privateKey(0), account0, _10_000, BigInteger.ONE, jar(),
			new NonVoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "newCampaign", CAMPAIGN, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER),
			crowdFunding, beneficiary, new BigIntegerValue(BigInteger.valueOf(50L)));

		postInstanceMethodCallTransaction
			(privateKey(2), funder1, _10_000, BigInteger.ONE, jar(),
			new VoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "contribute", ClassType.BIG_INTEGER, CAMPAIGN),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(48L)), campaign);

		postInstanceMethodCallTransaction
			(privateKey(3), funder2, _10_000, BigInteger.ONE, jar(),
			new VoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "contribute", ClassType.BIG_INTEGER, CAMPAIGN),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(1L)), campaign);

		BooleanValue reached = (BooleanValue) addInstanceMethodCallTransaction
			(privateKey(0), account0, _10_000, BigInteger.ONE, jar(),
			new NonVoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "checkGoalReached", BOOLEAN, CAMPAIGN),
			crowdFunding, campaign);

		assertFalse(reached.value);
	}

	@Test @DisplayName("contributions are enough then checkGoalReached yields false")
	void contributionsAreEnough() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference campaign = (StorageReference) addInstanceMethodCallTransaction
			(privateKey(0), account0, _10_000, BigInteger.ONE, jar(),
			new NonVoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "newCampaign", CAMPAIGN, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER),
			crowdFunding, beneficiary, new BigIntegerValue(BigInteger.valueOf(50L)));

		postInstanceMethodCallTransaction
			(privateKey(2), funder1, _10_000, BigInteger.ONE, jar(),
			new VoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "contribute", ClassType.BIG_INTEGER, CAMPAIGN),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(48L)), campaign);

		postInstanceMethodCallTransaction
			(privateKey(3), funder2, _10_000, BigInteger.ONE, jar(),
			new VoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "contribute", ClassType.BIG_INTEGER, CAMPAIGN),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(2L)), campaign);

		BooleanValue reached = (BooleanValue) addInstanceMethodCallTransaction
			(privateKey(0), account0, _10_000, BigInteger.ONE, jar(),
			new NonVoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "checkGoalReached", BOOLEAN, CAMPAIGN),
			crowdFunding, campaign);

		assertTrue(reached.value);
	}
}