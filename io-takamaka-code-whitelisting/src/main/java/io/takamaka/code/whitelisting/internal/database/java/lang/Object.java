package io.takamaka.code.whitelisting.internal.database.java.lang;

import io.takamaka.code.whitelisting.HasDeterministicTerminatingToString;
import io.takamaka.code.whitelisting.MustRedefineHashCode;

public abstract class Object {
	public Object() {}
	public abstract java.lang.Object clone();
	//public abstract java.lang.Class<?> getClass(); // this needs a special treatment in the class verifier
	public abstract boolean equals(java.lang.Object other);
	public abstract @HasDeterministicTerminatingToString java.lang.String toString();
	public abstract @MustRedefineHashCode int hashCode();
}