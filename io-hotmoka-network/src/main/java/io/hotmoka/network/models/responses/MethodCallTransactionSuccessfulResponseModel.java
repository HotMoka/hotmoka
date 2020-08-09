package io.hotmoka.network.models.responses;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import io.hotmoka.beans.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.network.models.updates.UpdateModel;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.StorageValueModel;

public class MethodCallTransactionSuccessfulResponseModel extends MethodCallTransactionResponseModel {

    /**
     * The return value of the method.
     */
    public StorageValueModel result;

    /**
     * The events generated by this transaction.
     */
    private List<StorageReferenceModel> events;

    public MethodCallTransactionSuccessfulResponseModel(MethodCallTransactionSuccessfulResponse response) {
        super(response);

        this.result = new StorageValueModel(response.result);
        this.events = response.getEvents().map(StorageReferenceModel::new).collect(Collectors.toList());
    }

    public MethodCallTransactionSuccessfulResponseModel() {}

    public MethodCallTransactionSuccessfulResponse toBean() {
        return new MethodCallTransactionSuccessfulResponse(
        	result.toBean(),
        	updates.stream().map(UpdateModel::toBean),
        	events.stream().map(StorageReferenceModel::toBean),
        	new BigInteger(gasConsumedForCPU),
        	new BigInteger(gasConsumedForRAM),
        	new BigInteger(gasConsumedForStorage)
        );
    }
}