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

package io.hotmoka.beans.values;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * A {@code double} value stored in blockchain.
 */
@Immutable
public final class DoubleValue extends StorageValue {
	static final byte SELECTOR = 4;

	/**
	 * The value.
	 */
	public final double value;

	/**
	 * Builds a {@code double} value.
	 * 
	 * @param value the value
	 */
	public DoubleValue(double value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return Double.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof DoubleValue && ((DoubleValue) other).value == value;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(value);
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;
		else
			return Double.compare(value, ((DoubleValue) other).value);
	}

	@Override
	public void into(MarshallingContext context) {
		context.writeByte(SELECTOR);
		context.writeDouble(value);
	}
}