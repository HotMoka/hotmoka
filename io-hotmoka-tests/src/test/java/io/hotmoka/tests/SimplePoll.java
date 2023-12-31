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

import static io.hotmoka.beans.StorageTypes.BIG_INTEGER;
import static io.hotmoka.beans.StorageTypes.PAYABLE_CONTRACT;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.StorageReference;

class SimplePoll extends HotmokaTest {

	private static final ClassType SIMPLE_SHARED_ENTITY = StorageTypes.classNamed("io.takamaka.code.dao.SimpleSharedEntity");
	private static final ClassType SIMPLE_POLL = StorageTypes.classNamed("io.takamaka.code.dao.SimplePoll");
	private static final ClassType ACTION_SIMPLE_POLL = StorageTypes.classNamed("io.takamaka.code.dao.SimplePoll$Action");
	private static final ClassType ACTION = StorageTypes.classNamed("io.hotmoka.examples.polls.CheckRunPerformedAction");

	private static final ConstructorSignature SIMPLE_SHARED_ENTITY_CONSTRUCTOR = new ConstructorSignature(SIMPLE_SHARED_ENTITY, 
			PAYABLE_CONTRACT, PAYABLE_CONTRACT, PAYABLE_CONTRACT, PAYABLE_CONTRACT, BIG_INTEGER, BIG_INTEGER, BIG_INTEGER, BIG_INTEGER);

	private static final ConstructorSignature SIMPLE_POLL_ENTITY_CONSTRUCTOR = new ConstructorSignature(SIMPLE_POLL, StorageTypes.SHARED_ENTITY_VIEW, ACTION_SIMPLE_POLL);
	private static final ConstructorSignature ACTION_CONSTRUCTOR = new ConstructorSignature(ACTION);

	private static final VoidMethodSignature VOTE_POLL = new VoidMethodSignature(SIMPLE_POLL, "vote");
	private static final VoidMethodSignature VOTE_POLL_WITH_PARAM = new VoidMethodSignature(SIMPLE_POLL, "vote", BIG_INTEGER);
	private static final VoidMethodSignature CLOSE_POLL = new VoidMethodSignature(SIMPLE_POLL, "close");

	private static final NonVoidMethodSignature IS_POLL_OVER = new NonVoidMethodSignature(SIMPLE_POLL, "isOver", StorageTypes.BOOLEAN);
	private static final NonVoidMethodSignature IS__RUN_PERFORMED = new NonVoidMethodSignature(ACTION, "isRunPerformed", StorageTypes.BOOLEAN);

	private StorageReference stakeholder0;
	private StorageReference stakeholder1;
	private StorageReference stakeholder2;
	private StorageReference stakeholder3;
	private StorageReference external;

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("polls.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000, _1_000_000_000, _1_000_000_000, _1_000_000_000, _1_000_000_000);
		
		stakeholder0 = account(0);
		stakeholder1 = account(1);
		stakeholder2 = account(2);
		stakeholder3 = account(3);
		external = account(4);
	}

	StorageReference addSimpleSharedEntity(BigInteger share0, BigInteger share1, BigInteger share2, BigInteger share3) 
			throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return addConstructorCallTransaction(privateKey(0), stakeholder0, _10_000_000, ONE, jar(),
				SIMPLE_SHARED_ENTITY_CONSTRUCTOR, stakeholder0, stakeholder1, stakeholder2, stakeholder3,
						new BigIntegerValue(share0), new BigIntegerValue(share1), new BigIntegerValue(share2), new BigIntegerValue(share3));
	}

	StorageReference addSimplePoll(StorageReference sharedEntity, StorageReference action) throws InvalidKeyException, SignatureException, TransactionException, CodeExecutionException, TransactionRejectedException {
		return addConstructorCallTransaction(privateKey(0), stakeholder0, _10_000_000, ONE, jar(), SIMPLE_POLL_ENTITY_CONSTRUCTOR, sharedEntity, action);
	}

	StorageReference addAction() throws InvalidKeyException, SignatureException, TransactionException, CodeExecutionException, TransactionRejectedException {
		return addConstructorCallTransaction(privateKey(0), stakeholder0, _10_000_000, ONE, jar(), ACTION_CONSTRUCTOR);
	}

	@Test
	@DisplayName("new SimplePoll where there are not enough votes able to close it")
	void NotOverSimplePollWithNotEnoughVotes() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), VOTE_POLL, simplePoll);
		
		BooleanValue isOver = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS_POLL_OVER, simplePoll);
		Assertions.assertFalse(isOver.value);
	}
	
	@Test
	@DisplayName("new SimplePoll where no one has voted yet")
	void NotOverSimplePoll() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		
		StorageReference simpleSharedEntity = addSimpleSharedEntity( BigInteger.valueOf(10), ONE, ONE, ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		BooleanValue isOver = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS_POLL_OVER, simplePoll);	
		Assertions.assertFalse(isOver.value);

		BooleanValue isActionPerformed = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS__RUN_PERFORMED, action);
		Assertions.assertFalse(isActionPerformed.value);
	}
	
	@Test
	@DisplayName("new SimplePoll where a contract external to the shared entity tries to vote")
	void SimplePollVoteFailureByExternalContract() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		BooleanValue isOver = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS_POLL_OVER, simplePoll);	
		Assertions.assertFalse(isOver.value);

		assertThrows(TransactionException.class, () -> addInstanceMethodCallTransaction(privateKey(4), external, _10_000_000, ZERO, jar(), VOTE_POLL, simplePoll));
		
		BooleanValue isActionPerformed = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS__RUN_PERFORMED, action);
		Assertions.assertFalse(isActionPerformed.value);
	}
	
	@Test
	@DisplayName("new SimplePoll where someone attempts to close it before it's over")
	void NotOverSimplePollWithCloseAttempt() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), VOTE_POLL, simplePoll);
		
		BooleanValue isOver = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS_POLL_OVER, simplePoll);
		Assertions.assertFalse(isOver.value);
		
		assertThrows(TransactionException.class, () -> addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), CLOSE_POLL, simplePoll));
	}
	
	@Test
	@DisplayName("new SimplePoll where someone attempts to vote twice")
	void SimplePollWithDoubleVoteAttempt() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {

		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), VOTE_POLL, simplePoll);
				
		assertThrows(TransactionException.class, () -> addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), VOTE_POLL, simplePoll));
	}

	@Test
	@DisplayName("new SimplePoll where someone attempts to vote with more voting power than the maximum")
	void SimplePollWithVoteAttemptWithMorePowerThanAllowed() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		assertThrows(TransactionException.class, () -> addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), VOTE_POLL_WITH_PARAM, simplePoll, new BigIntegerValue(BigInteger.TWO)));
	}

	@Test
	@DisplayName("new SimplePoll() where all the 4 participants (having the same voting power) vote with their maximum voting power")
	void SuccessfulSimplePollWhereAllStakeHoldersVote() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), VOTE_POLL, simplePoll);
		addInstanceMethodCallTransaction(privateKey(1), stakeholder1, _10_000_000, ZERO, jar(), VOTE_POLL, simplePoll);
		addInstanceMethodCallTransaction(privateKey(2), stakeholder2, _10_000_000, ZERO, jar(), VOTE_POLL, simplePoll);
		addInstanceMethodCallTransaction(privateKey(3), stakeholder3, _10_000_000, ZERO, jar(), VOTE_POLL, simplePoll);
		
		BooleanValue isOver = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS_POLL_OVER, simplePoll);
		Assertions.assertTrue(isOver.value);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), CLOSE_POLL, simplePoll);
		
		BooleanValue isActionPerformed = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS__RUN_PERFORMED, action);
		Assertions.assertTrue(isActionPerformed.value);
	}
	
	@Test
	@DisplayName("new SimplePoll() where more than half of the participants (having the same voting power) vote with their maximum voting power")
	void SuccessfulSimplePollWhereMoreThan50PercentStakeholdersVote() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		
		StorageReference simpleSharedEntity = addSimpleSharedEntity(ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), VOTE_POLL, simplePoll);
		addInstanceMethodCallTransaction(privateKey(1), stakeholder1, _10_000_000, ZERO, jar(), VOTE_POLL, simplePoll);
		addInstanceMethodCallTransaction(privateKey(2), stakeholder2, _10_000_000, ZERO, jar(), VOTE_POLL, simplePoll);
		
		BooleanValue isOver = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS_POLL_OVER, simplePoll);	
		Assertions.assertTrue(isOver.value);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), CLOSE_POLL, simplePoll);
		
		BooleanValue isActionPerformed = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS__RUN_PERFORMED, action);
		Assertions.assertTrue(isActionPerformed.value);
	}
	
	@Test
	@DisplayName("new SimplePoll where one of the participants, holding a huge amount of voting power (more than 50% of the total) votes with maximum power")
	void SuccessfulSimplePollWhereOneOfTheStakeHoldersHasWayMorePower() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		
		StorageReference simpleSharedEntity = addSimpleSharedEntity( BigInteger.valueOf(10), ONE, ONE, ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), VOTE_POLL, simplePoll);
		
		BooleanValue isOver = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS_POLL_OVER, simplePoll);	
		Assertions.assertTrue(isOver.value);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), CLOSE_POLL, simplePoll);
		
		BooleanValue isActionPerformed = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS__RUN_PERFORMED, action);
		Assertions.assertTrue(isActionPerformed.value);
	}
	
	@Test
	@DisplayName("new SimplePoll where one of the participants, holding a huge amount of voting power (more than 50% of the total), votes with less than the maximum power")
	void SuccessfulSimplePollWhereOneOfTheStakeHoldersHasWayMorePowerButVotesWithLessThanMaximum() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		
		StorageReference simpleSharedEntity = addSimpleSharedEntity( BigInteger.valueOf(10), ONE, ONE, ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), VOTE_POLL_WITH_PARAM, simplePoll, new BigIntegerValue(BigInteger.valueOf(8)));
		
		BooleanValue isOver = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS_POLL_OVER, simplePoll);	
		Assertions.assertTrue(isOver.value);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), CLOSE_POLL, simplePoll);
		
		BooleanValue isActionPerformed = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS__RUN_PERFORMED, action);
		Assertions.assertTrue(isActionPerformed.value);
	}
	
	@Test
	@DisplayName("new SimplePoll where everyone votes but one of the participants, holding a huge amount of voting power (more than 50% of the total), votes zero")
	void FailingSimplePollWhereOneOfTheStakeHoldersHasWayMorePowerButVotesWithZero() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		
		StorageReference simpleSharedEntity = addSimpleSharedEntity( BigInteger.valueOf(10), ONE, ONE, ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), VOTE_POLL_WITH_PARAM, simplePoll, new BigIntegerValue(ZERO));
		
		addInstanceMethodCallTransaction(privateKey(1), stakeholder1, _10_000_000, ZERO, jar(), VOTE_POLL, simplePoll);
		addInstanceMethodCallTransaction(privateKey(2), stakeholder2, _10_000_000, ZERO, jar(), VOTE_POLL, simplePoll);
		addInstanceMethodCallTransaction(privateKey(3), stakeholder3, _10_000_000, ZERO, jar(), VOTE_POLL, simplePoll);
		
		BooleanValue isOver = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS_POLL_OVER, simplePoll);	
		Assertions.assertTrue(isOver.value);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), CLOSE_POLL, simplePoll);
		
		BooleanValue isActionPerformed = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS__RUN_PERFORMED, action);
		Assertions.assertFalse(isActionPerformed.value);
	}
	
	@Test
	@DisplayName("new SimplePoll where everyone has the same voting power but votes zero")
	void FailingSimplePollWhereEveryoneVotesWithZero() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		
		StorageReference simpleSharedEntity = addSimpleSharedEntity( ONE, ONE, ONE, ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), VOTE_POLL_WITH_PARAM, simplePoll, new BigIntegerValue(BigInteger.valueOf(0)));
		addInstanceMethodCallTransaction(privateKey(1), stakeholder1, _10_000_000, ZERO, jar(), VOTE_POLL_WITH_PARAM, simplePoll, new BigIntegerValue(BigInteger.valueOf(0)));
		addInstanceMethodCallTransaction(privateKey(2), stakeholder2, _10_000_000, ZERO, jar(), VOTE_POLL_WITH_PARAM, simplePoll, new BigIntegerValue(BigInteger.valueOf(0)));
		addInstanceMethodCallTransaction(privateKey(3), stakeholder3, _10_000_000, ZERO, jar(), VOTE_POLL_WITH_PARAM, simplePoll, new BigIntegerValue(BigInteger.valueOf(0)));
		
		BooleanValue isOver = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS_POLL_OVER, simplePoll);	
		Assertions.assertTrue(isOver.value);
		
		addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), CLOSE_POLL, simplePoll);
		
		BooleanValue isActionPerformed = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS__RUN_PERFORMED, action);
		Assertions.assertFalse(isActionPerformed.value);
	}
	
	@Test
	@DisplayName("new SimplePoll where everyone has voted except for one of the participants, holding a huge amount of voting power (more than 50% of the total)")
	void NotOverSimplePollBeacuseTheStakeholderWithWayMoreVotingPowerHasNotVotedYet() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		
		StorageReference simpleSharedEntity = addSimpleSharedEntity( BigInteger.valueOf(10), ONE, ONE, ONE);
		StorageReference action = addAction();
		StorageReference simplePoll = addSimplePoll(simpleSharedEntity, action);
		
		addInstanceMethodCallTransaction(privateKey(1), stakeholder1, _10_000_000, ZERO, jar(), VOTE_POLL, simplePoll);
		addInstanceMethodCallTransaction(privateKey(2), stakeholder2, _10_000_000, ZERO, jar(), VOTE_POLL, simplePoll);
		addInstanceMethodCallTransaction(privateKey(3), stakeholder3, _10_000_000, ZERO, jar(), VOTE_POLL, simplePoll);
		
		BooleanValue isOver = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS_POLL_OVER, simplePoll);	
		Assertions.assertFalse(isOver.value);

		BooleanValue isActionPerformed = (BooleanValue) addInstanceMethodCallTransaction(privateKey(0), stakeholder0, _10_000_000, ZERO, jar(), IS__RUN_PERFORMED, action);
		Assertions.assertFalse(isActionPerformed.value);
	}
}