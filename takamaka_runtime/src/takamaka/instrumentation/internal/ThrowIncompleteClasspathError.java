package takamaka.instrumentation.internal;

import takamaka.instrumentation.IncompleteClasspathError;

/**
 * Utilities for throwing an {@link takamaka.instrumentation.IncompleteClasspathError}
 * instead of a {@link java.lang.ClassNotFoundException}.
 */
public class ThrowIncompleteClasspathError {

	public interface Task {
		public void run() throws ClassNotFoundException;
	}

	public interface Computation<T> {
		public T run() throws ClassNotFoundException;
	}

	public static void insteadOfClassNotFoundException(Task task) {
		try {
			task.run();
		}
		catch (ClassNotFoundException e) {
			throw new IncompleteClasspathError(e);
		}
	}

	public static <T> T insteadOfClassNotFoundException(Computation<T> computation) {
		try {
			return computation.run();
		}
		catch (ClassNotFoundException e) {
			throw new IncompleteClasspathError(e);
		}
	}
}