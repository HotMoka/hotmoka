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

package io.hotmoka.network.requests;

import io.hotmoka.beans.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceSystemMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequestImpl;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;

/**
 * Class which wraps a type request model
 * 
 * @param <T> the type request model
 */
public class TransactionRestRequestModel<T> {
    /**
     * The request model which should be an instance of {@link TransactionRequestModel}.
     */
    public T transactionRequestModel;

    /**
     * The runtime type of the request model
     */
    public String type;

    public TransactionRestRequestModel(T transactionRequestModel) {
        this.transactionRequestModel = transactionRequestModel;
        this.type = transactionRequestModel != null ? transactionRequestModel.getClass().getName() : null;
    }

    public TransactionRestRequestModel() {}

    /**
     * Builds the model of the given request.
     *
     * @param request the request
     * @return the corresponding model
     */
    public static TransactionRestRequestModel<?> from(TransactionRequest<?> request) {
        if (request == null)
            throw new RuntimeException("unexpected null request");
        else if (request instanceof ConstructorCallTransactionRequest)
            return new TransactionRestRequestModel<>(new ConstructorCallTransactionRequestModel((ConstructorCallTransactionRequest) request));
        else if (request instanceof InitializationTransactionRequest)
            return new TransactionRestRequestModel<>(new InitializationTransactionRequestModel((InitializationTransactionRequest) request));
        else if (request instanceof InstanceMethodCallTransactionRequest)
            return new TransactionRestRequestModel<>(new InstanceMethodCallTransactionRequestModel((InstanceMethodCallTransactionRequest) request));
        else if (request instanceof InstanceSystemMethodCallTransactionRequest)
            return new TransactionRestRequestModel<>(new InstanceSystemMethodCallTransactionRequestModel((InstanceSystemMethodCallTransactionRequest) request));
        else if (request instanceof JarStoreInitialTransactionRequest)
            return new TransactionRestRequestModel<>(new JarStoreInitialTransactionRequestModel((JarStoreInitialTransactionRequest) request));
        else if (request instanceof JarStoreTransactionRequestImpl)
            return new TransactionRestRequestModel<>(new JarStoreTransactionRequestModel((JarStoreTransactionRequestImpl) request));
        else if (request instanceof GameteCreationTransactionRequest)
            return new TransactionRestRequestModel<>(new GameteCreationTransactionRequestModel((GameteCreationTransactionRequest) request));
        else if (request instanceof StaticMethodCallTransactionRequest)
            return new TransactionRestRequestModel<>(new StaticMethodCallTransactionRequestModel((StaticMethodCallTransactionRequest) request));
        else
            throw new RuntimeException("unexpected transaction request of class " + request.getClass().getName());
    }
}