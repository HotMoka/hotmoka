package io.hotmoka.network.internal.models.transactions;

import java.util.List;

import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.internal.models.storage.ValueModel;
import io.hotmoka.network.internal.util.StorageResolver;
import io.hotmoka.network.json.JSONTransactionReference;

public class ConstructorCallTransactionRequestModel extends NonInitialTransactionRequestModel {
    private String constructorType;
    private List<ValueModel> values;

    public String getConstructorType() {
        return constructorType;
    }

    public void setConstructorType(String constructorType) {
        this.constructorType = constructorType;
    }

    public List<ValueModel> getValues() {
        return values;
    }

    public void setValues(List<ValueModel> values) {
        this.values = values;
    }

    public ConstructorCallTransactionRequest toBean() {
    	ConstructorSignature constructor = new ConstructorSignature(getConstructorType(), StorageResolver.resolveStorageTypes(getValues()));
        StorageValue[] actuals = StorageResolver.resolveStorageValues(getValues());

        return new ConstructorCallTransactionRequest(
        	decodeBase64(getSignature()),
            getCaller().toBean(),
            getNonce(),
            getChainId(),
            getGasLimit(),
            getGasPrice(),
            JSONTransactionReference.fromJSON(getClasspath()),
            constructor,
            actuals);
    }
}