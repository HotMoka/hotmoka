#!/bin/bash

# An example of a script that allows another node to become
# an active partner of a blockchain: it charges an account for the node
# and makes the node become a validator of the blockchain.
# This assumes that the script is run in a sibling directory of
# a hotmoka_node_info directory containing them pem file
# of an account with enough money to create the account, fund it and make
# it into a validator. Moreover, that latter directory must contain
# the pem file of a validator account with enough validator power to sell.

# Source it as follows (if you want to interact with panarea.hotmoka.io,
# but any node of the same blockchain will do):
# bash <(curl -H 'Cache-Control: no-cache, no-store' -s https://raw.githubusercontent.com/Hotmoka/hotmoka/master/scripts/sell.sh) seller validator hotmoka ws://panarea.hotmoka.io
# where seller is the address of the account that sells the money (for instance, the gamete)
# and validator is the address of the validator that sends part of its power.

TYPE=${3:-hotmoka}
SELLER_ADDRESS=$1
VALIDATOR_ADDRESS=$2
TYPE_CAPITALIZED=${TYPE^}
DIR=${TYPE}_node_info
if [ $TYPE = hotmoka ];
then
    NETWORK_URI=${4:-ws://panarea.hotmoka.io}
    GITHUB_ID=Hotmoka
    CLI=moka
else
    NETWORK_URI=${4:-ws://blueknot.vero4chain.it}
    GITHUB_ID=Vero4Chain
    CLI=blue
fi;

VERSION=$(moka node info --json --uri $NETWORK_URI | python3 -c "import sys, json; print(json.load(sys.stdin)['version'])")

echo "Selling some crypto and making an account into a validator of the $TYPE_CAPITALIZED blockchain at $NETWORK_URI, version $VERSION."
echo "The seller of the crypto is $SELLER_ADDRESS."
echo "The seller of the validation power is $VALIDATOR_ADDRESS."
echo "Assuming the pem's of both accounts to be in the $DIR directory."

echo " * downloading the blockchain CLI"
rm -r $DIR/${CLI} 2>/dev/null
mkdir $DIR/${CLI}
cd $DIR/${CLI}
wget --quiet https://github.com/${GITHUB_ID}/${TYPE}/releases/download/v${VERSION}/${CLI}_${VERSION}.tar.gz
tar zxf ${CLI}_${VERSION}.tar.gz
cd ../..

echo " * determining the amount of crypto to sell"
cd $DIR
MANIFEST=$(./${CLI}/${CLI} info --uri ${NETWORK_URI})
LINE=$(echo "$MANIFEST" | grep "validators" | sed '1!d')
VALIDATORS_ADDRESS=${LINE: -66}
RUN=$(./${CLI}/${CLI} call ${VALIDATORS_ADDRESS} getInitialSupply --class-of-receiver=io.takamaka.code.governance.Validators --use-colors=false --uri ${NETWORK_URI})
INITIAL_SUPPLY=$(echo "$RUN" | sed '1!d')
FUND_AMOUNT=$(echo "$INITIAL_SUPPLY/100" | bc)
echo "   -> $FUND_AMOUNT"
cd ..

echo " * determining the amount of validation power to sell"
cd $DIR
LINE=$(echo "$MANIFEST" | grep "initial validators" | sed '1!d')
INITIAL_VALIDATORS_ADDRESS=${LINE:26}
RUN=$(./${CLI}/${CLI} call ${INITIAL_VALIDATORS_ADDRESS} getTotalShares --class-of-receiver=io.takamaka.code.dao.SharedEntityView --use-colors=false --uri ${NETWORK_URI})
TOTAL_SHARES=$(echo "$RUN" | sed '1!d')
VALIDATION_AMOUNT=$(echo "$TOTAL_SHARES/100" | bc)
echo "   -> $VALIDATION_AMOUNT"
cd ..

echo " * funding the money account"
read -p "     key to pay into: " MONEY_ACCOUNT_PUBLIC_KEY_BASE58
read -s -p "     password of the seller account: " PASSWORD_OF_SELLER
echo
cd $DIR
./${CLI}/${CLI} send ${FUND_AMOUNT} ${MONEY_ACCOUNT_PUBLIC_KEY_BASE58} --anonymous --payer ${SELLER_ADDRESS} --uri ${NETWORK_URI} --interactive=false --password-of-payer=${PASSWORD_OF_SELLER} >/dev/null
cd ..

echo " * creating a sale offer of validation power"
read -p "     address of the validator account: " VALIDATOR_BUYER_ACCOUNT
cd $DIR
./${CLI}/${CLI} sell-validation ${VALIDATOR_ADDRESS} ${VALIDATION_AMOUNT} 0 100000 --buyer ${VALIDATOR_BUYER_ACCOUNT} --interactive=false --password-of-seller= --print-costs=false --uri ${NETWORK_URI} >/dev/null
cd ..

echo " * cleaning up"
rm -r ${DIR}/${CLI}
