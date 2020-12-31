package io.takamaka.code.system.tendermint;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.system.Validators;

/**
 * The validators of a Tendermint blockchain. They have an ED25519 public key
 * and an id derived from the public key, according to the algorithm used by Tendermint.
 */
public class TendermintValidators extends Validators {

	/**
	 * Creates a set of validators of aTendermint blockchain, from their public keys and powers.
	 * 
	 * @param publicKeys the public keys of the initial validators, as a space-separated
	 *                   sequence of Base64-encoded ED25519 publicKeys
	 * @param powers the initial powers of the initial validators, as a space-separated sequence of integers;
	 *               they must be as many as there are public keys in {@code publicKeys}
	 */
	public TendermintValidators(String publicKeys, String powers) {
		super(buildValidators(publicKeys), buildPowers(powers));
	}

	protected static TendermintED25519Validator[] buildValidators(String publicKeysAsStringSequence) {
		return splitAtSpaces(publicKeysAsStringSequence).stream()
			.map(TendermintED25519Validator::new)
			.toArray(TendermintED25519Validator[]::new);
	}

	@Override
	public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount, Offer offer) {
		// we ensure that the only shareholders are Validator's
		require(caller() instanceof TendermintED25519Validator, () -> "only a " + TendermintED25519Validator.class.getSimpleName() + " can accept an offer");
		super.accept(amount, offer);
	}
}