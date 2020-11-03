package io.takamaka.tests.coin;

import static java.math.BigInteger.ZERO;
import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.Event;
import io.takamaka.code.util.StorageMap;

public class Coin extends Contract {
	private final Contract minter;
	private final StorageMap<Contract, BigInteger> balances = new StorageMap<>();

	public static class Send extends Event {
		public final Contract from;
		public final Contract to;
		public final BigInteger amount;

		private @Entry Send(Contract from, Contract to, BigInteger amount) {
			this.from = from;
			this.to = to;
			this.amount = amount;
		}
	}

	public @Entry Coin() {
		minter = caller();
    }

	public @Entry void mint(Contract receiver, int amount) {
        require(caller() == minter, "Only the minter can mint new coins");
        require(amount > 0, "You can only mint a positive amount of coins");
        require(amount < 1000, "You can mint up to 1000 coins at a time");
        balances.update(receiver, ZERO, BigInteger.valueOf(amount)::add);
    }

	public @Entry void send(Contract receiver, BigInteger amount) {
		BigInteger myAmount = balances.get(caller());
		require(myAmount != null && myAmount.compareTo(amount) >= 0, "Insufficient balance");
		balances.put(caller(), myAmount.subtract(amount));
        balances.update(receiver, ZERO, amount::add);
        event(new Send(caller(), receiver, amount));
    }
}