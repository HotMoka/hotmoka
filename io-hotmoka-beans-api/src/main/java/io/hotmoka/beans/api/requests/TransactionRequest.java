/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.beans.api.requests;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.marshalling.api.Marshallable;

/**
 * A request of a transaction.
 * 
 * @param <R> the type of the response expected for this request
 */
@Immutable
public interface TransactionRequest<R extends TransactionResponse> extends Marshallable {

	@Override
	boolean equals(Object other);

	@Override
	int hashCode();

	@Override
	String toString();
}