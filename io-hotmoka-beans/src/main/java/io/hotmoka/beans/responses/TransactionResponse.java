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

package io.hotmoka.beans.responses;

import java.io.IOException;
import java.io.OutputStream;

import io.hotmoka.beans.marshalling.BeanMarshallingContext;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * The response of a transaction.
 */
public abstract class TransactionResponse extends AbstractMarshallable {

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the response
	 * @throws IOException if the response cannot be unmarshalled
	 */
	public static TransactionResponse from(UnmarshallingContext context) throws IOException {
		byte selector = context.readByte();

		switch (selector) {
		case GameteCreationTransactionResponse.SELECTOR: return GameteCreationTransactionResponse.from(context);
		case JarStoreInitialTransactionResponse.SELECTOR: return JarStoreInitialTransactionResponse.from(context);
		case InitializationTransactionResponse.SELECTOR: return InitializationTransactionResponse.from(context);
		case JarStoreTransactionFailedResponse.SELECTOR: return JarStoreTransactionFailedResponse.from(context);
		case JarStoreTransactionSuccessfulResponse.SELECTOR: return JarStoreTransactionSuccessfulResponse.from(context);
		case ConstructorCallTransactionExceptionResponse.SELECTOR: return ConstructorCallTransactionExceptionResponse.from(context);
		case ConstructorCallTransactionFailedResponse.SELECTOR: return ConstructorCallTransactionFailedResponse.from(context);
		case ConstructorCallTransactionSuccessfulResponse.SELECTOR:
		case ConstructorCallTransactionSuccessfulResponse.SELECTOR_NO_EVENTS: return ConstructorCallTransactionSuccessfulResponse.from(context, selector);
		case MethodCallTransactionExceptionResponse.SELECTOR: return MethodCallTransactionExceptionResponse.from(context);
		case MethodCallTransactionFailedResponse.SELECTOR: return MethodCallTransactionFailedResponse.from(context);
		case MethodCallTransactionSuccessfulResponse.SELECTOR:
		case MethodCallTransactionSuccessfulResponse.SELECTOR_NO_EVENTS_NO_SELF_CHARGED:
		case MethodCallTransactionSuccessfulResponse.SELECTOR_ONE_EVENT_NO_SELF_CHARGED: return MethodCallTransactionSuccessfulResponse.from(context, selector);
		case VoidMethodCallTransactionSuccessfulResponse.SELECTOR:
		case VoidMethodCallTransactionSuccessfulResponse.SELECTOR_NO_EVENTS_NO_SELF_CHARGED: return VoidMethodCallTransactionSuccessfulResponse.from(context, selector);
		default: throw new IOException("Unexpected response selector: " + selector);
		}
	}

	@Override
	protected final MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return new BeanMarshallingContext(os);
	}
}