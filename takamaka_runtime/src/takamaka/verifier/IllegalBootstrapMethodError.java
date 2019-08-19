package takamaka.verifier;

import org.apache.bcel.generic.ClassGen;

public class IllegalBootstrapMethodError extends Error {

	public IllegalBootstrapMethodError(ClassGen clazz) {
		super(clazz, "Illegal bootstrap method");
	}
}