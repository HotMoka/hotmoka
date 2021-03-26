package io.hotmoka.beans.values;

import java.io.IOException;

import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;

/**
 * A {@code boolean} value stored in blockchain.
 */
@Immutable
public final class BooleanValue extends StorageValue {
	static final byte SELECTOR_TRUE = 0;
	static final byte SELECTOR_FALSE = 1;

	/**
	 * The true Boolean value.
	 */
	public final static BooleanValue TRUE = new BooleanValue(true);

	/**
	 * The false Boolean value.
	 */
	public final static BooleanValue FALSE = new BooleanValue(false);

	/**
	 * The value.
	 */
	public final boolean value;

	/**
	 * Builds a {@code boolean} value.
	 * 
	 * @param value the value
	 */
	public BooleanValue(boolean value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return Boolean.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof BooleanValue && ((BooleanValue) other).value == value;
	}

	@Override
	public int hashCode() {
		return Boolean.hashCode(value);
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;
		else
			return Boolean.compare(value, ((BooleanValue) other).value);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		if (value)
			context.writeByte(SELECTOR_TRUE);
		else
			context.writeByte(SELECTOR_FALSE);
	}
}