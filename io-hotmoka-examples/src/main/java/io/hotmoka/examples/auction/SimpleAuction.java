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

package io.hotmoka.examples.auction;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.now;
import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.math.BigIntegerSupport;

/**
 * A contract for a simple auction. This class is derived from the Solidity code shown at
 * https://solidity.readthedocs.io/en/v0.4.25/solidity-by-example.html#simple-open-auction
 */
public class SimpleAuction extends Auction {

	/**
	 * The contract that will receive the bid at the end.
	 */
	private final PayableContract beneficiary;

	/**
	 * The deadline of the auction.
	 */
	private final long auctionEnd;

	/**
	 * The highest big.
	 */
	private BigInteger highestBid = BigInteger.ZERO;

    /**
	 * The bidder of the highest bid. This is {@code null} if no bid
	 * has been placed yet.
	 */
	private PayableContract highestBidder;

	/**
	 * Create, on behalf of the beneficiary, a simple auction.
	 * 
	 * @param biddingTime the length of the bidding period
	 * @param beneficiary the beneficiary
	 */
	public SimpleAuction(int biddingTime, PayableContract beneficiary) {
        this.beneficiary = beneficiary;
        this.auctionEnd = now() + biddingTime;
    }

	/**
	 * Places a bid. The value will only be refunded if the auction is not won.
	 * 
	 * @param amount the bid amount
	 */
	public @Payable @FromContract(PayableContract.class) void bid(BigInteger amount) {
        // reject the call if the bidding period is over or if the bid is not higher than previous
        require(now() < auctionEnd, "Auction already ended");
        require(BigIntegerSupport.compareTo(amount, highestBid) > 0, "There already is a higher bid");

        // refund the previous highest bidder
        if (highestBidder != null)
        	highestBidder.receive(highestBid);

        // take note of the new highest bid
        highestBidder = (PayableContract) caller();
        highestBid = amount;
        event(new BidIncrease(highestBidder, amount));
    }

	@Override
	public PayableContract auctionEnd() {
        require(now() >= auctionEnd, "Auction not yet ended");

        PayableContract winner = highestBidder;
    	
	    if (winner != null) {
	    	beneficiary.receive(highestBid);
	    	event(new AuctionEnd(winner, highestBid));
	    	highestBidder = null;
	    }

	    return winner;
    }
}