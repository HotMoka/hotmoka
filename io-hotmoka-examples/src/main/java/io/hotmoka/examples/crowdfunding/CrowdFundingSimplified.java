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

package io.hotmoka.examples.crowdfunding;
import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.math.BigIntegerSupport;
import io.takamaka.code.util.StorageList;
import io.takamaka.code.util.StorageLinkedList;

public class CrowdFundingSimplified extends Contract {
	public Campaign newCampaign(PayableContract beneficiary, BigInteger goal) {
		return new Campaign(beneficiary, goal);
	}

	public @Payable @FromContract void contribute(BigInteger amount, Campaign campaign) {
		campaign.funders.add(new Funder(caller(), amount));
		campaign.amount = campaign.amount.add(amount);
	}

	public boolean checkGoalReached(Campaign campaign) {
		if (BigIntegerSupport.compareTo(campaign.amount, campaign.fundingGoal) < 0)
			return false;
		else {
			BigInteger amount = campaign.amount;
			campaign.amount = BigInteger.ZERO;
			campaign.beneficiary.receive(amount);
			return true;
		}
	}

	@Exported
	public static class Campaign extends Storage {
		private final PayableContract beneficiary;
		private final BigInteger fundingGoal;
		private final StorageList<Funder> funders = new StorageLinkedList<>();
		private BigInteger amount;

		private Campaign(PayableContract beneficiary, BigInteger fundingGoal) {
			this.beneficiary = beneficiary;
			this.fundingGoal = fundingGoal;
			this.amount = BigInteger.ZERO;
		}
	}

	private static class Funder extends Storage {
		@SuppressWarnings("unused")
		private final Contract who;
		@SuppressWarnings("unused")
		private final BigInteger amount;

		public Funder(Contract who, BigInteger amount) {
			this.who = who;
			this.amount = amount;
		}
	}
}