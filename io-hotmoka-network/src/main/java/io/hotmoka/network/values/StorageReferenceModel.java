/*
Copyright 2021 Dinu Berinde and Fausto Spoto

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

package io.hotmoka.network.values;

import java.math.BigInteger;

import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.api.values.StorageReference;

public class StorageReferenceModel {
	public TransactionReferenceModel transaction;
    public String progressive;

    public StorageReferenceModel(StorageReference input) {
    	transaction = new TransactionReferenceModel(input.getTransaction());
    	progressive = input.getProgressive().toString();
    }

    public StorageReferenceModel() {}

    public StorageReference toBean() {
    	return StorageValues.reference(transaction.toBean(), new BigInteger(progressive));
    }
}