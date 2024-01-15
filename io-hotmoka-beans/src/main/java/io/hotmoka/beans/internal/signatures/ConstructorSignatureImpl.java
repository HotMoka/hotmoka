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

package io.hotmoka.beans.internal.signatures;

import java.io.IOException;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.api.signatures.ConstructorSignature;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.types.StorageType;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * The signature of a constructor of a class.
 */
@Immutable
public final class ConstructorSignatureImpl extends AbstractCodeSignature implements ConstructorSignature {
	final static byte SELECTOR = 0;
	final static byte SELECTOR_EOA = 3;

	/**
	 * Builds the signature of a constructor.
	 * 
	 * @param definingClass the class of the constructor
	 * @param formals the formal arguments of the constructor
	 */
	public ConstructorSignatureImpl(ClassType definingClass, StorageType... formals) {
		super(definingClass, formals);
	}

	/**
	 * Builds the signature of a constructor.
	 * 
	 * @param definingClass the name of the class of the constructor
	 * @param formals the formal arguments of the constructor
	 */
	public ConstructorSignatureImpl(String definingClass, StorageType... formals) {
		super(definingClass, formals);
	}

	@Override
	public String toString() {
		return getDefiningClass() + commaSeparatedFormals();
	}

    @Override
	public boolean equals(Object other) {
		return other instanceof ConstructorSignatureImpl && super.equals(other);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		if (equals(EOA_CONSTRUCTOR))
			context.writeByte(SELECTOR_EOA);
		else {
			context.writeByte(SELECTOR);
			super.into(context);
		}
	}

	/**
	 * The constructor of an externally owned account.
	 */
	public final static ConstructorSignatureImpl EOA_CONSTRUCTOR = new ConstructorSignatureImpl(StorageTypes.EOA, StorageTypes.BIG_INTEGER, StorageTypes.STRING);
}