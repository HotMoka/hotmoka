/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.exceptions;

import java.util.function.Supplier;

/**
 * Methods that check an unchecked exception thrown by a supplier.
 */
public abstract class CheckSupplier {

	private CheckSupplier() {}

	/**
	 * Runs a supplier and makes an unchecked exception into checked.
	 * 
	 * @param <R> the type of the return type of the supplied value
	 * @param <T> the type of the exception
	 * @param exception the class of the exception
	 * @param supplier the supplier
	 * @return the supplied value
	 * @throws T if the supplier throws an unchecked exception with this cause
	 */
	@SuppressWarnings("unchecked")
	public static <R, T extends Throwable> R check(Class<T> exception, Supplier<R> supplier) throws T {
		try {
			return supplier.get();
		}
		catch (UncheckedException e) {
			var cause = e.getCause();
			if (exception.isInstance(cause))
				throw (T) cause;
			else
				throw e;
		}
	}

	/**
	 * Runs a supplier and makes two unchecked exceptions into checked.
	 * 
	 * @param <R> the type of the return type of the supplied value
	 * @param <T1> the type of the first exception
	 * @param <T2> the type of the second exception
	 * @param exception1 the class of the first exception
	 * @param exception2 the class of the second exception
	 * @param supplier the supplier
	 * @return the supplied value
	 * @throws T1 if the supplier throws an unchecked exception with this cause
	 * @throws T2 if the supplier throws an unchecked exception with this cause
	 */
	@SuppressWarnings("unchecked")
	public static <R, T1 extends Throwable, T2 extends Throwable> R check(Class<T1> exception1, Class<T2> exception2, Supplier<R> supplier) throws T1, T2 {
		try {
			return supplier.get();
		}
		catch (UncheckedException e) {
			var cause = e.getCause();
			if (exception1.isInstance(cause))
				throw (T1) cause;
			else if (exception2.isInstance(cause))
				throw (T2) cause;
			else
				throw e;
		}
	}

	/**
	 * Runs a supplier and makes three unchecked exceptions into checked.
	 * 
	 * @param <R> the type of the return type of the supplied value
	 * @param <T1> the type of the first exception
	 * @param <T2> the type of the second exception
	 * @param <T3> the type of the third exception
	 * @param exception1 the class of the first exception
	 * @param exception2 the class of the second exception
	 * @param exception3 the class of the third exception
	 * @param supplier the supplier
	 * @return the supplied value
	 * @throws T1 if the supplier throws an unchecked exception with this cause
	 * @throws T2 if the supplier throws an unchecked exception with this cause
	 * @throws T3 if the supplier throws an unchecked exception with this cause
	 */
	@SuppressWarnings("unchecked")
	public static <R, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> R check(Class<T1> exception1, Class<T2> exception2, Class<T3> exception3, Supplier<R> supplier) throws T1, T2, T3 {
		try {
			return supplier.get();
		}
		catch (UncheckedException e) {
			var cause = e.getCause();
			if (exception1.isInstance(cause))
				throw (T1) cause;
			else if (exception2.isInstance(cause))
				throw (T2) cause;
			else if (exception3.isInstance(cause))
				throw (T3) cause;
			else
				throw e;
		}
	}
}