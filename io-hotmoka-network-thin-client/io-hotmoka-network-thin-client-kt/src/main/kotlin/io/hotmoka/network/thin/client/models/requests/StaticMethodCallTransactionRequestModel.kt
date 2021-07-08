package io.hotmoka.network.thin.client.models.requests

import io.hotmoka.network.thin.client.models.signatures.MethodSignatureModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.StorageValueModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel

class StaticMethodCallTransactionRequestModel(
        signature: String,
        caller: StorageReferenceModel,
        nonce: String,
        classpath: TransactionReferenceModel,
        chainId: String,
        gasLimit: String,
        gasPrice: String,
        method: MethodSignatureModel,
        actuals: List<StorageValueModel>
) : MethodCallTransactionRequestModel(
        signature,
        caller,
        nonce,
        classpath,
        chainId,
        gasLimit,
        gasPrice,
        method,
        actuals
)