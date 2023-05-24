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

package io.hotmoka.verification.errors;

import io.hotmoka.verification.internal.AbstractErrorImpl;

/**
 * An error issued when a redefining method uses {@code @@Payable}}
 * inconsistently with the redefined method.
 */
public class InconsistentPayableError extends AbstractErrorImpl {

	/**
	 * Builds the error.
	 * 
	 * @param where the description of the program point were the error occurs.
	 * @param methodName the name of the method where the error occurs
	 * @param classWhereItWasDefined the name of the class where the same method was defined
	 */
	public InconsistentPayableError(String where, String methodName, String classWhereItWasDefined) {
		super(where, methodName, -1, "@Payable is inconsistent with definition of the same method in class " + classWhereItWasDefined);
	}
}